package com.sofaflix.kmp

expect object AppPreferences {
    fun getString(key: String, defaultValue: String = ""): String
    fun putString(key: String, value: String)
}
