package com.lunahub.android.data.remote

import com.lunahub.android.domain.model.MediaType
import java.net.URL
import java.util.Calendar
import java.util.Locale

class LunaIndexParser {
    private val indexRegex =
        Regex("""<a href="(?<href>[^"]+)">(?<name>[^<]+)</a>\s+(?<date>\d{2}-[A-Za-z]{3}-\d{4})\s+(?<time>\d{2}:\d{2})\s+(?<size>\S+)""")

    fun extractCameraSubdirs(html: String): List<String> {
        return indexRegex.findAll(html)
            .mapNotNull { match ->
                val href = htmlDecode(match.groups["href"]?.value.orEmpty())
                if (href != "../" && href.endsWith("/") && Regex("""Camera\d+/""", RegexOption.IGNORE_CASE).matches(href)) {
                    href.trimEnd('/')
                } else {
                    null
                }
            }
            .sorted()
            .toList()
    }

    fun parse(html: String, baseUrl: String): List<LunaRemoteMedia> {
        val rawFiles = indexRegex.findAll(html).mapNotNull { match ->
            val href = htmlDecode(match.groups["href"]?.value.orEmpty())
            val name = htmlDecode(match.groups["name"]?.value.orEmpty())
            if (href == "../" || name == "../" || href.endsWith("/")) return@mapNotNull null

            val mediaType = mediaType(name) ?: return@mapNotNull null
            val absoluteUrl = URL(URL(baseUrl), href).toString()
            val createdAt = capturedAtFromName(name) ?: parseIndexTimestamp(
                match.groups["date"]?.value.orEmpty(),
                match.groups["time"]?.value.orEmpty(),
            )
            LunaRemoteMedia(
                id = absoluteUrl,
                fileName = name,
                filePath = href,
                mediaUrl = absoluteUrl,
                thumbnailUrl = if (mediaType == MediaType.Photo) absoluteUrl else null,
                mediaType = mediaType,
                fileSize = parseSize(match.groups["size"]?.value.orEmpty()),
                createdAt = createdAt,
                previewName = null,
                previewUrl = null,
            )
        }.toList()

        return attachVideoPreviews(rawFiles).sortedByDescending { it.createdAt }
    }

    private fun attachVideoPreviews(files: List<LunaRemoteMedia>): List<LunaRemoteMedia> {
        val lrvByKey = files
            .filter { it.fileName.endsWith(".lrv", ignoreCase = true) }
            .associateBy { videoKey(it.fileName) }
        return files
            .filterNot { it.fileName.endsWith(".lrv", ignoreCase = true) }
            .map { file ->
                val preview = if (file.mediaType == MediaType.Video) lrvByKey[videoKey(file.fileName)] else null
                if (preview == null) file else file.copy(
                    thumbnailUrl = preview.mediaUrl,
                    previewName = preview.fileName,
                    previewUrl = preview.mediaUrl,
                )
            }
    }

    private fun htmlDecode(value: String): String {
        return value
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
    }

    private fun extensionOf(name: String): String = name.substringAfterLast('.', "").lowercase(Locale.US)

    private fun mediaType(name: String): MediaType? {
        return when (extensionOf(name)) {
            "jpg", "jpeg", "png", "dng", "insp", "webp" -> MediaType.Photo
            "mp4", "mov", "lrv" -> MediaType.Video
            else -> null
        }
    }

    private fun videoKey(name: String): String {
        return name
            .replace(Regex("""^(VID|LRV|LIV)_""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\.(mp4|mov|lrv)$""", RegexOption.IGNORE_CASE), "")
    }

    private fun parseSize(text: String): Long {
        val match = Regex("""^(\d+(?:\.\d+)?)([KMG])?$""", RegexOption.IGNORE_CASE).find(text.trim()) ?: return 0L
        val value = match.groupValues[1].toDoubleOrNull() ?: return 0L
        val multiplier = when (match.groupValues.getOrNull(2)?.uppercase(Locale.US)) {
            "G" -> 1024L * 1024L * 1024L
            "M" -> 1024L * 1024L
            "K" -> 1024L
            else -> 1L
        }
        return (value * multiplier).toLong()
    }

    private fun capturedAtFromName(name: String): Long? {
        val match = Regex("""(?:VID|LRV|IMG|LIV|PIC|PANO)_(\d{4})(\d{2})(\d{2})_(\d{2})(\d{2})(\d{2})""", RegexOption.IGNORE_CASE).find(name)
            ?: return null
        return calendar(
            year = match.groupValues[1].toInt(),
            month = match.groupValues[2].toInt() - 1,
            day = match.groupValues[3].toInt(),
            hour = match.groupValues[4].toInt(),
            minute = match.groupValues[5].toInt(),
        )
    }

    private fun parseIndexTimestamp(dateText: String, timeText: String): Long {
        val dateMatch = Regex("""^(\d{2})-([A-Za-z]{3})-(\d{4})$""").find(dateText)
        val timeMatch = Regex("""^(\d{2}):(\d{2})$""").find(timeText)
        if (dateMatch == null || timeMatch == null) return System.currentTimeMillis()
        val month = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            .indexOfFirst { it.equals(dateMatch.groupValues[2], ignoreCase = true) }
        if (month < 0) return System.currentTimeMillis()
        return calendar(
            year = dateMatch.groupValues[3].toInt(),
            month = month,
            day = dateMatch.groupValues[1].toInt(),
            hour = timeMatch.groupValues[1].toInt(),
            minute = timeMatch.groupValues[2].toInt(),
        )
    }

    private fun calendar(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
