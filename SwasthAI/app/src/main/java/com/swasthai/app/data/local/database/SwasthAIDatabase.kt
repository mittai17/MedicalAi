package com.swasthai.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.swasthai.app.data.local.database.dao.ConsultationRequestDao
import com.swasthai.app.data.local.database.dao.PatientDao
import com.swasthai.app.data.local.database.dao.ReferralDao
import com.swasthai.app.data.local.database.dao.ReportDao
import com.swasthai.app.data.local.database.dao.ScreeningDao
import com.swasthai.app.data.local.database.dao.SyncQueueDao
import com.swasthai.app.data.local.database.dao.UserDao
import com.swasthai.app.data.local.database.entity.ConsultationRequestEntity
import com.swasthai.app.data.local.database.entity.DiagnosisResultEntity
import com.swasthai.app.data.local.database.entity.ImageRecordEntity
import com.swasthai.app.data.local.database.entity.PatientEntity
import com.swasthai.app.data.local.database.entity.RecommendationEntity
import com.swasthai.app.data.local.database.entity.ReferralEntity
import com.swasthai.app.data.local.database.entity.ReportEntity
import com.swasthai.app.data.local.database.entity.ScreeningEntity
import com.swasthai.app.data.local.database.entity.SymptomRecordEntity
import com.swasthai.app.data.local.database.entity.SyncQueueEntity
import com.swasthai.app.data.local.database.entity.UserEntity
import com.swasthai.app.data.local.database.entity.VitalsEntity

/**
 * SwasthAI Room Database
 *
 * Encrypted with SQLCipher for HIPAA-level data protection.
 * Contains all local health data: users, patients, screenings,
 * vitals, symptoms, diagnosis results, reports, referrals, and sync queue.
 */
@Database(
    entities = [
        UserEntity::class,
        PatientEntity::class,
        ScreeningEntity::class,
        VitalsEntity::class,
        SymptomRecordEntity::class,
        ImageRecordEntity::class,
        DiagnosisResultEntity::class,
        RecommendationEntity::class,
        ReportEntity::class,
        ReferralEntity::class,
        SyncQueueEntity::class,
        ConsultationRequestEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class SwasthAIDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun patientDao(): PatientDao
    abstract fun screeningDao(): ScreeningDao
    abstract fun reportDao(): ReportDao
    abstract fun referralDao(): ReferralDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun consultationRequestDao(): ConsultationRequestDao

    companion object {
        const val DATABASE_NAME = "swasthai_database"
        // Database encryption passphrase — in production, derive from device-specific key
        const val DATABASE_PASSPHRASE = "SwasthAI_Secure_2024_Edge_Health"
    }
}
