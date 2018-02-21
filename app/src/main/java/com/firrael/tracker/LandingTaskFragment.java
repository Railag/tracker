package com.firrael.tracker;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.firrael.tracker.base.SimpleFragment;
import com.firrael.tracker.realm.RealmDB;
import com.firrael.tracker.realm.TaskModel;

import org.w3c.dom.Text;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

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
    private TextView mEmptyMessage;

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
        mList = v.findViewById(R.id.task_list);
        mEmptyMessage = v.findViewById(R.id.empty_message);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        mList.setLayoutManager(linearLayoutManager);

        List<TaskModel> tasks = loadTasks();

        mAdapter = new TaskAdapter();

        if (tasks != null && tasks.size() > 0) {
            TaskAdapter.EditListener listener = task -> getMainActivity().toEditTask(RealmDB.get().copyFromRealm(task));
            mAdapter.setTasks(tasks, listener);

            mList.setAdapter(mAdapter);

            mList.setVisibility(View.VISIBLE);
            mEmptyMessage.setVisibility(View.GONE);
        } else {
            mList.setVisibility(View.GONE);
            mEmptyMessage.setVisibility(View.VISIBLE);
        }

        getMainActivity().setupFab(v1 -> getMainActivity().toNewTask(), MainActivity.FAB_NEW);
    }

    private List<TaskModel> loadTasks() {
        Realm realm = RealmDB.get();
        final RealmResults<TaskModel> tasks = realm.where(TaskModel.class).findAll().sort("startDate", Sort.DESCENDING);

        return tasks;
    }
}
