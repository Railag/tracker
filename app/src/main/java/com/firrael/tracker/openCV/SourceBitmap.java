package com.firrael.tracker.openCV;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by railag on 14.05.2018.
 */
public class SourceBitmap implements Parcelable {
    private String name;
    private Bitmap bitmap;

    public SourceBitmap(String name, Bitmap bitmap) {
        this.name = name;
        this.bitmap = bitmap;
    }

    protected SourceBitmap(Parcel in) {
        name = in.readString();
        bitmap = in.readParcelable(Bitmap.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeParcelable(bitmap, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SourceBitmap> CREATOR = new Creator<SourceBitmap>() {
        @Override
        public SourceBitmap createFromParcel(Parcel in) {
            return new SourceBitmap(in);
        }

        @Override
        public SourceBitmap[] newArray(int size) {
            return new SourceBitmap[size];
        }
    };

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
