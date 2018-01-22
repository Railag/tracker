package com.firrael.tracker;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firrael.tracker.base.SimpleFragment;
import com.firrael.tracker.openCV.OpenCVActivity;
import com.firrael.tracker.realm.RealmDB;
import com.firrael.tracker.realm.TaskModel;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmResults;

import static android.app.Activity.RESULT_OK;
import static com.firrael.tracker.AttachFragment.REQUEST_DOCUMENT_SCAN;
import static com.firrael.tracker.AttachFragment.REQUEST_IMAGE_CAPTURE;

/**
 * Created by railag on 16.01.2018.
 */

public class EditTaskFragment extends SimpleFragment {

    private final static String TAG = EditTaskFragment.class.getSimpleName();

    private final static String KEY_TASK = "keyTask";

    public static EditTaskFragment newInstance(TaskModel task) {

        Bundle args = new Bundle();
        args.putParcelable(KEY_TASK, task);

        EditTaskFragment fragment = new EditTaskFragment();
        fragment.setArguments(args);
        return fragment;
    }


    private TaskModel mTask;

    private EditText mTaskEdit;
    private ImageView mTakePictureIcon;
    private ImageView mScanDocumentIcon;
    private TextView mStartDateView;
    private TextView mEndDateView;
    private Button mDoneButton;
    private Button mRemoveButton;
    private ImageView mTakePicturePreview;
    private TextView mScanDataPreview;

    private boolean mHasPicture;
    private boolean mHasScan;

    private String mCurrentPhotoPath;

    private List<String> mCurrentScanData;

    @Override
    protected String getTitle() {
        return getString(R.string.edit_task);
    }

    @Override
    protected int getViewId() {
        return R.layout.fragment_edit_task;
    }

    @Override
    protected void initView(View v) {
        mTakePictureIcon = v.findViewById(R.id.take_picture_icon);
        mScanDocumentIcon = v.findViewById(R.id.scan_document_icon);
        mTaskEdit = v.findViewById(R.id.task_name);
        mStartDateView = v.findViewById(R.id.start_date);
        mEndDateView = v.findViewById(R.id.end_date);
        mDoneButton = v.findViewById(R.id.done_button);
        mRemoveButton = v.findViewById(R.id.remove_button);
        mTakePicturePreview = v.findViewById(R.id.take_picture_preview);
        mScanDataPreview = v.findViewById(R.id.scan_data_preview);

        mTakePictureIcon.setOnClickListener(v1 -> takePicture());
        mScanDocumentIcon.setOnClickListener(v1 -> scanDocument());

        getMainActivity().setupFab(view -> saveUpdates(), MainActivity.FAB_DONE);

        Bundle args = getArguments();
        if (args != null && args.containsKey(KEY_TASK)) {
            mTask = args.getParcelable(KEY_TASK);

            if (mTask == null) {
                throw new IllegalArgumentException("Task must be present for EditTaskFragment");
            }

            mTaskEdit.setText(mTask.getTaskName());

            init();
        }
    }

    private void init() {
        mStartDateView.setText(String.format("Started %s", Utils.formatUIDate(mTask.getStartDate())));
        if (!TextUtils.isEmpty(mTask.getEndDate())) {
            mEndDateView.setText(String.format("Finished %s", Utils.formatUIDate(mTask.getEndDate())));
        }

        switch (mTask.getStatus()) {
            case TaskModel.STATUS_IN_PROGRESS:
                mDoneButton.setVisibility(View.VISIBLE);
                mDoneButton.setOnClickListener(v12 -> {
                    RealmDB.get().executeTransaction(realm -> {
                        mTask.done();
                        realm.copyToRealmOrUpdate(mTask);
                    });
                    init();
                });
                break;
            case TaskModel.STATUS_FINISHED:
                mDoneButton.setVisibility(View.GONE);
                break;
        }

        mRemoveButton.setOnClickListener(view -> {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.remove_this_task)
                    .setPositiveButton(R.string.remove, (dialogInterface, i) -> {
                        RealmDB.get().executeTransaction(realm -> {
                            RealmResults<TaskModel> result = realm.where(TaskModel.class).equalTo("id", mTask.getId()).findAll();
                            result.deleteAllFromRealm();
                        });
                        getMainActivity().toLanding();
                    })
                    .setNegativeButton(R.string.close, (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                    })
                    .show();
        });

        if (mTask.hasImage()) {
            loadImage(mTask.getImageUrl());
        }

        if (mTask.hasScan()) {
            loadScan(mTask.getOpenCVScanData());
        }
    }

    private void saveUpdates() {
        String newName = mTaskEdit.getText().toString();

        RealmDB.get().executeTransaction(realm -> {
            if (!TextUtils.isEmpty(newName)) {
                mTask.setTaskName(newName);
            }

            if (mHasPicture) {
                if (!TextUtils.isEmpty(mCurrentPhotoPath)) {
                    mTask.setImageUrl(mCurrentPhotoPath);
                }
            }

            if (mHasScan) {
                mTask.setOpenCVScanData(mCurrentScanData);
            }

            realm.copyToRealmOrUpdate(mTask);
        });

        getMainActivity().toLanding();
    }

    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(getActivity(), R.string.error_photo, Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(getActivity(),
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = Utils.getCurrentTimestamp();
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void scanDocument() {
        Intent intent = new Intent(getMainActivity(), OpenCVActivity.class);
        startActivityForResult(intent, REQUEST_DOCUMENT_SCAN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    mTakePicturePreview.setImageBitmap(imageBitmap);
                }
            } else {
                loadImage(mCurrentPhotoPath);
                mHasPicture = true;
            }
        } else if (requestCode == REQUEST_DOCUMENT_SCAN && resultCode == RESULT_OK) {
            if (data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    mCurrentScanData = extras.getStringArrayList(OpenCVActivity.KEY_SCAN_RESULTS);
                    if (mCurrentScanData != null && mCurrentScanData.size() > 0) {
                        loadScan(mCurrentScanData);
                        mHasScan = true;
                    }
                }
            }
        }
    }

    private void loadImage(String url) {
        Uri uri = Uri.fromFile(new File(url));
        Glide.with(this).load(uri).into(mTakePicturePreview);
    }

    private void loadScan(List<String> openCVData) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < openCVData.size(); i++) {
            String line = openCVData.get(i);
            builder.append("region#");
            builder.append(i);
            builder.append(": ");
            builder.append(line);
            builder.append("\n");
        }

        mScanDataPreview.setText(builder.toString());
    }
}