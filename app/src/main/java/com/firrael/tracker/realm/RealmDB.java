package com.firrael.tracker.realm;

import io.realm.Realm;

/**
 * Created by railag on 29.12.2017.
 */

public class RealmDB {
    public static Realm get() {
        return Realm.getDefaultInstance();
    }
}
