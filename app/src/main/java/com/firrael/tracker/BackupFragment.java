package com.firrael.tracker;

import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.firrael.tracker.base.SimpleFragment;
import com.firrael.tracker.realm.RealmDB;
import com.firrael.tracker.realm.TaskModel;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityOptions;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Created by railag on 17.01.2018.
 */

public class BackupFragment extends SimpleFragment {
    private final static String TAG = BackupFragment.class.getSimpleName();

    private final static String BACKUP_ROOT_INDICATOR = " tasks";

    public final static int REQUEST_FIND_FOLDER = 3;

    private final static String KEY_DRIVE_ID = "response_drive_id";

    public static BackupFragment newInstance() {

        Bundle args = new Bundle();

        BackupFragment fragment = new BackupFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private ImageButton uploadButton;
    private ImageButton importButton;

    private DriveResourceClient mDriveResourceClient;
    private DriveClient mDriveClient;

    private int mNumberOfTasks;
    private int mFinishedTasks;
    private boolean loading;

    private Map<String, Bitmap> mBitmapsMap;

    interface SelectFolderListener {
        void selectedFolder(Bundle extras);
    }

    @Override
    protected String getTitle() {
        return getString(R.string.backup);
    }

    @Override
    protected int getViewId() {
        return R.layout.fragment_backup;
    }

    @Override
    protected void initView(View v) {
        uploadButton = v.findViewById(R.id.upload_button);
        importButton = v.findViewById(R.id.import_button);

        getMainActivity().hideFab();

        mDriveResourceClient = App.getDrive();
        mDriveClient = App.getDriveClient();

        uploadButton.setOnClickListener(view -> {
            upload();
        });

        importButton.setOnClickListener(view -> {
            importTasks();
        });
    }

    private void importTasks() { // look for Folders && Title.contains("tasks")
        if (mDriveClient == null) {
            return;
        }

        OpenFileActivityOptions options = new OpenFileActivityOptions.Builder()
                .setSelectionFilter(Filters.and(Filters.eq(SearchableField.MIME_TYPE, DriveFolder.MIME_TYPE),
                        Filters.contains(SearchableField.TITLE, BACKUP_ROOT_INDICATOR)))
                .build();

        Task<IntentSender> intentSenderTask = mDriveClient.newOpenFileActivityIntentSender(options);
        intentSenderTask.continueWith(task -> {
            loading = true;
            startLoading();
            IntentSender intentSender = task.getResult();

            try {
                SelectFolderListener selectFolderListener = extras -> {
                    if (extras == null || !extras.containsKey(KEY_DRIVE_ID)) {
                        return;
                    }

                    mBitmapsMap = new HashMap<>();

                    DriveId driveId = (DriveId) extras.get(KEY_DRIVE_ID);
                    DriveFolder backupFolder = driveId.asDriveFolder();
                    Task<MetadataBuffer> tasksMetadataTask = mDriveResourceClient.listChildren(backupFolder);
                    tasksMetadataTask.continueWith(task1 -> {
                        MetadataBuffer tasksMetadata = task1.getResult();

                        if (tasksMetadata == null || tasksMetadata.getCount() == 0) {
                            loading = false;
                            stopLoading();
                            Snackbar.make(getView(), R.string.no_tasks_found_import_error, Toast.LENGTH_SHORT).show();
                            return null;
                        }

                        List<DriveFolder> taskFolders = new ArrayList<>();
                        for (int i = 0; i < tasksMetadata.getCount(); i++) {
                            Metadata metadata = tasksMetadata.get(i);
                            DriveFolder folder = metadata.getDriveId().asDriveFolder();
                            taskFolders.add(folder);
                        }

                        for (DriveFolder folder : taskFolders) {
                            Task<MetadataBuffer> localTaskMetadataTask = mDriveResourceClient.listChildren(folder);
                            localTaskMetadataTask.continueWith(task2 -> {
                                MetadataBuffer localTaskMetadata = task2.getResult();


                                mNumberOfTasks += localTaskMetadata.getCount();
                                mFinishedTasks = 0;

                                for (int i = 0; i < localTaskMetadata.getCount(); i++) {
                                    Metadata metadata = localTaskMetadata.get(i);
                                    DriveFile file = metadata.getDriveId().asDriveFile();
                                    String mimeType = metadata.getMimeType();
                                    boolean isImage = mimeType.contains("image");

                                    Task<DriveContents> fileTask = mDriveResourceClient.openFile(file, DriveFile.MODE_READ_ONLY);

                                    fileTask.continueWith(task3 -> {
                                        DriveContents fileContents = task3.getResult();

                                        InputStream inputStream = fileContents.getInputStream();
                                        InputStreamReader reader = new InputStreamReader(inputStream);

                                        if (isImage) {
                                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                            mBitmapsMap.put(metadata.getTitle(), bitmap);
                                        } else {
                                            Gson gson = Utils.buildGson();
                                            TaskModel taskModel = gson.fromJson(reader, TaskModel.class);
                                            RealmDB.get().executeTransaction(realm -> realm.copyToRealmOrUpdate(taskModel));
                                        }

                                        tasksMetadata.release();
                                        localTaskMetadata.release();

                                        verifyProgress(false);

                                        return mDriveResourceClient.discardContents(fileContents);
                                    });
                                }

                                return null;
                            });
                        }


                        return null;
                    });

                };

                getMainActivity().setSelectFolderListener(selectFolderListener);
                getMainActivity().startIntentSenderForResult(
                        intentSender, REQUEST_FIND_FOLDER, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Unable to create file", e);
                Snackbar.make(getView(), "Unable to open Google Drive folders", Snackbar.LENGTH_SHORT).show();
            }

            return null;
        });
    }

    private void upload() {
        List<TaskModel> tasks = loadTasks();
        if (tasks == null || tasks.size() == 0) {
            Snackbar.make(getView(), R.string.no_tasks_found_backup_error, Snackbar.LENGTH_SHORT).show();
            return;
        }

        startLoading();
        loading = true;

        mNumberOfTasks = tasks.size();
        for (TaskModel taskModel : tasks) {
            if (taskModel.hasImage()) {
                mNumberOfTasks++;
            }
        }

        String timeStamp = Utils.getCurrentTimestamp();
        final String folderName = timeStamp + BACKUP_ROOT_INDICATOR;

        Task<DriveFolder> folderTask = DriveUtils.createFolder(folderName, mDriveResourceClient);
        Tasks.whenAll(folderTask).continueWith((Continuation<Void, Void>) task -> {
            DriveFolder parentFolder = folderTask.getResult();

            Task<MetadataBuffer> metadataTask = DriveUtils.getMetadataForFolder(folderName, mDriveResourceClient);
            Tasks.whenAll(metadataTask).continueWithTask(task2 -> {
                MetadataBuffer buffer = metadataTask.getResult();

                DriveFolder rootDriveFolder = DriveUtils.getDriveFolder(buffer, folderName);

                for (int i = 0; i < tasks.size(); i++) {
                    addTaskToGoogleDrive(tasks.get(i), rootDriveFolder);
                }

                buffer.release();

                return null;
            });

            return null;
        });
    }

    private void addTaskToGoogleDrive(TaskModel taskModel, DriveFolder folder) {
        final Task<DriveContents> createJsonTask = mDriveResourceClient.createContents();
        final Task<DriveContents> createImageTask = mDriveResourceClient.createContents();

        Task<DriveFolder> folderTask = DriveUtils.createSubFolder(taskModel.getTaskName(), folder, mDriveResourceClient);

        Tasks.whenAll(folderTask, createJsonTask, createImageTask).continueWith((Continuation<Void, Void>) task -> {

            Task<MetadataBuffer> metadataBufferTask = DriveUtils.getMetadataForFolder(taskModel.getTaskName(), mDriveResourceClient);

            metadataBufferTask.continueWith(task3 -> {
                MetadataBuffer metadata = metadataBufferTask.getResult();
                DriveFolder taskFolder = DriveUtils.getDriveFolder(metadata, taskModel.getTaskName());

                if (taskModel.hasImage()) {
                    DriveContents contents = createImageTask.getResult();
                    Glide.with(this).load(taskModel.getImageUrl()).into(new SimpleTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            if (resource instanceof BitmapDrawable) {
                                BitmapDrawable bitmapDrawable = (BitmapDrawable) resource;
                                Bitmap bitmap = bitmapDrawable.getBitmap();
                                String name = taskModel.getTaskName() + " image";

                                Task<DriveFile> imageTask = DriveUtils.createImage(contents, bitmap, name, taskFolder, mDriveResourceClient);
                                imageTask.addOnCompleteListener(task12 -> {
                                    verifyProgress(true);
                                });
                            }
                        }
                    });
                }

                DriveContents jsonFile = createJsonTask.getResult();
                OutputStream outputStream = jsonFile.getOutputStream();

                Gson gson = Utils.buildGson();

                String json = gson.toJson(RealmDB.get().copyFromRealm(taskModel));

                outputStream.write(json.getBytes());

                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(taskModel.getTaskName())
                        .setMimeType("text/json")
                        .build();

                metadata.release();

                Task<DriveFile> lastTask = mDriveResourceClient.createFile(taskFolder, changeSet, jsonFile);
                lastTask.addOnCompleteListener(task1 -> {
                    verifyProgress(true);
                });
                return null;
            });

            return null;
        });
    }

