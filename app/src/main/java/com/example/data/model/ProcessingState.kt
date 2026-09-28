package com.example.data.model

sealed interface ProcessingState {
    object Idle : ProcessingState

    data class FetchingMetadata(
        val message: String = "Fetching YouTube Metadata & Transcript..."
    ) : ProcessingState

    data class AnalyzingViralMoments(
        val message: String = "Gemini AI identifying viral short moments...",
        val videoTitle: String,
        val statusText: String? = null
    ) : ProcessingState

    data class RenderingVideo(
        val message: String = "Processing & rendering 9:16 vertical short...",
        val viralMoment: ViralMoment
    ) : ProcessingState

    data class Ready(
        val clippedShort: ClippedShort
    ) : ProcessingState

    data class Error(
        val stage: Stage,
        val errorMessage: String,
        val details: String? = null
    ) : ProcessingState

    enum class Stage {
        FETCH_METADATA,
        AI_ANALYSIS,
        RENDERING,
        DOWNLOAD,
        SHARE
    }
}
