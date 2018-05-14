package com.firrael.tracker;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

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
        return mat;
    }

    public static void removeImage() {
        File path = new File(Environment.getExternalStorageDirectory() + "/Images/");
        path.mkdirs();
        File file = new File(path, TEST_IMAGE);
        file.delete();
    }

    public static String readTextFile(Context context, String filename) {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open(filename)));

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return builder.toString();
    }

}
