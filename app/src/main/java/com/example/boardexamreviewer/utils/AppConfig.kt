package com.example.boardexamreviewer.utils

/**
 * [SUB-MODULE: APP CONFIGURATION]
 * This file stores your API keys and settings.
 */
object AppConfig {
    // The API key is now pulled from local.properties for security
    const val GEMINI_API_KEY = com.example.boardexamreviewer.BuildConfig.GEMINI_API_KEY
    
    // The model we are using (Gemini 2.5 Flash-Lite is the official stable version as of 2026)
    const val GEMINI_MODEL = "gemini-2.5-flash-lite"
    
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
}
