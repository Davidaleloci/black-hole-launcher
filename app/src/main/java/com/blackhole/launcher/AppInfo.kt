package com.blackhole.launcher

import android.graphics.Bitmap

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Bitmap? = null
)
