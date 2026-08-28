package com.swasthai.app.data.remote.api

import retrofit2.http.*

/**
 * SwasthAI Retrofit API Service.
 *
 * Defines all HTTP endpoints for the FastAPI backend.
 * In offline/stub mode all calls will fail gracefully and
 * the SyncWorker will retry when connectivity is restored.
 */
interface SwasthAIApiService {

    // ── Auth ──
    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): retrofit2.Response<Map<String, Any>>

    @POST("auth/register")
    suspend fun register(@Body body: Map<String, Any>): retrofit2.Response<Map<String, Any>>

    // ── Patients ──
    @POST("patients")
    suspend fun uploadPatient(@Body body: Map<String, Any?>): retrofit2.Response<Unit>

    @PUT("patients/{id}")
    suspend fun updatePatient(@Path("id") id: String, @Body body: Map<String, Any?>): retrofit2.Response<Unit>

    @GET("patients")
    suspend fun getPatients(): retrofit2.Response<List<Map<String, Any?>>>

    // ── Screenings ──
    @POST("screenings")
    suspend fun uploadScreening(@Body body: Map<String, Any?>): retrofit2.Response<Unit>

    @PUT("screenings/{id}")
    suspend fun updateScreening(@Path("id") id: String, @Body body: Map<String, Any?>): retrofit2.Response<Unit>

    // ── Vitals ──
    @POST("vitals")
    suspend fun uploadVitals(@Body body: Map<String, Any?>): retrofit2.Response<Unit>

    // ── Symptoms ──
    @POST("symptoms")
    suspend fun uploadSymptom(@Body body: Map<String, Any?>): retrofit2.Response<Unit>

    // ── Reports ──
    @POST("reports")
    suspend fun uploadReport(@Body body: Map<String, Any?>): retrofit2.Response<Unit>

    @GET("reports")
    suspend fun getReports(): retrofit2.Response<List<Map<String, Any?>>>

    // ── Referrals ──
    @POST("referrals")
    suspend fun uploadReferral(@Body body: Map<String, Any?>): retrofit2.Response<Unit>

    @GET("referrals/pending")
    suspend fun getPendingReferrals(): retrofit2.Response<List<Map<String, Any?>>>

    // ── Health Tips ──
    @GET("health-tips")
    suspend fun getHealthTips(): retrofit2.Response<List<Map<String, Any?>>>

    // ── Sync ──
    @POST("sync/batch")
    suspend fun syncBatch(@Body body: List<Map<String, Any?>>): retrofit2.Response<Map<String, Any>>

    // ── AI Fallback (remote Gemma second opinion) ──
    @POST("ai/fallback")
    suspend fun getAiFallback(
        @Body body: Map<String, Any?>
    ): retrofit2.Response<Map<String, Any>>
}
