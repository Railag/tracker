package com.firrael.tracker.realm;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.firrael.tracker.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by railag on 29.12.2017.
 */

public class TaskModel extends RealmObject implements Parcelable {
    public final static int STATUS_IN_PROGRESS = 0;
    public final static int STATUS_FINISHED = 1;

    @PrimaryKey
    private int id;

    private String taskName;
    private String startDate;
    private String endDate;
    private int status;

    // attached content
    private String imageUrl;
    private RealmList<String> openCVScanData;

    public TaskModel() {
    }

    public TaskModel(int id, String taskName, String startDate, int status) {
        this.id = id;
        this.taskName = taskName;
        this.startDate = startDate;
        this.status = status;
    }

    protected TaskModel(Parcel in) {
        id = in.readInt();
        taskName = in.readString();
        startDate = in.readString();
        endDate = in.readString();
        status = in.readInt();
        imageUrl = in.readString();
        in.readStringList(openCVScanData);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(taskName);
        dest.writeString(startDate);
        dest.writeString(endDate);
        dest.writeInt(status);
        dest.writeString(imageUrl);
        dest.writeStringList(openCVScanData);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TaskModel> CREATOR = new Creator<TaskModel>() {
        @Override
        public TaskModel createFromParcel(Parcel in) {
            return new TaskModel(in);
        }

        @Override
        public TaskModel[] newArray(int size) {
            return new TaskModel[size];
        }
    };

    public void done() {
        this.status = STATUS_FINISHED;
        DateFormat formatter = new SimpleDateFormat(Utils.DATE_FORMAT, Locale.getDefault());
        this.endDate = formatter.format(new Date());
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getOpenCVScanData() {
        return openCVScanData;
    }

    public void setOpenCVScanData(List<String> openCVScanData) {
        this.openCVScanData = new RealmList<>();
        this.openCVScanData.addAll(openCVScanData);
    }

    public boolean hasImage() {
        return !TextUtils.isEmpty(imageUrl);
    }

    public boolean hasScan() {
        return openCVScanData != null && openCVScanData.size() > 0;
    }
}