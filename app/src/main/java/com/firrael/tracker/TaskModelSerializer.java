package com.firrael.tracker;

import com.firrael.tracker.realm.TaskModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Created by railag on 17.01.2018.
 */

class TaskModelSerializer implements JsonSerializer<TaskModel> {

    @Override
    public JsonElement serialize(TaskModel task, Type typeOfSrc, JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", task.getId());
        jsonObject.addProperty("task_name", task.getTaskName());
        jsonObject.addProperty("status", task.getStatus());
        jsonObject.addProperty("start_date", task.getStartDate());
        jsonObject.addProperty("end_date", task.getEndDate());
        jsonObject.addProperty("image_url", task.getImageUrl());
        JsonArray scanData = new JsonArray();
        for (String data : task.getOpenCVScanData()) {
            scanData.add(data);
        }
        jsonObject.add("opencv_data", scanData);
        return jsonObject;
    }
}