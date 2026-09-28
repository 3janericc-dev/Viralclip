package com.example.data.model

import java.util.Locale

data class ViralMoment(
    val title: String,
    val hookScore: Double,
    val startTimeSeconds: Int,
    val endTimeSeconds: Int,
    val reasoning: String
) {
    val durationSeconds: Int
        get() = (endTimeSeconds - startTimeSeconds).coerceAtLeast(1)

    fun formattedTimeRange(): String {
        val startMin = startTimeSeconds / 60
        val startSec = startTimeSeconds % 60
        val endMin = endTimeSeconds / 60
        val endSec = endTimeSeconds % 60
        return String.format(Locale.US, "%02d:%02d - %02d:%02d (%ds)", startMin, startSec, endMin, endSec, durationSeconds)
    }
}
