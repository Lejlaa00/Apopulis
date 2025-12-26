package com.example.apopulis.util

import java.text.SimpleDateFormat
import java.util.*

object DateFormatter {
    fun formatRelativeTime(dateString: String?): String {
        if (dateString == null) return "Recently"

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateString)

            if (date != null) {
                val now = Date()
                val diff = now.time - date.time
                val minutes = diff / (1000 * 60)
                val hours = diff / (1000 * 60 * 60)
                val days = diff / (1000 * 60 * 60 * 24)

                when {
                    minutes < 1 -> "Just now"
                    minutes < 60 -> "$minutes m ago"
                    hours < 24 -> "$hours h ago"
                    days < 7 -> "$days d ago"
                    else -> {
                        val outputFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                        outputFormat.format(date)
                    }
                }
            } else {
                "Recently"
            }
        } catch (e: Exception) {
            "Recently"
        }
    }
}

