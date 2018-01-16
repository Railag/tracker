package com.firrael.tracker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import com.firrael.tracker.base.SimpleFragment;
import com.firrael.tracker.realm.RealmDB;
import com.firrael.tracker.realm.TaskModel;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;

import static android.app.Activity.RESULT_OK;

/**
 * Created by railag on 15.01.2018.
 */

public class AttachFragment extends SimpleFragment {

    private final static String TAG = AttachFragment.class.getSimpleName();

    private static final int REQUEST_IMAGE_CAPTURE = 1;

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

    private String taskName;

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

        mTakePictureIcon.setOnClickListener(v1 -> takePicture());
        mScanDocumentIcon.setOnClickListener(v1 -> scanDocument());

        getMainActivity().setupFab(view -> done());

        Bundle args = getArguments();
        if (args != null) {
            taskName = args.getString(KEY_TASK_NAME, "");
        }
    }

    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void scanDocument() {
        // TODO add handling

        getMainActivity().toOpenCV();
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

            TaskModel taskModel = new TaskModel(nextId, taskName, currentDate, TaskModel.STATUS_IN_PROGRESS);

            boolean hasPicture = false;
            if (hasPicture) {
                // TODO add picture to model
            }

            boolean hasScan = false;
            if (hasScan) {
                // TODO add scan to model
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
            }
        }
    }

}