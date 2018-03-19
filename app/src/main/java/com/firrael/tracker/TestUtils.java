package com.firrael.tracker;

import android.os.Environment;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;

/**
 * Created by railag on 21.02.2018.
 */

public class TestUtils {
    private static final String TAG = TestUtils.class.getSimpleName();
    final static String TEST_IMAGE = "image.png";


    public static void saveImage(Mat mat) {
        File path = new File(Environment.getExternalStorageDirectory() + "/Images/");
        path.mkdirs();
        File file = new File(path, TEST_IMAGE);

        String filename = file.toString();
        Boolean bool = Imgcodecs.imwrite(filename, mat);

        if (bool)
            Log.i(TAG, "SUCCESS writing image to external storage");
        else
            Log.i(TAG, "Fail writing image to external storage");
    }

    public static Mat loadImage() {
        File path = new File(Environment.getExternalStorageDirectory() + "/Images/");
        path.mkdirs();
        File file = new File(path, TEST_IMAGE);

        String filename = file.toString();
        Mat mat = Imgcodecs.imread(filename, 0);
        //    if (Core.countNonZero(mat) != 0) { // non-empty matrix (and not 0x0 matrix) -> turn to white-black channel
    //    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        //    }
        return mat;
    }

    public static void removeImage() {
        File path = new File(Environment.getExternalStorageDirectory() + "/Images/");
        path.mkdirs();
        File file = new File(path, TEST_IMAGE);
        file.delete();
    }

}
