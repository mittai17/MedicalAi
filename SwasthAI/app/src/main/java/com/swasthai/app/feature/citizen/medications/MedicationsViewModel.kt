package com.swasthai.app.feature.citizen.medications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.domain.model.Medication
import com.swasthai.app.domain.model.MedicationDose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class MedicationsUiState(
    val todayProgress: Pair<Int, Int> = 0 to 0,
    val missedDoses: List<Pair<Medication, MedicationDose>> = emptyList(),
    val upcomingReminders: List<Pair<Medication, MedicationDose>> = emptyList(),
    val currentMedicines: List<Medication> = emptyList(),
    val isAddMedicineFlowActive: Boolean = false,
    val selectedMedicine: Medication? = null
)

class MedicationsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MedicationsUiState())
    val uiState: StateFlow<MedicationsUiState> = _uiState.asStateFlow()

    private val _medicines = MutableStateFlow<List<Medication>>(emptyList())
    private val _doses = MutableStateFlow<List<MedicationDose>>(emptyList())

    init {
        // Load initial mock data to demonstrate functionality reliably
        loadInitialData()
    }

    private fun loadInitialData() {
        val med1 = Medication(
            id = UUID.randomUUID().toString(),
            name = "Aspirin",
            dosage = "81 mg",
            frequency = "Daily"
        )
        val med2 = Medication(
            id = UUID.randomUUID().toString(),
            name = "Metformin",
            dosage = "500 mg",
            frequency = "Twice daily"
        )
        val med3 = Medication(
            id = UUID.randomUUID().toString(),
            name = "Lisinopril",
            dosage = "10 mg",
            frequency = "Daily"
        )
        
        _medicines.value = listOf(med1, med2, med3)

        // Mock doses
        val now = Calendar.getInstance()
        
        // Missed dose: 8 AM today
        val missedTime = now.clone() as Calendar
        missedTime.set(Calendar.HOUR_OF_DAY, 8)
        missedTime.set(Calendar.MINUTE, 0)
        val missedDose = MedicationDose(
            id = UUID.randomUUID().toString(),
            medicationId = med2.id,
            scheduledTimeMillis = missedTime.timeInMillis,
            isTaken = false
        )

        // Taken dose
        val takenTime = now.clone() as Calendar
        takenTime.set(Calendar.HOUR_OF_DAY, 9)
        takenTime.set(Calendar.MINUTE, 0)
        val takenDose = MedicationDose(
            id = UUID.randomUUID().toString(),
            medicationId = med1.id,
            scheduledTimeMillis = takenTime.timeInMillis,
            isTaken = true,
            takenTimeMillis = takenTime.timeInMillis + 5000 // taken slightly after
        )

        // Upcoming dose tonight 8 PM
        val upcomingTime1 = now.clone() as Calendar
        upcomingTime1.set(Calendar.HOUR_OF_DAY, 20)
        upcomingTime1.set(Calendar.MINUTE, 0)
        val upcomingDose1 = MedicationDose(
            id = UUID.randomUUID().toString(),
            medicationId = med1.id,
            scheduledTimeMillis = upcomingTime1.timeInMillis,
            isTaken = false
        )

        // Upcoming dose tomorrow 8 AM
        val upcomingTime2 = now.clone() as Calendar
        upcomingTime2.add(Calendar.DAY_OF_YEAR, 1)
        upcomingTime2.set(Calendar.HOUR_OF_DAY, 8)
        upcomingTime2.set(Calendar.MINUTE, 0)
        val upcomingDose2 = MedicationDose(
            id = UUID.randomUUID().toString(),
            medicationId = med3.id,
            scheduledTimeMillis = upcomingTime2.timeInMillis,
            isTaken = false
        )

        _doses.value = listOf(missedDose, takenDose, upcomingDose1, upcomingDose2)

        updateUiState()
    }

    private fun updateUiState() {
        val meds = _medicines.value
        val allDoses = _doses.value

        val nowMillis = System.currentTimeMillis()
        
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        // Map doses to pairs
        val dosePairs = allDoses.mapNotNull { dose ->
            val med = meds.find { it.id == dose.medicationId }
            if (med != null) Pair(med, dose) else null
        }

        // Today's doses
        val todayDoses = dosePairs.filter {
            it.second.scheduledTimeMillis in todayStart..todayEnd
        }
        val takenToday = todayDoses.count { it.second.isTaken }
        val totalToday = todayDoses.size
        
        // Missed doses (scheduled in the past, not taken)
        val missed = dosePairs.filter {
            !it.second.isTaken && it.second.scheduledTimeMillis < nowMillis
        }

        // Upcoming doses (scheduled in the future, not taken, and only for today)
        val upcoming = dosePairs.filter {
            !it.second.isTaken && 
            it.second.scheduledTimeMillis >= nowMillis &&
            it.second.scheduledTimeMillis <= todayEnd
        }.sortedBy { it.second.scheduledTimeMillis }

        _uiState.update { currentState ->
            currentState.copy(
                todayProgress = takenToday to totalToday,
                missedDoses = missed,
                upcomingReminders = upcoming,
                currentMedicines = meds
            )
        }
    }

    fun setAddMedicineFlowActive(active: Boolean) {
        _uiState.update { it.copy(isAddMedicineFlowActive = active) }
    }

    fun selectMedicine(medication: Medication) {
        _uiState.update { it.copy(selectedMedicine = medication) }
    }
    
    fun clearSelectedMedicine() {
        _uiState.update { it.copy(selectedMedicine = null) }
    }

    fun addMedicine(
        name: String, 
        dosage: String, 
        frequency: String, 
        reminderTimes: List<Long>, 
        startDate: Long, 
        endDate: Long?, 
        foodTiming: String
    ) {
        val newMed = Medication(
            id = UUID.randomUUID().toString(),
            name = name,
            dosage = dosage,
            frequency = frequency,
            startDate = startDate,
            endDate = endDate,
            foodTiming = foodTiming,
            reminderTimes = reminderTimes
        )
        
        _medicines.value = _medicines.value + newMed
        
        val newDoses = mutableListOf<MedicationDose>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate
        
        val maxDays = if (endDate != null) {
            val diff = endDate - startDate
            (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtMost(60).coerceAtLeast(0)
        } else {
            0 // Default 0 days if no end date
        }
        
        for (dayOffset in 0..maxDays) {
            val dayStart = calendar.clone() as Calendar
            dayStart.add(Calendar.DAY_OF_YEAR, dayOffset)
            dayStart.set(Calendar.HOUR_OF_DAY, 0)
            dayStart.set(Calendar.MINUTE, 0)
            dayStart.set(Calendar.SECOND, 0)
            dayStart.set(Calendar.MILLISECOND, 0)
            
            for (reminderTimeOffset in reminderTimes) {
                val scheduledTime = dayStart.timeInMillis + reminderTimeOffset
                newDoses.add(
                    MedicationDose(
                        id = UUID.randomUUID().toString(),
                        medicationId = newMed.id,
                        scheduledTimeMillis = scheduledTime,
                        isTaken = false
                    )
                )
            }
        }
        
        _doses.value = _doses.value + newDoses
        
        updateUiState()
        setAddMedicineFlowActive(false)
    }

    fun snoozeDose(doseId: String) {
        _doses.update { list ->
            list.map {
                if (it.id == doseId) {
                    it.copy(scheduledTimeMillis = it.scheduledTimeMillis + 15 * 60 * 1000)
                } else it
            }
        }
        updateUiState()
    }

    fun deleteMedicine(medId: String) {
        _medicines.update { list -> list.filter { it.id != medId } }
        _doses.update { list -> list.filter { it.medicationId != medId } }
        clearSelectedMedicine()
        updateUiState()
    }

    fun markDoseTaken(doseId: String) {
        _doses.update { list ->
            list.map {
                if (it.id == doseId) it.copy(isTaken = true, takenTimeMillis = System.currentTimeMillis()) else it
            }
        }
        updateUiState()
    }
}
