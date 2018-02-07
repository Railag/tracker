package com.firrael.tracker.tesseract;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import io.realm.RealmObject;

/**
 * Created by railag on 07.02.2018.
 */

public class RecognizedRegion extends RealmObject implements Parcelable, Comparable<RecognizedRegion> {

    private int regionNumber;
    private String recognizedLine;

    public RecognizedRegion(int regionNumber, String result) {
        this.regionNumber = regionNumber;
        this.recognizedLine = result;
    }

    public RecognizedRegion() {
        recognizedLine = "";
    }

    protected RecognizedRegion(Parcel in) {
        regionNumber = in.readInt();
        recognizedLine = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(regionNumber);
        dest.writeString(recognizedLine);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RecognizedRegion> CREATOR = new Creator<RecognizedRegion>() {
        @Override
        public RecognizedRegion createFromParcel(Parcel in) {
            return new RecognizedRegion(in);
        }

        @Override
        public RecognizedRegion[] newArray(int size) {
            return new RecognizedRegion[size];
        }
    };

    public String getRecognizedLine() {
        return recognizedLine;
    }

    public void setRecognizedLine(String recognizedLine) {
        this.recognizedLine = recognizedLine;
    }

    public int getRegionNumber() {
        return regionNumber;
    }

    public void setRegionNumber(int regionNumber) {
        this.regionNumber = regionNumber;
    }

    @Override
    public String toString() {
        return recognizedLine;
    }

    @Override
    public int compareTo(@NonNull RecognizedRegion o) {
        return Integer.compare(regionNumber, o.getRegionNumber());
    }
}