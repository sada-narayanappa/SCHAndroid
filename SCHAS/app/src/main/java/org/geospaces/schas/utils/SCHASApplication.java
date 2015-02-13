package org.geospaces.schas.utils;

import android.app.Application;
import android.util.Log;

public class SCHASApplication extends Application {

    private static SCHASApplication sInstance;


    public static SCHASApplication getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        sInstance.initializeInstance();
    }

    protected void initializeInstance() {
        SCHASSettings.readSettings();
        Log.i("","Application object initialized!!!!");
    }
}
