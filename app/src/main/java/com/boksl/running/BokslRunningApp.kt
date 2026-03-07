package com.boksl.running

import android.app.Application
import android.content.res.Configuration
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class BokslRunningApp : Application() {
    override fun onCreate() {
        super.onCreate()
        applyKoreanLocale()
    }

    private fun applyKoreanLocale() {
        val locale = Locale.KOREA
        Locale.setDefault(locale)

        val resources = applicationContext.resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}
