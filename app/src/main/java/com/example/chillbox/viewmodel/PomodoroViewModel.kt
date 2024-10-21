package com.example.chillbox.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.chillbox.model.PomodoroSessionType

class PomodoroViewModel : ViewModel() {

    // Customization attributes (now using Float for flexibility)
    val workSessionLength = 0.1f // 0.1 minutes = 6 seconds (for testing)
    val shortRestSessionLength = 0.1f // 0.1 minutes = 6 seconds
    val longRestSessionLength = 0.2f // 0.5 minutes = 12 seconds

    private var currentTimeInMillis = (workSessionLength * 60 * 1000L).toLong() // Default work session in milliseconds

    private var timerJob: Job? = null

    // LiveData to track session type and timer value
    val currentSession = MutableLiveData<PomodoroSessionType>().apply { value = PomodoroSessionType.Work }
    val timerValue = MutableLiveData("00:00") // Default work session value
    val isTimerRunning = MutableLiveData(false)
    val workSessionCount = MutableLiveData(0) // Track work session count0

    // Initialize timer display
    init {
        updateTimerDisplay()
    }

    // Start the timer
    fun startTimer() {
        isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (currentTimeInMillis > 0 && isTimerRunning.value == true) {
                delay(1000L) // Delay for 1 second
                currentTimeInMillis -= 1000L
                updateTimerDisplay()
            }
            if (currentTimeInMillis <= 0L) {
                switchSession()
            }
        }
    }

    // Pause the timer
    fun pauseTimer() {
        isTimerRunning.value = false
        timerJob?.cancel()
    }

    // Reset the timer to initial value based on current session
    fun resetTimer() {
        pauseTimer()
        currentSession.value = PomodoroSessionType.Work
        workSessionCount.value = 0
        isTimerRunning.value = false
        currentTimeInMillis = (workSessionLength * 60 * 1000L).toLong()
        updateTimerDisplay()
    }

    // Switch between sessions
    private fun switchSession() {
        // Safely increment the work session count only when in Work session
        workSessionCount.value = if (currentSession.value == PomodoroSessionType.Work) {
            (workSessionCount.value ?: 0) + 1
        } else {
            workSessionCount.value ?: 0
        }

        // Determine the next session based on the current session
        currentSession.value = when (currentSession.value) {
            PomodoroSessionType.Work -> if ((workSessionCount.value ?: 0) % 5 == 0) {
                PomodoroSessionType.LongRest
            } else {
                PomodoroSessionType.Rest
            }
            PomodoroSessionType.Rest, PomodoroSessionType.LongRest -> PomodoroSessionType.Work
            else -> PomodoroSessionType.Work
        }

        // Update the timer value based on the next session
        currentTimeInMillis = when (currentSession.value) {
            PomodoroSessionType.Work -> (workSessionLength * 60 * 1000L).toLong()
            PomodoroSessionType.Rest -> (shortRestSessionLength * 60 * 1000L).toLong()
            PomodoroSessionType.LongRest -> (longRestSessionLength * 60 * 1000L).toLong()
            null -> (workSessionLength * 60 * 1000L).toLong()
        }
        startTimer() // Automatically start next session
    }

    // Update timer display in MM:SS format
    @SuppressLint("DefaultLocale")
    private fun updateTimerDisplay() {
        val minutes = (currentTimeInMillis / 1000) / 60
        val seconds = (currentTimeInMillis / 1000) % 60
        timerValue.value = String.format("%02d:%02d", minutes, seconds)
    }
}
