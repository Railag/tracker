package com.firrael.tracker.openCV;

import android.graphics.Bitmap;

import java.util.List;

/**
 * Created by railag on 07.02.2018.
 */

class BitmapResults {
    private List<Bitmap> regions;
    private Bitmap sourceBitmap;

    public BitmapResults(List<Bitmap> regions, Bitmap sourceBitmap) {
        this.regions = regions;
        this.sourceBitmap = sourceBitmap;
    }

    public List<Bitmap> getRegions() {
        return regions;
    }

    public void setRegions(List<Bitmap> regions) {
        this.regions = regions;
    }

    public Bitmap getSourceBitmap() {
        return sourceBitmap;
    }

    public void setSourceBitmap(Bitmap sourceBitmap) {
        this.sourceBitmap = sourceBitmap;
    }
}
