package com.tracker.firrael.tracker;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by railag on 23.11.2017.
 */

public class Utils {
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CAMERA = 2;
    private static String[] PERMISSIONS_STORAGE = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public static void verifyCameraPermission(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA
            );
        }
    }

    public static float dp2px(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(App.PREFS, Context.MODE_PRIVATE);
    }

    public static void hideKeyboard(@Nullable Activity act) {
        if (act != null && act.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) act.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(act.getCurrentFocus().getWindowToken(), 0);
        }
    }

    private final static double NANO = 1000000000;

    public static double calcTime(long startTime) {
        long currTime = System.nanoTime();
        long diff = currTime - startTime;

        return diff / NANO;
    }

    public static boolean canWrite(Activity activity) {
        return ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
}
