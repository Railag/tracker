package com.firrael.tracker;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firrael.tracker.realm.TaskModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by railag on 04.01.2018.
 */

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private List<TaskModel> tasks = new ArrayList<>();

    public void setTasks(List<TaskModel> tasks) {
        this.tasks = tasks;
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
        TaskModel task = tasks.get(position);

        holder.mTaskName.setText(task.getTask());
        holder.mStartDate.setText(task.getStartDate());
        holder.mEndDate.setText(task.getEndDate());
        // TODO switch image based on status: holder.mStatus.setImageDrawable();
        // TODO add Glide for caching
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

        public ViewHolder(View itemView) {
            super(itemView);
            mTaskName = itemView.findViewById(R.id.taskName);
            mStartDate = itemView.findViewById(R.id.startDate);
            mEndDate = itemView.findViewById(R.id.endDate);
            mStatus = itemView.findViewById(R.id.status);
        }
    }
}
