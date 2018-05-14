package com.firrael.tracker;

import android.graphics.Bitmap;
import android.util.Log;

import com.firrael.tracker.openCV.Kernel;
import com.firrael.tracker.openCV.SourceBitmap;

import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.features2d.MSER;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.firrael.tracker.openCV.OpenCVActivity.CONTOUR_COLOR;
import static org.opencv.android.Utils.bitmapToMat;
import static org.opencv.android.Utils.matToBitmap;

/**
 * Created by railag on 19.02.2018.
 */

public class OpenCVUtils {
    private final static String TAG = OpenCVUtils.class.getSimpleName();

    public static Bitmap createBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
        matToBitmap(mat, bitmap);
        return bitmap;
    }

    public static SourceBitmap createSourceBitmap(String name, Mat mat) {
        Bitmap bitmap = createBitmap(mat);
        return new SourceBitmap(name, bitmap);
    }

    public static Mat createMat(Bitmap bitmap) {
        Mat mat = new Mat();
        bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        return mat;
    }

    public static List<MatOfPoint> detectRegions(MSER detector, Mat mat) {
        MatOfKeyPoint keyPoint = new MatOfKeyPoint();
        List<KeyPoint> listPoint;
        KeyPoint kPoint;
        Mat mask = Mat.zeros(mat.size(), CvType.CV_8UC1);
        int rectanX1, rectanY1, rectanX2, rectanY2;

        List<MatOfPoint> contours = new ArrayList<>();
        Mat kernel = Kernel.LARGE.generate();
        Mat morbyte = new Mat();
        Mat hierarchy = new Mat();

        //

        detector.detect(mat, keyPoint);
        listPoint = keyPoint.toList();

        for (int i = 0; i < listPoint.size(); i++) {
            kPoint = listPoint.get(i);
            rectanX1 = (int) (kPoint.pt.x - 0.5 * kPoint.size);
            rectanY1 = (int) (kPoint.pt.y - 0.5 * kPoint.size);
            rectanX2 = (int) (kPoint.size);
            rectanY2 = (int) (kPoint.size);
            if (rectanX1 <= 0)
                rectanX1 = 1;
            if (rectanY1 <= 0)
                rectanY1 = 1;
            if ((rectanX1 + rectanX2) > mat.width())
                rectanX2 = mat.width() - rectanX1;
            if ((rectanY1 + rectanY2) > mat.height())
                rectanY2 = mat.height() - rectanY1;
            Rect borderRect = new Rect(rectanX1, rectanY1, rectanX2, rectanY2);
            try {
                Mat roi = new Mat(mask, borderRect);
                roi.setTo(CONTOUR_COLOR);
            } catch (Exception ex) {
                Log.d(TAG, "mat roi error " + ex.getMessage());
            }
        }

        Imgproc.morphologyEx(mask, morbyte, Imgproc.MORPH_DILATE, kernel); // dilate filter


        Imgproc.findContours(morbyte, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        Collections.reverse(contours);

        return contours;
    }

    public static Mat dilate(Mat mat) {
        Mat kernel = Kernel.TINY.generate();
        Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_DILATE, kernel);
        return mat;
    }

    public static Mat erode(Mat mat) {
        Mat kernel = Kernel.TINY.generate();
        Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_ERODE, kernel);
        return mat;
    }

    public static Mat close(Mat mat) {
        Mat kernel = Kernel.TINY.generate();
        Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_CLOSE, kernel);
        return mat;
    }

    public static Mat open(Mat mat) {
        Mat kernel = Kernel.TINY.generate();
        Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_OPEN, kernel);
        return mat;
    }

    public static Mat deskew(Mat src, double angle) {
        Point center = new Point(src.width() / 2, src.height() / 2);
        Mat rotImage = Imgproc.getRotationMatrix2D(center, angle, 1.0);
        //1.0 means 100 % scale
        Size size = new Size(src.width(), src.height());
        Imgproc.warpAffine(src, src, rotImage, size, Imgproc.INTER_LINEAR + Imgproc.CV_WARP_FILL_OUTLIERS);
        return src;
    }
}