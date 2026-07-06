package com.lunahub.android.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.formatBytes(): String {
    val gb = 1024L * 1024L * 1024L
    val mb = 1024L * 1024L
    return when {
        this >= gb -> "%.1f GB".format(this.toDouble() / gb)
        this >= mb -> "%.1f MB".format(this.toDouble() / mb)
        this >= 1024L -> "%.1f KB".format(this.toDouble() / 1024L)
        else -> "$this B"
    }
}

fun Long.formatDate(): String = SimpleDateFormat("M月d日 E", Locale.CHINA).format(Date(this))

fun Long.formatDateTime(): String = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(Date(this))

fun Long.formatDuration(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    return "%02d:%02d".format(minutes, seconds % 60)
}
