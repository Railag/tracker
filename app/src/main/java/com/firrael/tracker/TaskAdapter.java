package com.firrael.tracker;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.firrael.tracker.realm.RealmDB;
import com.firrael.tracker.realm.TaskModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by railag on 04.01.2018.
 */

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private List<TaskModel> tasks = new ArrayList<>();
    private EditListener listener;

    interface EditListener {
        void edit(TaskModel task);
    }

    public void setTasks(List<TaskModel> tasks, EditListener listener) {
        this.tasks = tasks;
        this.listener = listener;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        Resources res = context.getResources();

        TaskModel task = tasks.get(position);

        holder.mTaskName.setText(task.getTaskName());
        holder.mStartDate.setText(task.getStartDate());
        holder.mEndDate.setText(task.getEndDate());
        switch (task.getStatus()) {  // TODO add Glide for caching
            case TaskModel.STATUS_IN_PROGRESS:
                holder.mStatus.setColorFilter(res.getColor(R.color.yellow));
                break;
            case TaskModel.STATUS_FINISHED:
                holder.mStatus.setColorFilter(res.getColor(R.color.green));
                break;
        }

        switch (task.getStatus()) {
            case TaskModel.STATUS_IN_PROGRESS:
                holder.mDoneButton.setVisibility(View.VISIBLE);
                holder.mDoneButton.setOnClickListener(v12 -> {
                    RealmDB.get().executeTransaction(realm -> {
                        task.done();
                        realm.copyToRealmOrUpdate(task);
                    });
                    notifyDataSetChanged();
                });
                break;
            case TaskModel.STATUS_FINISHED:
                holder.mDoneButton.setVisibility(View.GONE);
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.edit(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTaskName;
        TextView mStartDate;
        TextView mEndDate;
        ImageView mStatus;
        Button mDoneButton;

        public ViewHolder(View itemView) {
            super(itemView);
            mTaskName = itemView.findViewById(R.id.taskName);
            mStartDate = itemView.findViewById(R.id.startDate);
            mEndDate = itemView.findViewById(R.id.endDate);
            mStatus = itemView.findViewById(R.id.status);
            mDoneButton = itemView.findViewById(R.id.done_button);
        }
    }
}