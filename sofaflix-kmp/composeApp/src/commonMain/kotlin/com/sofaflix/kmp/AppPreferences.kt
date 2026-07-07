package com.sofaflix.kmp

expect object AppPreferences {
    fun getString(key: String, defaultValue: String = ""): String
    fun putString(key: String, value: String)
    fun remove(key: String)
    fun getAppVersion(): String
    fun getAppVersionCode(): Int
    fun getPlatform(): String
}
