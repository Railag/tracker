package com.firrael.tracker;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import static org.opencv.android.Utils.bitmapToMat;
import static org.opencv.android.Utils.matToBitmap;

/**
 * Created by railag on 19.02.2018.
 */

public class OpenCVUtils {
    public static Bitmap createBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
        matToBitmap(mat, bitmap);
        return bitmap;
    }

    public static Mat createMat(Bitmap bitmap) {
        Mat mat = new Mat();
        bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        return mat;
    }
}