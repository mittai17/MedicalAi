package com.swasthai.app.domain.repository

import com.swasthai.app.domain.model.User
import com.swasthai.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations.
 */
interface AuthRepository {
    suspend fun login(phone: String, password: String): Result<User>
    suspend fun loginOffline(phone: String, password: String): Result<User>
    suspend fun register(name: String, phone: String, password: String, role: UserRole): Result<User>
    suspend fun getCurrentUser(): User?
    suspend fun logout()
    val isLoggedIn: Flow<Boolean>
}

/**
 * Repository interface for patient management operations.
 */
interface PatientRepository {
    suspend fun registerPatient(patient: com.swasthai.app.domain.model.Patient): Result<Unit>
    /** Alias for registerPatient — used by AddPatient flow. */
    suspend fun savePatient(patient: com.swasthai.app.domain.model.Patient): Result<Unit>
    suspend fun updatePatient(patient: com.swasthai.app.domain.model.Patient): Result<Unit>
    suspend fun getPatientById(patientId: String): com.swasthai.app.domain.model.Patient?
    fun getPatientsByHealthWorker(healthWorkerId: String): Flow<List<com.swasthai.app.domain.model.Patient>>
    fun getAllPatients(): Flow<List<com.swasthai.app.domain.model.Patient>>
    fun searchPatients(query: String): Flow<List<com.swasthai.app.domain.model.Patient>>
    suspend fun getPatientCount(healthWorkerId: String): Int
}

/**
 * Repository interface for screening operations.
 */
interface ScreeningRepository {
    suspend fun createScreening(screening: com.swasthai.app.domain.model.Screening): Result<String>
    suspend fun updateScreening(screening: com.swasthai.app.domain.model.Screening): Result<Unit>
    suspend fun getScreeningById(screeningId: String): com.swasthai.app.domain.model.Screening?
    fun getScreeningsByUser(userId: String): Flow<List<com.swasthai.app.domain.model.Screening>>
    fun getRecentScreenings(userId: String, limit: Int = 5): Flow<List<com.swasthai.app.domain.model.Screening>>
    suspend fun saveVitals(vitals: com.swasthai.app.domain.model.Vitals): Result<Unit>
    suspend fun saveSymptoms(symptoms: List<com.swasthai.app.domain.model.Symptom>): Result<Unit>
    suspend fun saveDiagnosisResult(result: com.swasthai.app.domain.model.DiagnosisResult): Result<Unit>
    suspend fun getTodayScreeningCount(userId: String): Int
    suspend fun getScreeningCountInRange(userId: String, startDate: Long, endDate: Long): Int
    suspend fun getHighRiskCountInRange(userId: String, startDate: Long, endDate: Long): Int
}

/**
 * Repository interface for report operations.
 */
interface ReportRepository {
    suspend fun saveReport(report: com.swasthai.app.domain.model.Report): Result<Unit>
    suspend fun getReportByScreening(screeningId: String): com.swasthai.app.domain.model.Report?
    fun getAllReports(): Flow<List<com.swasthai.app.domain.model.Report>>
    suspend fun saveReferral(referral: com.swasthai.app.domain.model.Referral): Result<Unit>
    suspend fun getReferralByDiagnosis(diagnosisId: String): com.swasthai.app.domain.model.Referral?
    fun getPendingReferrals(): Flow<List<com.swasthai.app.domain.model.Referral>>
    suspend fun getPendingReferralCount(): Int
}

/**
 * Repository interface for sync operations.
 */
interface SyncRepository {
    fun getPendingSyncCount(): Flow<Int>
    suspend fun syncPendingData(): Result<Int>
    suspend fun clearCompletedSyncItems()
}
