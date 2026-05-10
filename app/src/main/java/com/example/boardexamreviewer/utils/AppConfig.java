package com.example.boardexamreviewer.utils;

import com.example.boardexamreviewer.BuildConfig;

/**
 * [SUB-MODULE: APP CONFIGURATION]
 * This file stores your API keys and settings.
 */
public class AppConfig {
    // The API key is now pulled from local.properties for security
    public static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;
    
    // The model we are using
    public static final String GEMINI_MODEL = "gemini-2.5-flash-lite";
    
    public static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/";
}
