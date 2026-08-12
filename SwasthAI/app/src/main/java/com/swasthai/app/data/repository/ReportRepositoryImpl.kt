package com.swasthai.app.data.repository

import com.swasthai.app.data.local.database.dao.ReferralDao
import com.swasthai.app.data.local.database.dao.ReportDao
import com.swasthai.app.data.mapper.toDomain
import com.swasthai.app.data.mapper.toEntity
import com.swasthai.app.domain.model.Referral
import com.swasthai.app.domain.model.Report
import com.swasthai.app.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of ReportRepository.
 * Manages medical reports and patient referrals.
 */
class ReportRepositoryImpl @Inject constructor(
    private val reportDao: ReportDao,
    private val referralDao: ReferralDao
) : ReportRepository {

    override suspend fun saveReport(report: Report): Result<Unit> {
        return try {
            reportDao.insertReport(report.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReportByScreening(screeningId: String): Report? {
        return reportDao.getReportByScreening(screeningId)?.toDomain()
    }

    override fun getAllReports(): Flow<List<Report>> {
        return reportDao.getAllReports().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveReferral(referral: Referral): Result<Unit> {
        return try {
            referralDao.insertReferral(referral.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReferralByDiagnosis(diagnosisId: String): Referral? {
        return referralDao.getReferralByDiagnosis(diagnosisId)?.toDomain()
    }

    override fun getPendingReferrals(): Flow<List<Referral>> {
        return referralDao.getPendingReferrals().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPendingReferralCount(): Int {
        return referralDao.getPendingReferralCount()
    }
}
