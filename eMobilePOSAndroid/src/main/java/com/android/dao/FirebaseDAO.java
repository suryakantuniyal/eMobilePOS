package com.android.dao;

import com.android.emobilepos.firebase.NotificationSettings;

import io.realm.Realm;

/**
 * Created by guarionex on 12/6/16.
 */

public class FirebaseDAO {
    public static NotificationSettings getNotificationSettings() {
        Realm r = Realm.getDefaultInstance();
        return r.where(NotificationSettings.class).findFirst();
    }

    public static void saveFirebaseSettings(NotificationSettings settings) {
        Realm r = Realm.getDefaultInstance();
        try {
            r.beginTransaction();
            r.insertOrUpdate(settings);
        } finally {
            r.commitTransaction();
        }
    }

    public static void saveToken(String regID, String fcm_token) {
        Realm r = Realm.getDefaultInstance();
        try {
            r.beginTransaction();
            NotificationSettings settings = getNotificationSettings();
            settings.setRegistrationToken(fcm_token);
            settings.setHubRegistrationId(regID);
            r.insertOrUpdate(settings);
        } finally {
            r.commitTransaction();
        }
    }

    public static void saveHUBRegistrationStatus(NotificationSettings.HUBRegistrationStatus status) {
        Realm r = Realm.getDefaultInstance();
        try {
            r.beginTransaction();
            NotificationSettings settings = getNotificationSettings();
            settings.setRegistrationStatusEnum(status);
            r.insertOrUpdate(settings);
        } finally {
            r.commitTransaction();
        }
    }
}
