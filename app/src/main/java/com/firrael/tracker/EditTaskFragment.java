package com.firrael.tracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firrael.tracker.base.SimpleFragment;
import com.firrael.tracker.realm.RealmDB;
import com.firrael.tracker.realm.TaskModel;

import io.realm.Realm;

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

        mTakePictureIcon.setOnClickListener(v1 -> takePicture());
        mScanDocumentIcon.setOnClickListener(v1 -> scanDocument());

        getMainActivity().setupFab(view -> saveUpdates());

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
        mStartDateView.setText(mTask.getStartDate());
        mEndDateView.setText(mTask.getEndDate());

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
    }

    private void takePicture() {
        // TODO
    }

    private void scanDocument() {
        // TODO add handling

        getMainActivity().toOpenCV();
    }

    private void saveUpdates() {
        String newName = mTaskEdit.getText().toString();


        RealmDB.get().executeTransaction(realm -> {
            if (!TextUtils.isEmpty(newName)) {
                mTask.setTaskName(newName);
            }

            boolean hasPicture = false;
            if (hasPicture) {
                // TODO add picture to model
            }

            boolean hasScan = false;
            if (hasScan) {
                // TODO add scan to model
            }

            realm.copyToRealmOrUpdate(mTask);
        });

        getMainActivity().toLanding();
    }
}
