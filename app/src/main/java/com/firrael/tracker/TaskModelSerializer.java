package com.firrael.tracker;

import com.firrael.tracker.realm.TaskModel;
import com.firrael.tracker.tesseract.RecognizedRegion;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by railag on 17.01.2018.
 */

class TaskModelSerializer implements JsonSerializer<TaskModel>, JsonDeserializer<TaskModel> {

    private final static String ID = "id";
    private final static String TASK_NAME = "task_name";
    private final static String STATUS = "status";
    private final static String START_DATE = "start_date";
    private final static String END_DATE = "end_date";
    private final static String IMAGE_URL = "image_url";
    private final static String OPENCV_DATA = "opencv_data";
    private final static String REGION_ID = "region_id";
    private final static String REGION_LINE = "region_line";

    @Override
    public JsonElement serialize(TaskModel task, Type typeOfSrc, JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(ID, task.getId());
        jsonObject.addProperty(TASK_NAME, task.getTaskName());
        jsonObject.addProperty(STATUS, task.getStatus());
        jsonObject.addProperty(START_DATE, task.getStartDate());
        jsonObject.addProperty(END_DATE, task.getEndDate());
        jsonObject.addProperty(IMAGE_URL, task.getImageUrl());
        JsonArray scanData = new JsonArray();
        for (RecognizedRegion region : task.getOpenCVScanData()) {
            JsonObject regionObj = new JsonObject();
            regionObj.addProperty(REGION_ID, region.getRegionNumber());
            regionObj.addProperty(REGION_LINE, region.getRecognizedLine());
            scanData.add(regionObj);
        }
        jsonObject.add(OPENCV_DATA, scanData);
        return jsonObject;
    }

    @Override
    public TaskModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json == null) {
            return null;
        }

        JsonObject obj = json.getAsJsonObject();

        TaskModel taskModel = new TaskModel();
        if (obj.has(ID)) {
            taskModel.setId(obj.get(ID).getAsInt());
        }

        if (obj.has(TASK_NAME)) {
            taskModel.setTaskName(obj.get(TASK_NAME).getAsString());
        }

        if (obj.has(STATUS)) {
            taskModel.setStatus(obj.get(STATUS).getAsInt());
        }

        if (obj.has(START_DATE)) {
            taskModel.setStartDate(obj.get(START_DATE).getAsString());
        }

        if (obj.has(END_DATE)) {
            taskModel.setEndDate(obj.get(END_DATE).getAsString());
        }

        if (obj.has(IMAGE_URL)) {
            taskModel.setImageUrl(obj.get(IMAGE_URL).getAsString());
        }

        if (obj.has(OPENCV_DATA)) {
            List<RecognizedRegion> data = new ArrayList<>();
            JsonArray scanData = obj.getAsJsonArray(OPENCV_DATA);
            for (JsonElement regionElement : scanData) {
                RecognizedRegion region = new RecognizedRegion();
                JsonObject regionObj = regionElement.getAsJsonObject();

                if (regionObj.has(REGION_ID)) {
                    region.setRegionNumber(regionObj.get(REGION_ID).getAsInt());
                }

                if (regionObj.has(REGION_LINE)) {
                    region.setRecognizedLine(regionObj.get(REGION_LINE).getAsString());
                }

                data.add(region);
            }

            taskModel.setOpenCVScanData(data);
        }

        return taskModel;
    }
}