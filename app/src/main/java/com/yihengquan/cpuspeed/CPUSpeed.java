package com.yihengquan.cpuspeed;

import android.app.Application;

import com.facebook.ads.AudienceNetworkAds;

public class CPUSpeed extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Setup facebook ads
        AudienceNetworkAds.initialize(this);
    }
}
