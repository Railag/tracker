package com.firrael.tracker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.View;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;

import static android.app.Activity.RESULT_OK;

/**
 * Created by railag on 15.01.2018.
 */

public class AttachFragment extends SimpleFragment {

    private final static String TAG = AttachFragment.class.getSimpleName();

    public static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final int REQUEST_DOCUMENT_SCAN = 2;

    private final static String KEY_TASK_NAME = "keyTaskName";

    public static AttachFragment newInstance(String taskName) {

        Bundle args = new Bundle();
        args.putString(KEY_TASK_NAME, taskName);

        AttachFragment fragment = new AttachFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private ImageView mTakePictureIcon;
    private ImageView mScanDocumentIcon;

    private ImageView mTakePicturePreview;
    private TextView mScanDataPreview;


    private String mTaskName;

    private boolean mHasPicture;
    private boolean mHasScan;

    private String mCurrentPhotoPath;

    private List<String> mCurrentScanData;

    @Override
    protected String getTitle() {
        return getString(R.string.attach_document);
    }

    @Override
    protected int getViewId() {
        return R.layout.fragment_attach_document;
    }

    @Override
    protected void initView(View v) {
        mTakePictureIcon = v.findViewById(R.id.take_picture_icon);
        mScanDocumentIcon = v.findViewById(R.id.scan_document_icon);

        mTakePicturePreview = v.findViewById(R.id.take_picture_preview);
        mScanDataPreview = v.findViewById(R.id.scan_data_preview);

        mTakePictureIcon.setOnClickListener(v1 -> takePicture());
        mScanDocumentIcon.setOnClickListener(v1 -> scanDocument());

        getMainActivity().setupFab(view -> done(), MainActivity.FAB_DONE);

        Bundle args = getArguments();
        if (args != null) {
            mTaskName = args.getString(KEY_TASK_NAME, "");
        }
    }

    private void takePicture() {
        if (!Utils.checkCameraPermission(getActivity())) {
            Utils.verifyCameraPermission(getActivity());
            return;
        }

        if (!Utils.checkDiskPermission(getActivity())) {
            Utils.verifyStoragePermissions(getActivity());
            return;
        }

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
        if (!Utils.checkCameraPermission(getActivity())) {
            Utils.verifyCameraPermission(getActivity());
            return;
        }

        if (!Utils.checkDiskPermission(getActivity())) {
            Utils.verifyStoragePermissions(getActivity());
            return;
        }

        Intent intent = new Intent(getMainActivity(), OpenCVActivity.class);
        startActivityForResult(intent, REQUEST_DOCUMENT_SCAN);
    }

    private void done() {
        Realm realm = RealmDB.get();

        DateFormat formatter = new SimpleDateFormat(Utils.DATE_FORMAT, Locale.getDefault());
        String currentDate = formatter.format(new Date());

        realm.executeTransaction(realm1 -> {
            Number currentIdNum = realm.where(TaskModel.class).max("id");
            int nextId;
            if (currentIdNum == null) {
                nextId = 1;
            } else {
                nextId = currentIdNum.intValue() + 1;
            }

            TaskModel taskModel = new TaskModel(nextId, mTaskName, currentDate, TaskModel.STATUS_IN_PROGRESS);

            if (mHasPicture) {
                if (!TextUtils.isEmpty(mCurrentPhotoPath)) {
                    taskModel.setImageUrl(mCurrentPhotoPath);
                }
            }

            if (mHasScan) {
                taskModel.setOpenCVScanData(mCurrentScanData);
            }

            realm1.copyToRealmOrUpdate(taskModel);
        });

        getMainActivity().toLanding();
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
        for (String line : openCVData) {
            builder.append(line);
            builder.append("\n");
        }

        mScanDataPreview.setText(builder.toString());
    }
}