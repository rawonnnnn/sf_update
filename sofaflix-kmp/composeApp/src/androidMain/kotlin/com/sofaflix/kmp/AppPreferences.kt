package com.sofaflix.kmp

import android.content.Context
import android.content.SharedPreferences

actual object AppPreferences {
    private lateinit var sharedPrefs: SharedPreferences

    fun init(context: Context) {
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
}
