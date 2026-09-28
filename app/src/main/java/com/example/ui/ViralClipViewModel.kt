package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.model.ClippedShort
import com.example.data.model.ProcessingState
import com.example.data.model.VideoMetadata
import com.example.data.model.ViralMoment
import com.example.data.network.CloudRenderService
import com.example.data.network.GeminiService
import com.example.data.network.YouTubeApiService
import com.example.data.storage.PreferencesManager
import com.example.data.storage.ShortsExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ViralClipViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val prefs = PreferencesManager(context)
    private val youtubeApiService = YouTubeApiService()
    private val geminiService = GeminiService()
    private val cloudRenderService = CloudRenderService()
    private val shortsExporter = ShortsExporter(context)

    // Form inputs
    val youtubeUrl = MutableStateFlow("")
    val geminiApiKey = MutableStateFlow("")
    val isApiKeyVisible = MutableStateFlow(false)

    // Pipeline state
    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()

    private val _renderProgress = MutableStateFlow(0)
    val renderProgress: StateFlow<Int> = _renderProgress.asStateFlow()

    private val _renderSubtext = MutableStateFlow("")
    val renderSubtext: StateFlow<String> = _renderSubtext.asStateFlow()

    // Export & Download state
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Explicit error dialog state
    private val _errorDialogState = MutableStateFlow<ProcessingState.Error?>(null)
    val errorDialogState: StateFlow<ProcessingState.Error?> = _errorDialogState.asStateFlow()

    init {
        // Initialize API key from preferences or BuildConfig
        val savedKey = prefs.geminiApiKey
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (savedKey.isNotBlank()) {
            geminiApiKey.value = savedKey
        } else if (!buildKey.isNullOrBlank() && buildKey != "MY_GEMINI_API_KEY") {
            geminiApiKey.value = buildKey
        }
    }

    fun onYoutubeUrlChanged(newUrl: String) {
        youtubeUrl.value = newUrl
    }

    fun onGeminiApiKeyChanged(newKey: String) {
        geminiApiKey.value = newKey
        prefs.geminiApiKey = newKey
    }

    fun toggleApiKeyVisibility() {
        isApiKeyVisible.value = !isApiKeyVisible.value
    }

    fun setSampleUrl(url: String) {
        youtubeUrl.value = url
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun dismissErrorDialog() {
        _errorDialogState.value = null
        if (_processingState.value is ProcessingState.Error) {
            _processingState.value = ProcessingState.Idle
        }
    }

    fun resetToInput() {
        _processingState.value = ProcessingState.Idle
        _errorDialogState.value = null
        _renderProgress.value = 0
        _renderSubtext.value = ""
    }

    /**
     * Executes the entire multi-step background process:
     * 1. YouTube Metadata & Transcript Fetching
     * 2. Gemini AI Viral Moment Extraction
     * 3. 9:16 Cloud Video Crop & Rendering
     */
    fun startAutoClipPipeline() {
        val inputUrl = youtubeUrl.value.trim()
        val key = geminiApiKey.value.trim()

        // Validation: YouTube URL
        if (inputUrl.isBlank()) {
            val err = ProcessingState.Error(
                stage = ProcessingState.Stage.FETCH_METADATA,
                errorMessage = "YouTube URL cannot be empty.",
                details = "Please paste a link to any public YouTube video or choose one of the quick samples below."
            )
            _processingState.value = err
            _errorDialogState.value = err
            return
        }

        val videoId = youtubeApiService.extractVideoId(inputUrl)
        if (videoId == null) {
            val err = ProcessingState.Error(
                stage = ProcessingState.Stage.FETCH_METADATA,
                errorMessage = "Invalid YouTube video link format.",
                details = "Ensure the URL is a standard YouTube format (e.g. https://www.youtube.com/watch?v=... or https://youtu.be/...)."
            )
            _processingState.value = err
            _errorDialogState.value = err
            return
        }

        // Validation: Gemini API Key
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            val err = ProcessingState.Error(
                stage = ProcessingState.Stage.AI_ANALYSIS,
                errorMessage = "Gemini API key is required.",
                details = "Please enter your free Gemini API key from Google AI Studio (aistudio.google.com) to analyze viral video highlights."
            )
            _processingState.value = err
            _errorDialogState.value = err
            return
        }

        // Run multi-step pipeline
        viewModelScope.launch {
            try {
                // ==================== STAGE 1: FETCH METADATA & TRANSCRIPT ====================
                _processingState.value = ProcessingState.FetchingMetadata()

                val metaResult = youtubeApiService.fetchMetadata(videoId)
                if (metaResult.isFailure) {
                    val err = ProcessingState.Error(
                        stage = ProcessingState.Stage.FETCH_METADATA,
                        errorMessage = "Could not fetch YouTube video information.",
                        details = metaResult.exceptionOrNull()?.message ?: "Check your network connection."
                    )
                    _processingState.value = err
                    _errorDialogState.value = err
                    return@launch
                }

                val metadata = metaResult.getOrThrow()
                val transcript = youtubeApiService.fetchTranscript(metadata)

                // ==================== STAGE 2: GEMINI AI VIRAL MOMENT IDENTIFICATION ====================
                _processingState.value = ProcessingState.AnalyzingViralMoments(
                    videoTitle = metadata.title
                )

                val geminiResult = geminiService.analyzeTranscript(
                    apiKey = key,
                    transcript = transcript,
                    onStatusUpdate = { status ->
                        _processingState.value = ProcessingState.AnalyzingViralMoments(
                            videoTitle = metadata.title,
                            statusText = status
                        )
                    }
                )
                if (geminiResult.isFailure) {
                    val err = ProcessingState.Error(
                        stage = ProcessingState.Stage.AI_ANALYSIS,
                        errorMessage = "Gemini AI failed to extract viral highlight moment.",
                        details = geminiResult.exceptionOrNull()?.message ?: "Unknown AI error"
                    )
                    _processingState.value = err
                    _errorDialogState.value = err
                    return@launch
                }

                val viralMoment = geminiResult.getOrThrow()

                // ==================== STAGE 3: CLOUD VIDEO CROPPING & RENDERING ====================
                _processingState.value = ProcessingState.RenderingVideo(
                    viralMoment = viralMoment
                )

                val renderResult = cloudRenderService.renderVerticalShort(
                    videoId = videoId,
                    videoTitle = metadata.title,
                    authorName = metadata.authorName,
                    viralMoment = viralMoment,
                    customCloudEndpoint = prefs.customRenderUrl.ifBlank { null },
                    onProgressUpdate = { pct, sub ->
                        _renderProgress.value = pct
                        _renderSubtext.value = sub
                    }
                )

                if (renderResult.isFailure) {
                    val err = ProcessingState.Error(
                        stage = ProcessingState.Stage.RENDERING,
                        errorMessage = "9:16 Video Rendering failed.",
                        details = renderResult.exceptionOrNull()?.message ?: "Cloud encoder error"
                    )
                    _processingState.value = err
                    _errorDialogState.value = err
                    return@launch
                }

                // SUCCESS: Short ready for preview and export!
                val clippedShort = renderResult.getOrThrow()
                _processingState.value = ProcessingState.Ready(clippedShort)

            } catch (e: Exception) {
                val err = ProcessingState.Error(
                    stage = ProcessingState.Stage.RENDERING,
                    errorMessage = "An unexpected error occurred during processing.",
                    details = e.localizedMessage ?: e.message
                )
                _processingState.value = err
                _errorDialogState.value = err
            }
        }
    }

    /**
     * Downloads and saves the MP4 file directly into the phone's public Movies/ViralShorts gallery folder.
     */
    fun saveToDevice(short: ClippedShort) {
        viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0
            _toastMessage.value = "Downloading 9:16 Short to device..."

            val downloadResult = shortsExporter.downloadToCache(
                videoUrl = short.videoUrl,
                shortId = short.id,
                onProgress = { _downloadProgress.value = it }
            )

            if (downloadResult.isFailure) {
                _isDownloading.value = false
                val err = ProcessingState.Error(
                    stage = ProcessingState.Stage.DOWNLOAD,
                    errorMessage = "Failed to download video file.",
                    details = downloadResult.exceptionOrNull()?.message
                )
                _errorDialogState.value = err
                return@launch
            }

            val cachedFile = downloadResult.getOrThrow()
            val saveResult = shortsExporter.saveToMoviesViralShorts(cachedFile, short.viralMoment.title)

            _isDownloading.value = false

            if (saveResult.isSuccess) {
                _toastMessage.value = "Saved successfully to Movies/ViralShorts in your Gallery!"
            } else {
                val err = ProcessingState.Error(
                    stage = ProcessingState.Stage.DOWNLOAD,
                    errorMessage = "Failed to write into Movies/ViralShorts.",
                    details = saveResult.exceptionOrNull()?.message
                )
                _errorDialogState.value = err
            }
        }
    }

    /**
     * Shares video to YouTube Shorts application.
     */
    fun postOnYouTubeShorts(short: ClippedShort) {
        shareVideoToPackage(short, ShortsExporter.PACKAGE_YOUTUBE, "YouTube")
    }

    /**
     * Shares video to TikTok application.
     */
    fun postOnTikTok(short: ClippedShort) {
        shareVideoToPackage(short, ShortsExporter.PACKAGE_TIKTOK, "TikTok")
    }

    private fun shareVideoToPackage(short: ClippedShort, targetPackage: String, appName: String) {
        viewModelScope.launch {
            _isDownloading.value = true
            _toastMessage.value = "Preparing video for $appName..."

            val downloadResult = shortsExporter.downloadToCache(
                videoUrl = short.videoUrl,
                shortId = short.id
            )

            _isDownloading.value = false

            if (downloadResult.isFailure) {
                val err = ProcessingState.Error(
                    stage = ProcessingState.Stage.SHARE,
                    errorMessage = "Could not prepare video file for sharing.",
                    details = downloadResult.exceptionOrNull()?.message
                )
                _errorDialogState.value = err
                return@launch
            }

            val file = downloadResult.getOrThrow()
            val shareResult = shortsExporter.shareShort(file, short.viralMoment.title, targetPackage)

            if (shareResult.isFailure) {
                val err = ProcessingState.Error(
                    stage = ProcessingState.Stage.SHARE,
                    errorMessage = "Could not open $appName.",
                    details = shareResult.exceptionOrNull()?.message
                )
                _errorDialogState.value = err
            }
        }
    }
}
