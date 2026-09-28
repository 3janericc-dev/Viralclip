package com.example.data.model

data class ClippedShort(
    val id: String,
    val originalVideoId: String,
    val videoTitle: String,
    val authorName: String,
    val viralMoment: ViralMoment,
    val videoUrl: String,
    val localFilePath: String? = null,
    val aspectRatio: String = "9:16",
    val generatedAtMillis: Long = System.currentTimeMillis()
)
