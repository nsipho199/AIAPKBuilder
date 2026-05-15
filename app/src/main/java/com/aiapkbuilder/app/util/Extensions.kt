package com.aiapkbuilder.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.*

/**
 * Extension functions for File operations
 */

fun File.getMimeType(): String = when {
    name.endsWith(".apk") -> "application/vnd.android.package-archive"
    name.endsWith(".aab") -> "application/x-authorware-bin"
    name.endsWith(".zip") -> "application/zip"
    name.endsWith(".txt") -> "text/plain"
    name.endsWith(".json") -> "application/json"
    name.endsWith(".xml") -> "text/xml"
    else -> "application/octet-stream"
}

fun File.getReadableSize(): String {
    val size = this.length()
    return when {
        size <= 0 -> "0 B"
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
        size < 1024 * 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
    }
}

fun File.getUri(context: Context): Uri = FileProvider.getUriForFile(
    context,
    "${context.packageName}.fileprovider",
    this
)

/**
 * Extension functions for String operations
 */

fun String.isValidEmail(): Boolean {
    val emailPattern = "^[A-Za-z0-9+_.-]+@(.+)$".toRegex()
    return this.matches(emailPattern)
}

fun String.isValidUrl(): Boolean {
    return try {
        Uri.parse(this)
        true
    } catch (e: Exception) {
        false
    }
}

fun String.toTitleCase(): String = this.split(" ").joinToString(" ") { word ->
    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

fun String.truncate(length: Int, suffix: String = "..."): String =
    if (this.length > length) {
        this.substring(0, length - suffix.length) + suffix
    } else {
        this
    }

/**
 * Extension functions for Time operations
 */

fun Long.toReadableTime(): String {
    val seconds = this / 1000
    return when {
        seconds < 60 -> "$seconds sec"
        seconds < 3600 -> "${seconds / 60} min ${seconds % 60} sec"
        else -> "${seconds / 3600} h ${(seconds % 3600) / 60} min"
    }
}

fun Long.toFormattedDate(): String {
    val date = Date(this)
    return android.text.format.DateFormat.format("MMM dd, yyyy HH:mm", date).toString()
}

fun Long.toFormattedTime(): String {
    val date = Date(this)
    return android.text.format.DateFormat.format("HH:mm:ss", date).toString()
}

fun Long.isToday(): Boolean {
    val calendar = Calendar.getInstance()
    val today = calendar.apply {
        timeInMillis = System.currentTimeMillis()
    }
    val other = calendar.apply {
        timeInMillis = this@isToday
    }
    return today.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

/**
 * Extension functions for Collection operations
 */

fun <T> List<T>.chunked(size: Int): List<List<T>> {
    val chunks = mutableListOf<List<T>>()
    var index = 0
    while (index < this.size) {
        chunks.add(this.subList(index, minOf(index + size, this.size)))
        index += size
    }
    return chunks
}

fun <T> List<T>.takeIfNotEmpty(): List<T>? = if (this.isNotEmpty()) this else null

fun <K, V> Map<K, V>.filterNotNull(): Map<K, V> = this.filter { (_, v) -> v != null }

/**
 * Extension functions for Context
 */

fun Context.openUrlInBrowser(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    } catch (e: Exception) {
        AppLogger.e("Failed to open URL: $url", e)
    }
}

fun Context.shareText(text: String, title: String = "Share") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(intent, title))
}
