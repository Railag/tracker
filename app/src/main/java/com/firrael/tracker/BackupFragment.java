package com.firrael.tracker;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.ImageButton;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.firrael.tracker.base.SimpleFragment;
import com.firrael.tracker.realm.RealmDB;
import com.firrael.tracker.realm.TaskModel;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
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

    private int mNumberOfTasks;
    private int mFinishedTasks;
    private boolean loading;

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
        getMainActivity().startLoading();
        loading = true;

        List<TaskModel> tasks = loadTasks();

        mNumberOfTasks = tasks.size();
        for (TaskModel taskModel : tasks) {
            if (taskModel.hasImage()) {
                mNumberOfTasks++;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        final String folderName = timeStamp + " tasks";

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
                                    verifyProgress();
                                });
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

                metadata.release();

                Task<DriveFile> lastTask = mDriveResourceClient.createFile(taskFolder, changeSet, jsonFile);
                lastTask.addOnCompleteListener(task1 -> {
                    verifyProgress();
                });
                return null;
            });

            return null;
        });
    }

    private void verifyProgress() {
        if (loading) {
            mFinishedTasks++;
            if (mFinishedTasks >= mNumberOfTasks) {
                loading = false;
                mFinishedTasks = 0;
                mNumberOfTasks = 0;
                Snackbar.make(getView(), R.string.backup_success_snackbar, Snackbar.LENGTH_SHORT).show();
                getMainActivity().stopLoading();
            }
        }
    }

    private List<TaskModel> loadTasks() {
        Realm realm = RealmDB.get();
        final RealmResults<TaskModel> tasks = realm.where(TaskModel.class).findAll().sort("startDate");

        return tasks;
    }
}