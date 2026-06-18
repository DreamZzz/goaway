package com.eat868299

import android.app.Application
import android.util.Log
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.umeng.commonsdk.UMConfigure

class MainApplication : Application(), ReactApplication {

    override val reactHost: ReactHost by lazy {
        getDefaultReactHost(
            context = applicationContext,
            packageList =
                PackageList(this).packages.apply {
                    add(UmengAnalyticsBridgePackage())
                    add(MealVoiceRecorderPackage())
                },
        )
    }

    override fun onCreate() {
        super.onCreate()
        loadReactNative(this)
        initUmeng()
    }

    private fun initUmeng() {
        val appKey = getString(R.string.umeng_app_key)
        val channel = BuildConfig.CHANNEL
        if (appKey.isEmpty()) {
            Log.w("Umeng", "Skipped init: umeng_app_key missing in strings.xml")
            return
        }
        if (BuildConfig.DEBUG) {
            UMConfigure.setLogEnabled(true)
        }
        // preInit + init both in onCreate for Play Store build;
        // preInit-in-attachBaseContext is a Chinese market requirement only.
        UMConfigure.preInit(this, appKey, channel)
        UMConfigure.init(this, appKey, channel, UMConfigure.DEVICE_TYPE_PHONE, "")
        Log.i("Umeng", "Initialized with channel=$channel")
    }
}
