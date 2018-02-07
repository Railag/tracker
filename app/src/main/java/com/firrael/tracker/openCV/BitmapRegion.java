package com.firrael.tracker.openCV;

import android.graphics.Bitmap;

/**
 * Created by railag on 07.02.2018.
 */

public class BitmapRegion {
    private int regionNumber;
    private Bitmap bitmap;

    public BitmapRegion(int regionNumber, Bitmap bitmap) {
        this.regionNumber = regionNumber;
        this.bitmap = bitmap;
    }

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
