package com.firrael.tracker.openCV;

import android.graphics.Bitmap;

import java.util.List;

/**
 * Created by railag on 07.02.2018.
 */

class BitmapResults {
    private List<Bitmap> regions;
    private List<Bitmap> sourceBitmaps;

    public BitmapResults(List<Bitmap> regions, List<Bitmap> sourceBitmaps) {
        this.regions = regions;
        this.sourceBitmaps = sourceBitmaps;
    }

    public List<Bitmap> getRegions() {
        return regions;
    }

    public void setRegions(List<Bitmap> regions) {
        this.regions = regions;
    }

    public List<Bitmap> getSourceBitmaps() {
        return sourceBitmaps;
    }

    public void setSourceBitmaps(List<Bitmap> sourceBitmaps) {
        this.sourceBitmaps = sourceBitmaps;
    }
}