    private void verifyProgress(boolean uploading) {
        if (loading) {
            mFinishedTasks++;
            if (mFinishedTasks >= mNumberOfTasks) {
                loading = false;
                mFinishedTasks = 0;
                mNumberOfTasks = 0;

                if (!uploading) {
                    handleBitmapsMap();
                }

                int stringResource = uploading ? R.string.backup_success_snackbar : R.string.import_success_snackbar;
                Snackbar.make(getView(), stringResource, Snackbar.LENGTH_SHORT).show();

                getMainActivity().stopLoading();
            }
        }
    }

    private void handleBitmapsMap() {
        for (Map.Entry<String, Bitmap> entry : mBitmapsMap.entrySet()) {
            String title = entry.getKey().replace(" image", ""); // remove image mark -> receive task name;
            Bitmap bitmap = entry.getValue();

            RealmDB.get().executeTransaction(realm -> {
                RealmQuery<TaskModel> query = realm.where(TaskModel.class).contains("taskName", title);
                TaskModel task = query.findFirst();
                if (task != null) {
                    String url = task.getImageUrl();
                    if (!TextUtils.isEmpty(url)) {
                        FileOutputStream out = null;
                        File photoFile;

                        try {
                            photoFile = createImageFile(title);
                            String photoPath = photoFile.getAbsolutePath();

                            out = new FileOutputStream(photoPath);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                            bitmap.recycle();

                            task.setImageUrl(photoPath);
                            realm.copyToRealmOrUpdate(task);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (out != null) {
                                    out.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

            });
        }

        mBitmapsMap.clear();
    }

    private File createImageFile(String title) throws IOException {
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                title,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }

    private List<TaskModel> loadTasks() {
        Realm realm = RealmDB.get();
        final RealmResults<TaskModel> tasks = realm.where(TaskModel.class).findAll().sort("startDate");

        return tasks;
    }
}