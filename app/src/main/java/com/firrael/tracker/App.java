package com.firrael.tracker;

import android.app.Application;

import java.lang.ref.WeakReference;

import io.realm.Realm;

public class App extends Application {
    public static final String PREFS = "prefs";

    private static WeakReference<MainActivity> activityRef;

    public static void setMainActivity(MainActivity activity) {
        activityRef = new WeakReference<>(activity);
    }

    public static MainActivity getMainActivity() {
        return activityRef != null ? activityRef.get() : null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
    }


}