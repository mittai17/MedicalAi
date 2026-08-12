package com.swasthai.app.di

import android.content.Context
import androidx.room.Room
import com.swasthai.app.data.local.database.SwasthAIDatabase
import com.swasthai.app.data.local.database.dao.PatientDao
import com.swasthai.app.data.local.database.dao.ReferralDao
import com.swasthai.app.data.local.database.dao.ReportDao
import com.swasthai.app.data.local.database.dao.ScreeningDao
import com.swasthai.app.data.local.database.dao.SyncQueueDao
import com.swasthai.app.data.local.database.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

/**
 * Hilt module providing Room Database and all DAOs.
 *
 * The database is encrypted using SQLCipher for secure
 * storage of patient health data on-device.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SwasthAIDatabase {
        val passphrase = SwasthAIDatabase.DATABASE_PASSPHRASE.toByteArray()
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context,
            SwasthAIDatabase::class.java,
            SwasthAIDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserDao(database: SwasthAIDatabase): UserDao = database.userDao()

    @Provides
    fun providePatientDao(database: SwasthAIDatabase): PatientDao = database.patientDao()

    @Provides
    fun provideScreeningDao(database: SwasthAIDatabase): ScreeningDao = database.screeningDao()

    @Provides
    fun provideReportDao(database: SwasthAIDatabase): ReportDao = database.reportDao()

    @Provides
    fun provideReferralDao(database: SwasthAIDatabase): ReferralDao = database.referralDao()

    @Provides
    fun provideSyncQueueDao(database: SwasthAIDatabase): SyncQueueDao = database.syncQueueDao()
}
