package com.swasthai.app.feature.citizen.exercise

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.data.local.database.entity.ExerciseSessionEntity
import com.swasthai.app.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

private val Context.exerciseDataStore by preferencesDataStore(name = "exercise_prefs")
private val KEY_DAILY_GOAL = intPreferencesKey("daily_goal_minutes")
const val DEFAULT_EXERCISE_GOAL = 20

data class DailyExerciseSummary(
    val date: LocalDate,
    val totalMinutes: Int,
    val sessions: List<ExerciseSessionEntity>
)

data class WeeklyExerciseState(
    val isLoading: Boolean = true,
    val activeDays: Int = 0,
    val totalMinutes: Int = 0,
    val maxMinutesInDay: Int = 0,
    val dailySummaries: List<DailyExerciseSummary> = emptyList() // Mon-Sun
)

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _weeklyState = MutableStateFlow(WeeklyExerciseState())
    val weeklyState: StateFlow<WeeklyExerciseState> = _weeklyState.asStateFlow()

    /** Today's total exercise minutes derived from real session data. */
    val todayMinutes: StateFlow<Int> = _weeklyState.map { state ->
        val today = LocalDate.now()
        state.dailySummaries.find { it.date == today }?.totalMinutes ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Daily goal in minutes, persisted to DataStore. */
    val dailyGoalMinutes: StateFlow<Int> = context.exerciseDataStore.data
        .map { prefs -> prefs[KEY_DAILY_GOAL] ?: DEFAULT_EXERCISE_GOAL }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_EXERCISE_GOAL)

    // Using a mock user ID for demo purposes
    private val currentUserId = "mock-user-123"

    init {
        loadWeeklyData()
    }

    /** Persist a new daily exercise goal to DataStore. */
    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch {
            context.exerciseDataStore.edit { prefs ->
                prefs[KEY_DAILY_GOAL] = minutes
            }
        }
    }

    private fun loadWeeklyData() {
        val now = LocalDate.now()
        val firstDayOfWeek = now.with(WeekFields.of(Locale.FRANCE).dayOfWeek(), 1) // Monday
        val lastDayOfWeek = now.with(WeekFields.of(Locale.FRANCE).dayOfWeek(), 7) // Sunday

        val startTimestamp = firstDayOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTimestamp = lastDayOfWeek.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        viewModelScope.launch {
            exerciseRepository.getSessionsBetweenDates(currentUserId, startTimestamp, endTimestamp)
                .collectLatest { sessions ->
                    val summaries = mutableListOf<DailyExerciseSummary>()
                    var totalMin = 0
                    var activeDays = 0
                    var maxMin = 0

                    for (i in 0..6) {
                        val date = firstDayOfWeek.plusDays(i.toLong())
                        val daySessions = sessions.filter { 
                            val sessionDate = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(it.date), ZoneId.systemDefault())
                            sessionDate == date
                        }
                        val dayMin = daySessions.sumOf { it.durationMinutes }
                        if (dayMin > 0) activeDays++
                        totalMin += dayMin
                        if (dayMin > maxMin) maxMin = dayMin

                        summaries.add(DailyExerciseSummary(date, dayMin, daySessions))
                    }

                    _weeklyState.value = WeeklyExerciseState(
                        isLoading = false,
                        activeDays = activeDays,
                        totalMinutes = totalMin,
                        maxMinutesInDay = maxMin,
                        dailySummaries = summaries
                    )
                }
        }
    }

    fun recordExerciseSession(exercise: SimpleExercise) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val session = ExerciseSessionEntity(
                userId = currentUserId,
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                date = startOfDay,
                startTime = now - (exercise.durationMinutes * 60 * 1000),
                endTime = now,
                durationMinutes = exercise.durationMinutes,
                completed = true
            )
            exerciseRepository.recordSession(session)
        }
    }
}
