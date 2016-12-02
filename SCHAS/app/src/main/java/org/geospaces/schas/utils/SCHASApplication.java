package org.geospaces.schas.utils;

import android.app.Application;
import android.content.Context;
import android.provider.Settings;
import android.support.multidex.MultiDex;
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
        Context context = getApplicationContext();
        String ID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        SCHASSettings.deviceID = ID;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
