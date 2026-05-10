package com.example.boardexamreviewer.ui.pomodoro

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.boardexamreviewer.data.AppDatabase
import com.example.boardexamreviewer.data.StudySessionEntity
import com.example.boardexamreviewer.databinding.FragmentPomodoroBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [SUB-MODULE: POMODORO CLOCK]
 * This fragment manages the study timer, white noise, and session tracking.
 * Label: POMODORO_MODULE
 */
/**
 * [SUB-MODULE: POMODORO CLOCK]
 * This fragment manages the study timer, white noise, and session tracking.
 * We kept this code basic so it is easy for students to understand.
 */
class PomodoroFragment : Fragment() {

    private var _binding: FragmentPomodoroBinding? = null
    private val binding get() = _binding!!

    private var timerService: TimerService? = null
    private var isBound = false
    private var isWorking = true
    private var extensionUsed = false

    private val connection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            
            // [STEP: SYNC] Sync the UI with the user's saved time
            val currentUserId = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getInt("current_user_id", -1)
            timerService?.prepareForUser(currentUserId)
            
            isBound = true
            setupServiceListeners()
            updateUIFromService()
        }

        override fun onServiceDisconnected(arg0: android.content.ComponentName) {
            isBound = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPomodoroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // [STEP: PERSISTENT ENGINE]
        // Starting the service explicitly ensures it keeps running even if we leave this screen.
        val serviceIntent = Intent(requireContext(), TimerService::class.java)
        requireContext().startService(serviceIntent)
        requireContext().bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        setupSpinners()

        // Live update display as you type
        binding.etWorkTime.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                if (timerService?.isTimerRunning != true) {
                    val mins = s.toString().toLongOrNull() ?: 25
                    updateCountDownText(mins * 60 * 1000)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnStartPause.setOnClickListener {
            if (timerService?.isTimerRunning == true) pauseTimer() else startTimer()
        }

        binding.btnReset.setOnClickListener { resetTimer() }

        binding.btnExtend.setOnClickListener {
            if (!extensionUsed && timerService?.isTimerRunning == true) {
                extendTimer()
            }
        }
    }

    private fun setupServiceListeners() {
        timerService?.onTickListener = { updateCountDownText(it) }
        timerService?.onFinishListener = {
            playAlarm()
            saveSession()
            isWorking = !isWorking
            resetTimer()
        }
    }

    private fun updateUIFromService() {
        timerService?.let {
            if (it.isTimerRunning || it.timeLeftInMillis > 0) {
                updateCountDownText(it.timeLeftInMillis)
            } else {
                // If not running, show the time set in the input box
                val workMins = binding.etWorkTime.text.toString().toLongOrNull() ?: 25
                updateCountDownText(workMins * 60 * 1000)
            }
            binding.btnStartPause.text = if (it.isTimerRunning) "PAUSE" else if (it.timeLeftInMillis > 0) "RESUME" else "START FOCUS SESSION"
        }
    }

    private fun setupSpinners() {
        val noises = arrayOf("None", "Rain", "Waves", "Forest")
        binding.spinnerWhiteNoise.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, noises))

        val alarms = arrayOf("Default Bell", "Digital", "Zen", "None")
        binding.spinnerAlarm.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, alarms))
    }

    private fun startTimer() {
        val currentUserId = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getInt("current_user_id", -1)
        val duration = if (timerService?.timeLeftInMillis ?: 0 > 0) {
            timerService?.timeLeftInMillis ?: 0
        } else {
            val mins = if (isWorking) {
                binding.etWorkTime.text.toString().toLongOrNull() ?: 25
            } else {
                binding.etBreakTime.text.toString().toLongOrNull() ?: 5
            }
            mins * 60 * 1000
        }
        timerService?.startTimer(duration, currentUserId)
        
        // Start Ambient Sound
        val selectedAmbient = binding.spinnerWhiteNoise.text.toString()
        timerService?.playAmbient(selectedAmbient)
        
        binding.btnStartPause.text = "Pause"
    }

    private fun pauseTimer() {
        timerService?.pauseTimer()
        binding.btnStartPause.text = "Resume"
    }

    private fun resetTimer() {
        timerService?.resetTimer()
        isWorking = true
        extensionUsed = false
        val workMins = binding.etWorkTime.text.toString().toLongOrNull() ?: 25
        updateCountDownText(workMins * 60 * 1000)
        binding.btnStartPause.text = "Start Focus Session"
    }

    private fun onTimerFinished() {
        binding.btnStartPause.text = "START FOCUS SESSION"
        Toast.makeText(context, "Session Finished!", Toast.LENGTH_LONG).show()
        
        // Play Alarm
        val selectedAlarm = binding.spinnerAlarm.text.toString()
        timerService?.playAlarm(selectedAlarm)
    }

    private fun extendTimer() {
        val currentUserId = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getInt("current_user_id", -1)
        timerService?.let {
            val newTime = it.timeLeftInMillis + 5 * 60 * 1000
            it.startTimer(newTime, currentUserId)
            extensionUsed = true
            binding.btnExtend.isEnabled = false
            Toast.makeText(context, "Extended by 5 minutes", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCountDownText(millis: Long) {
        if (_binding == null) return
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        binding.tvTimerDisplay.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun playAlarm() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            RingtoneManager.getRingtone(requireContext(), notification).play()
        } catch (e: Exception) {}
    }

    private fun saveSession() {
        if (!isWorking) return
        val duration = binding.etWorkTime.text.toString().toIntOrNull() ?: 25
        val appContext = requireContext().applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(appContext)
            db.appDao().insertStudySession(StudySessionEntity(userId = 1, durationMinutes = duration, sessionType = "Work"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isBound) {
            requireContext().unbindService(connection)
            isBound = false
        }
        _binding = null
    }
}
