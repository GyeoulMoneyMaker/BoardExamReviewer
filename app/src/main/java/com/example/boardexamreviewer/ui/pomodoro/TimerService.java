package com.example.boardexamreviewer.ui.pomodoro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.example.boardexamreviewer.MainActivity;
import java.util.HashMap;
import java.util.Map;

/**
 * [SUB-MODULE: TIMER ENGINE]
 * This service runs in the background to keep the study timer alive.
 */
public class TimerService extends Service {

    private final IBinder binder = new TimerBinder();
    private CountDownTimer timer;
    private MediaPlayer ambientPlayer;
    private MediaPlayer alarmPlayer;
    
    public long timeLeftInMillis = 0;
    public boolean isTimerRunning = false;
    private int ownerUserId = -1;
    private final Map<Integer, Long> userTimeMap = new HashMap<>();
    
    public interface OnTickListener {
        void onTick(long millisUntilFinished);
    }
    
    public interface OnFinishListener {
        void onFinish();
    }
    
    public OnTickListener onTickListener;
    public OnFinishListener onFinishListener;

    public class TimerBinder extends Binder {
        public TimerService getService() {
            return TimerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private SharedPreferences prefs;
    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener = (sharedPreferences, key) -> {
        if ("current_user_id".equals(key)) {
            int newUserId = sharedPreferences.getInt(key, -1);
            prepareForUser(newUserId);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
        
        int initialUser = prefs.getInt("current_user_id", -1);
        prepareForUser(initialUser);
    }

    public void prepareForUser(int userId) {
        // [LOCKDOWN] If the current timer belongs to someone else, PAUSE IT NOW.
        if (ownerUserId != -1 && ownerUserId != userId && isTimerRunning) {
            userTimeMap.put(ownerUserId, timeLeftInMillis);
            pauseTimer();
        }
        
        ownerUserId = userId;
        Long savedTime = userTimeMap.get(userId);
        timeLeftInMillis = savedTime != null ? savedTime : 0;
    }

    public void startTimer(long durationMillis, int userId) {
        // [STEP: PERSISTENCE] Save current progress for the old user if they switch
        if (ownerUserId != -1 && ownerUserId != userId && isTimerRunning) {
            userTimeMap.put(ownerUserId, timeLeftInMillis);
            pauseTimer();
        }
        
        ownerUserId = userId;
        Long savedTime = userTimeMap.get(userId);
        timeLeftInMillis = savedTime != null ? savedTime : durationMillis;
        
        if (timer != null) {
            timer.cancel();
        }

        timer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                if (onTickListener != null) {
                    onTickListener.onTick(timeLeftInMillis);
                }
                updateNotification();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                if (onFinishListener != null) {
                    onFinishListener.onFinish();
                }
                stopForeground(true);
            }
        }.start();

        isTimerRunning = true;
        startForeground(1, createNotification());
    }

    public void pauseTimer() {
        if (timer != null) {
            timer.cancel();
        }
        isTimerRunning = false;
        if (ownerUserId != -1) {
            userTimeMap.put(ownerUserId, timeLeftInMillis);
        }
        stopAudio();
        stopForeground(true);
    }

    public void resetTimer() {
        if (timer != null) {
            timer.cancel();
        }
        isTimerRunning = false;
        if (ownerUserId != -1) {
            userTimeMap.remove(ownerUserId);
        }
        timeLeftInMillis = 0;
        stopAudio();
        stopForeground(true);
        stopSelf();
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        long minutes = (timeLeftInMillis / 1000) / 60;
        long seconds = (timeLeftInMillis / 1000) % 60;
        String timeText = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);

        return new NotificationCompat.Builder(this, "timer_channel")
            .setContentTitle("Study Session Active")
            .setContentText("Time remaining: " + timeText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .build();
    }

    private void updateNotification() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(1, createNotification());
        }
    }

    public void playAmbient(String type) {
        if (ambientPlayer != null) {
            ambientPlayer.stop();
            ambientPlayer.release();
            ambientPlayer = null;
        }

        if ("None".equals(type)) return;

        try {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ambientPlayer = MediaPlayer.create(this, soundUri);
            if (ambientPlayer != null) {
                ambientPlayer.setLooping(true);
                ambientPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playAlarm(String type) {
        if ("None".equals(type)) return;
        
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            alarmPlayer = MediaPlayer.create(this, alarmUri);
            if (alarmPlayer != null) {
                alarmPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAudio() {
        if (ambientPlayer != null) {
            ambientPlayer.stop();
            ambientPlayer.release();
            ambientPlayer = null;
        }
        
        if (alarmPlayer != null) {
            alarmPlayer.stop();
            alarmPlayer.release();
            alarmPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        stopAudio();
        if (prefs != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
        }
        super.onDestroy();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel("timer_channel", "Study Timer", NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
