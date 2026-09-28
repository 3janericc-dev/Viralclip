package com.example.data.model

data class VideoMetadata(
    val videoId: String,
    val title: String,
    val authorName: String,
    val thumbnailUrl: String,
    val originalUrl: String,
    val transcript: String = ""
)
