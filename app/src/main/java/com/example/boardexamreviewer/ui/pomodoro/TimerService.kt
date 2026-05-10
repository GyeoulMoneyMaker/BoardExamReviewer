package com.example.boardexamreviewer.ui.pomodoro

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.boardexamreviewer.MainActivity
import com.example.boardexamreviewer.R

import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri

/**
 * [SUB-MODULE: TIMER ENGINE]
 * This service runs in the background to keep the study timer alive.
 */
class TimerService : Service() {

    private val binder = TimerBinder()
    private var timer: CountDownTimer? = null
    private var ambientPlayer: MediaPlayer? = null
    private var alarmPlayer: MediaPlayer? = null
    
    var timeLeftInMillis: Long = 0
    var isTimerRunning = false
    private var ownerUserId: Int = -1 
    private val userTimeMap = mutableMapOf<Int, Long>() // [NEW: MULTI-USER STORAGE]
    
    var onTickListener: ((Long) -> Unit)? = null
    var onFinishListener: (() -> Unit)? = null

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private lateinit var prefs: android.content.SharedPreferences
    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "current_user_id") {
            val newUserId = sharedPreferences.getInt(key, -1)
            prepareForUser(newUserId)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        prefs = getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        
        // Initial sync
        val initialUser = prefs.getInt("current_user_id", -1)
        prepareForUser(initialUser)
    }

    fun prepareForUser(userId: Int) {
        // [LOCKDOWN] If the current timer belongs to someone else, PAUSE IT NOW.
        if (ownerUserId != -1 && ownerUserId != userId && isTimerRunning) {
            userTimeMap[ownerUserId] = timeLeftInMillis
            pauseTimer()
        }
        
        ownerUserId = userId
        timeLeftInMillis = userTimeMap[userId] ?: 0
    }

    fun startTimer(durationMillis: Long, userId: Int) {
        // [STEP: PERSISTENCE] Save current progress for the old user if they switch
        if (ownerUserId != -1 && ownerUserId != userId && isTimerRunning) {
            userTimeMap[ownerUserId] = timeLeftInMillis
            pauseTimer()
        }
        
        ownerUserId = userId
        // If we have a saved time for this user, use it. Otherwise use the new duration.
        timeLeftInMillis = userTimeMap[userId] ?: durationMillis
        
        timer?.cancel()

        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                onTickListener?.invoke(timeLeftInMillis)
                updateNotification()
            }

            override fun onFinish() {
                isTimerRunning = false
                onFinishListener?.invoke()
                stopForeground(true)
            }
        }.start()

        isTimerRunning = true
        startForeground(1, createNotification())
    }

    fun pauseTimer() {
        timer?.cancel()
        isTimerRunning = false
        if (ownerUserId != -1) userTimeMap[ownerUserId] = timeLeftInMillis // Save progress
        stopAudio() // [STOP MUSIC]
        stopForeground(true)
    }

    fun resetTimer() {
        timer?.cancel()
        isTimerRunning = false
        if (ownerUserId != -1) userTimeMap.remove(ownerUserId) // Fully clear for this user
        timeLeftInMillis = 0
        stopAudio() // [STOP MUSIC]
        stopForeground(true)
        stopSelf() // Stop the service if fully reset
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        return NotificationCompat.Builder(this, "timer_channel")
            .setContentTitle("Study Session Active")
            .setContentText("Time remaining: $timeText")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, createNotification())
    }

    fun playAmbient(type: String) {
        ambientPlayer?.stop()
        ambientPlayer?.release()
        ambientPlayer = null

        if (type == "None") return

        // NOTE: In a real app, you would have raw/rain.mp3
        // We will simulate playing by using a system sound or just logging
        try {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ambientPlayer = MediaPlayer.create(this, soundUri)
            ambientPlayer?.isLooping = true
            ambientPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playAlarm(type: String) {
        if (type == "None") return
        
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            alarmPlayer = MediaPlayer.create(this, alarmUri)
            alarmPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAudio() {
        ambientPlayer?.stop()
        ambientPlayer?.release()
        ambientPlayer = null
        
        alarmPlayer?.stop()
        alarmPlayer?.release()
        alarmPlayer = null
    }

    override fun onDestroy() {
        stopAudio()
        if (::prefs.isInitialized) {
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("timer_channel", "Study Timer", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
