package com.firrael.tracker.openCV;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by railag on 07.02.2018.
 */

public class BitmapRegion implements Parcelable {
    private int regionNumber;
    private Bitmap bitmap;

    public BitmapRegion(int regionNumber, Bitmap bitmap) {
        this.regionNumber = regionNumber;
        this.bitmap = bitmap;
    }

    protected BitmapRegion(Parcel in) {
        regionNumber = in.readInt();
        bitmap = in.readParcelable(Bitmap.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(regionNumber);
        dest.writeParcelable(bitmap, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BitmapRegion> CREATOR = new Creator<BitmapRegion>() {
        @Override
        public BitmapRegion createFromParcel(Parcel in) {
            return new BitmapRegion(in);
        }

        @Override
        public BitmapRegion[] newArray(int size) {
            return new BitmapRegion[size];
        }
    };

    public int getRegionNumber() {
        return regionNumber;
    }

    public void setRegionNumber(int regionNumber) {
        this.regionNumber = regionNumber;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
}
