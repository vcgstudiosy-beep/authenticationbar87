package com.authbar87.authenticator;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

public class AuthApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Apply Material You dynamic colors on Android 12+ when available.
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
