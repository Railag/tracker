package com.firrael.tracker.realm;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;

/**
 * Created by railag on 29.12.2017.
 */

public class TaskModel extends RealmObject {
    private String task;

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
}