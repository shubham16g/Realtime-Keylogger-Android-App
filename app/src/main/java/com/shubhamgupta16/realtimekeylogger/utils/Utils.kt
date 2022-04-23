package com.shubhamgupta16.realtimekeylogger.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings

@SuppressLint("HardwareIds")
fun Context.getDeviceId(): String {
    return Settings.Secure.getString(
        contentResolver, Settings.Secure.ANDROID_ID
    )
}
fun getDeviceName(): String {
    return Build.MODEL
}

fun CharSequence?.encode(): String? {
    if (this == null) return null
    return Uri.encode(this.toString())
}