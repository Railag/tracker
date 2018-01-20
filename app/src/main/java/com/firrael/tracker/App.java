package com.firrael.tracker;

import android.app.Application;

import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveResourceClient;

import java.lang.ref.WeakReference;

import io.realm.Realm;

public class App extends Application {
    public static final String PREFS = "prefs";

    private static WeakReference<MainActivity> activityRef;
    private static WeakReference<DriveResourceClient> driveRef;
    private static WeakReference<DriveClient> driveClientRef;

    public static void setMainActivity(MainActivity activity) {
        activityRef = new WeakReference<>(activity);
    }

    public static MainActivity getMainActivity() {
        return activityRef != null ? activityRef.get() : null;
    }

    public static void setDriveClient(DriveClient driveClient) {
        App.driveClientRef = new WeakReference<>(driveClient);
    }

    public static DriveClient getDriveClient() {
        return driveClientRef != null ? driveClientRef.get() : null;
    }

    public static void setDrive(DriveResourceClient drive) {
        App.driveRef = new WeakReference<>(drive);
    }

    public static DriveResourceClient getDrive() {
        return driveRef != null ? driveRef.get() : null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
    }


}