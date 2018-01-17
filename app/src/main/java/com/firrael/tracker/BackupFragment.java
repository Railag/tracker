package com.firrael.tracker;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageButton;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.firrael.tracker.base.SimpleFragment;
import com.firrael.tracker.realm.RealmDB;
import com.firrael.tracker.realm.TaskModel;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;

/**
 * Created by railag on 17.01.2018.
 */

public class BackupFragment extends SimpleFragment {
    private final static String TAG = BackupFragment.class.getSimpleName();

    public static BackupFragment newInstance() {

        Bundle args = new Bundle();

        BackupFragment fragment = new BackupFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private ImageButton uploadButton;
    private ImageButton importButton;

    private DriveResourceClient mDriveResourceClient;

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

        getMainActivity().showToolbar();

        getMainActivity().hideFab();

        mDriveResourceClient = App.getDrive();

        uploadButton.setOnClickListener(view -> {
            upload();
        });

        importButton.setOnClickListener(view -> {
            importTasks();
        });
    }

    private void importTasks() {
        // TODO
    }

    private void upload() {
        List<TaskModel> tasks = loadTasks();


        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        final String folderName = timeStamp + " tasks";

        Task<DriveFolder> folderTask = createGoogleDriveFolder(folderName);
        Tasks.whenAll(folderTask).continueWith((Continuation<Void, Void>) task -> {
            DriveFolder parentFolder = folderTask.getResult();

            Task<MetadataBuffer> metadataTask = getMetadataForFolder(folderName);
            Tasks.whenAll(metadataTask).continueWithTask(task2 -> {
                MetadataBuffer buffer = metadataTask.getResult();

                DriveFolder rootDriveFolder = getDriveFolder(buffer, folderName);

                for (int i = 0; i < tasks.size(); i++) {
                    addTaskToGoogleDrive(tasks.get(i), rootDriveFolder);
                }

                return null;
            });

            return null;
        });
    }

    private Task<MetadataBuffer> getMetadataForFolder(String folderName) {
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, folderName))
                .build();
        Task<MetadataBuffer> folders = mDriveResourceClient.query(query);
        return folders;
    }

    private DriveFolder getDriveFolder(MetadataBuffer metadataBuffer, String folderName) {
        Metadata folderMetadata = null;
        for (int i = 0; i < metadataBuffer.getCount(); i++) {
            Metadata metadata = metadataBuffer.get(i);
            if (metadata.getTitle().equalsIgnoreCase(folderName)) {
                folderMetadata = metadata;
                break;
            }
        }

        DriveFolder folder = folderMetadata.getDriveId().asDriveFolder();
        return folder;
    }

    private void addTaskToGoogleDrive(TaskModel taskModel, DriveFolder folder) {
        final Task<DriveContents> createJsonTask = mDriveResourceClient.createContents();
        final Task<DriveContents> createImageTask = mDriveResourceClient.createContents();

        Task<DriveFolder> folderTask = createGoogleDriveFolder(taskModel.getTaskName(), folder);
        Tasks.whenAll(folderTask, createJsonTask, createImageTask).continueWith((Continuation<Void, Void>) task -> {

            Task<MetadataBuffer> metadataBufferTask = getMetadataForFolder(taskModel.getTaskName());

            Tasks.whenAll(metadataBufferTask).continueWith((Continuation<Void, Void>) task2 -> {
                Task<MetadataBuffer> metadataTask = getMetadataForFolder(taskModel.getTaskName());

                Tasks.whenAll(metadataTask).continueWith((Continuation<Void, Void>) task3 -> {
                    MetadataBuffer metadata = metadataTask.getResult();
                    DriveFolder taskFolder = getDriveFolder(metadata, taskModel.getTaskName());

                    if (taskModel.hasImage()) {
                        DriveContents contents = createImageTask.getResult();
                        OutputStream outputStream = contents.getOutputStream();
                        Glide.with(this).load(taskModel.getImageUrl()).into(new SimpleTarget<Drawable>() {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                if (resource instanceof BitmapDrawable) {
                                    BitmapDrawable bitmapDrawable = (BitmapDrawable) resource;
                                    Bitmap bitmap = bitmapDrawable.getBitmap();
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

                                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                            .setTitle(taskModel.getTaskName() + " image")
                                            .setMimeType("image/jpeg")
                                            .build();

                                    // TODO    metadataBuffer.release();

                                    mDriveResourceClient.createFile(taskFolder, changeSet, contents);
                                }
                            }
                        });
                    }

                    DriveContents jsonFile = createJsonTask.getResult();
                    OutputStream outputStream = jsonFile.getOutputStream();

                    Gson gson = new GsonBuilder()
                            .setExclusionStrategies(new ExclusionStrategy() {
                                @Override
                                public boolean shouldSkipField(FieldAttributes f) {
                                    return f.getDeclaringClass().equals(RealmObject.class);
                                }

                                @Override
                                public boolean shouldSkipClass(Class<?> clazz) {
                                    return false;
                                }
                            })
                            .registerTypeAdapter(TaskModel.class, new TaskModelSerializer())
                            .create();

                    String json = gson.toJson(RealmDB.get().copyFromRealm(taskModel));

                    outputStream.write(json.getBytes());

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(taskModel.getTaskName())
                            .setMimeType("text/json")
                            .build();

                    // TODO    metadataBuffer.release();

                    mDriveResourceClient.createFile(taskFolder, changeSet, jsonFile);

                    return null;
                });

                return null;
            });

            return null;
        });
    }

    private Task<DriveFolder> createGoogleDriveFolder(String name) {
        final Task<DriveFolder> rootFolderTask = mDriveResourceClient.getRootFolder();
        rootFolderTask.continueWithTask(task -> {
            DriveFolder parentFolder = task.getResult();
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(name)
                    .setMimeType(DriveFolder.MIME_TYPE)
                    .setStarred(true)
                    .build();
            return mDriveResourceClient.createFolder(parentFolder, changeSet);
        });
        return rootFolderTask;
    }


    private Task<DriveFolder> createGoogleDriveFolder(String name, DriveFolder driveFolder) {
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(name)
                .setMimeType(DriveFolder.MIME_TYPE)
                .setStarred(true)
                .build();
        return mDriveResourceClient.createFolder(driveFolder, changeSet);
    }

    private List<TaskModel> loadTasks() {
        Realm realm = RealmDB.get();
        final RealmResults<TaskModel> tasks = realm.where(TaskModel.class).findAll().sort("startDate");

        return tasks;
    }
}