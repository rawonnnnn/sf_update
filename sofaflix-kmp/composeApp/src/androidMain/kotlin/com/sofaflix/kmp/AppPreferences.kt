package com.sofaflix.kmp

import android.content.Context
import android.content.SharedPreferences

actual object AppPreferences {
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        sharedPrefs = context.getSharedPreferences("sofaflix_prefs", Context.MODE_PRIVATE)
    }

    actual fun getString(key: String, defaultValue: String): String {
        if (!::sharedPrefs.isInitialized) return defaultValue
        return sharedPrefs.getString(key, defaultValue) ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        if (!::sharedPrefs.isInitialized) return
        sharedPrefs.edit().putString(key, value).apply()
    }

    actual fun remove(key: String) {
        if (!::sharedPrefs.isInitialized) return
        sharedPrefs.edit().remove(key).apply()
    }

    actual fun getAppVersion(): String {
        if (!::appContext.isInitialized) return "1.0.0"
        return try {
            val pInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    actual fun getAppVersionCode(): Int {
        if (!::appContext.isInitialized) return 1
        return try {
            val pInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                (pInfo.longVersionCode and 0xFFFFFFFFL).toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    actual fun getPlatform(): String = "android"
}
