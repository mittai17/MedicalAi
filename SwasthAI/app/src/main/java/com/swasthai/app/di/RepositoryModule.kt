package com.swasthai.app.di

import com.swasthai.app.data.local.database.dao.PatientDao
import com.swasthai.app.data.local.database.dao.ReferralDao
import com.swasthai.app.data.local.database.dao.ReportDao
import com.swasthai.app.data.local.database.dao.ScreeningDao
import com.swasthai.app.data.local.database.dao.SyncQueueDao
import com.swasthai.app.data.local.database.dao.UserDao
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.data.repository.AuthRepositoryImpl
import com.swasthai.app.data.repository.PatientRepositoryImpl
import com.swasthai.app.data.repository.ScreeningRepositoryImpl
import com.swasthai.app.data.repository.ReportRepositoryImpl
import com.swasthai.app.data.repository.SyncRepositoryImpl
import com.swasthai.app.domain.repository.AuthRepository
import com.swasthai.app.domain.repository.PatientRepository
import com.swasthai.app.domain.repository.ScreeningRepository
import com.swasthai.app.domain.repository.ReportRepository
import com.swasthai.app.domain.repository.SyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding repository interfaces to their implementations.
 *
 * All repositories follow the Repository Pattern from Clean Architecture,
 * abstracting the data sources from the domain layer.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        userDao: UserDao,
        userPreferences: UserPreferences
    ): AuthRepository = AuthRepositoryImpl(userDao, userPreferences)

    @Provides
    @Singleton
    fun providePatientRepository(
        patientDao: PatientDao,
        syncQueueDao: SyncQueueDao
    ): PatientRepository = PatientRepositoryImpl(patientDao, syncQueueDao)

    @Provides
    @Singleton
    fun provideScreeningRepository(
        screeningDao: ScreeningDao,
        syncQueueDao: SyncQueueDao
    ): ScreeningRepository = ScreeningRepositoryImpl(screeningDao, syncQueueDao)

    @Provides
    @Singleton
    fun provideReportRepository(
        reportDao: ReportDao,
        referralDao: ReferralDao
    ): ReportRepository = ReportRepositoryImpl(reportDao, referralDao)

    @Provides
    @Singleton
    fun provideSyncRepository(
        syncQueueDao: SyncQueueDao
    ): SyncRepository = SyncRepositoryImpl(syncQueueDao)
}
