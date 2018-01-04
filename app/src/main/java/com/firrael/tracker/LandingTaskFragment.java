package com.firrael.tracker;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.firrael.tracker.base.SimpleFragment;
import com.firrael.tracker.realm.RealmDB;
import com.firrael.tracker.realm.TaskModel;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

/**
 * Created by railag on 04.01.2018.
 */

public class LandingTaskFragment extends SimpleFragment {

    public static LandingTaskFragment newInstance() {

        Bundle args = new Bundle();

        LandingTaskFragment fragment = new LandingTaskFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private RecyclerView mList;

    private TaskAdapter mAdapter;

    @Override
    protected String getTitle() {
        return getString(R.string.app_name);
    }

    @Override
    protected int getViewId() {
        return R.layout.fragment_landing;
    }

    @Override
    protected void initView(View v) {
        mList = v.findViewById(R.id.taskList);

        List<TaskModel> tasks = loadTasks();

        mAdapter = new TaskAdapter();
        mAdapter.setTasks(tasks);

        mList.setAdapter(mAdapter);
    }

    private List<TaskModel> loadTasks() {
        Realm realm = RealmDB.get();
        // TODO fetch recent tasks

        List<TaskModel> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(new TaskModel("task #" + i));
        }

        return tasks;
    }
}
