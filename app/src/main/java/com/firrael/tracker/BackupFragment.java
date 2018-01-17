package com.firrael.tracker;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.firrael.tracker.base.SimpleFragment;
import com.firrael.tracker.realm.RealmDB;
import com.firrael.tracker.realm.TaskModel;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by railag on 17.01.2018.
 */

public class BackupFragment extends SimpleFragment {

    public static BackupFragment newInstance() {

        Bundle args = new Bundle();

        BackupFragment fragment = new BackupFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private ImageButton uploadButton;
    private ImageButton importButton;

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
        // TODO
    }

    private List<TaskModel> loadTasks() {
        Realm realm = RealmDB.get();
        final RealmResults<TaskModel> tasks = realm.where(TaskModel.class).findAll().sort("startDate");

        return tasks;
    }
}
