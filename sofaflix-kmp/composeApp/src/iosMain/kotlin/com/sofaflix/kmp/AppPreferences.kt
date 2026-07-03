package com.sofaflix.kmp

import platform.Foundation.NSUserDefaults

actual object AppPreferences {
    actual fun getString(key: String, defaultValue: String): String {
        return NSUserDefaults.standardUserDefaults.stringForKey(key) ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
        NSUserDefaults.standardUserDefaults.synchronize()
    }
}
