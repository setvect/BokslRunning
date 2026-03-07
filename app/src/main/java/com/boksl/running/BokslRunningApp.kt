package com.boksl.running

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class BokslRunningApp : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base?.withKoreanLocale())
    }

    override fun onCreate() {
        super.onCreate()
        Locale.setDefault(Locale.KOREA)
    }
}

internal fun Context.withKoreanLocale(): Context {
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(Locale.KOREA)
    configuration.setLayoutDirection(Locale.KOREA)
    return createConfigurationContext(configuration)
}
