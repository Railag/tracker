package com.firrael.tracker;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

        switch (circle) {

            case TOP_RIGHT:
                holder.circle.setRotation(180);
                break;
            case TOP_LEFT:
                holder.circle.setRotation(90);
                break;
            case DOWN_RIGHT:
                holder.circle.setRotation(270);
                break;
            case DOWN_LEFT:
            default:
                break;
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mCircle;

        public ViewHolder(View itemView) {
            super(itemView);
            mCircle = itemView.findViewById(R.id.)
        }
    }
}
