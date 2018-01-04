package com.firrael.tracker.realm;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;

/**
 * Created by railag on 29.12.2017.
 */

public class TaskModel extends RealmObject {
    private String task;
    private String startDate;
    private String endDate;
    private int status;

    public TaskModel() {
    }

    public TaskModel(String task) {
        this.task = task;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}