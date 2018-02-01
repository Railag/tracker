package com.firrael.tracker;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.inputmethod.InputMethodManager;

import com.firrael.tracker.realm.TaskModel;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import io.realm.RealmObject;

/**
 * Created by railag on 23.11.2017.
 */

public class Utils {
    public final static String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss a";
    public final static String DATE_UI_FORMAT = "MMM, dd 'at' hh:mm a";


    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CAMERA = 2;
    private static final int REQUEST_SPEECH = 3;
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

    public static void verifySpeechPermission(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_SPEECH
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

    public static boolean checkDiskPermission(Activity activity) {
        return ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkCameraPermission(Activity activity) {
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkVoicePermission(Activity activity) {
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    public static String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }

    public static Gson buildGson() {
        return new GsonBuilder()
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getDeclaringClass().equals(RealmObject.class);
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .registerTypeAdapter(TaskModel.class, new TaskModelSerializer())
                .create();
    }

    public static String formatUIDate(String dateString) {
        if (TextUtils.isEmpty(dateString)) {
            return dateString;
        }

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat(Utils.DATE_FORMAT, Locale.getDefault());
        try {
            Date date = format.parse(dateString);
            Calendar dateCalendar = Calendar.getInstance();
            dateCalendar.setTime(date);
            int daysPassed = calendar.get(Calendar.DAY_OF_MONTH) - dateCalendar.get(Calendar.DAY_OF_MONTH);
            String day;
            switch (daysPassed) {
                case 0:
                    day = "Today";
                    break;
                case 1:
                    day = "Yesterday";
                    break;
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    day = daysPassed + " Days Ago";
                    break;
                case 7:
                    day = "A Week Ago";
                    break;
                default:
                    return new SimpleDateFormat(Utils.DATE_UI_FORMAT, Locale.getDefault()).format(date);
            }
            return day + " at " + dateCalendar.get(Calendar.HOUR) + ":" + getMinutes(dateCalendar) + getAmOrPm(dateCalendar);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString;
        }
    }

    private static String getAmOrPm(Calendar dateCalendar) {
        int amPm = dateCalendar.get(Calendar.AM_PM);
        if (amPm == Calendar.AM) {
            return " AM";
        } else if (amPm == Calendar.PM) {
            return " PM";
        }

        return "";
    }

    private static String getMinutes(Calendar dateCalendar) {
        int minutes = dateCalendar.get(Calendar.MINUTE);
        if (minutes < 10) {
            return "0" + minutes;
        } else {
            return String.valueOf(minutes);
        }
    }
}
