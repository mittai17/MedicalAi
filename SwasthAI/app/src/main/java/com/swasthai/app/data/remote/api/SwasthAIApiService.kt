package com.swasthai.app.data.remote.api

import com.swasthai.app.BuildConfig
import com.swasthai.app.data.remote.SessionTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SwasthAI Ktor API service.
 *
 * Defines all HTTP endpoints for the FastAPI backend. SwasthAI has no
 * login/account: requests carry the anonymous device id header (see
 * [SessionTokenProvider]) and bodies are built with org.json, so the service
 * stays fully offline-capable — every call fails gracefully (returns null /
 * false) and sync simply retries when connectivity returns.
 */
@Singleton
class SwasthAIApiService @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionTokenProvider: SessionTokenProvider
) {

    // ── Auth (legacy; SwasthAI is login-free but the endpoints stay wired) ──
    suspend fun login(body: Map<String, String>): JSONObject? =
        postObject("auth/login", toJsonObject(body).toString())

    suspend fun register(body: Map<String, Any>): JSONObject? =
        postObject("auth/register", toJsonObject(body).toString())

    // ── Patients ──
    suspend fun uploadPatient(body: Map<String, Any?>): Boolean = postOk("patients", body)
    suspend fun updatePatient(id: String, body: Map<String, Any?>): Boolean = putOk("patients/$id", body)
    suspend fun getPatients(): JSONArray? = getArray("patients")

    // ── Screenings ──
    suspend fun uploadScreening(body: Map<String, Any?>): Boolean = postOk("screenings", body)
    suspend fun updateScreening(id: String, body: Map<String, Any?>): Boolean = putOk("screenings/$id", body)

    // ── Vitals / Symptoms / Reports ──
    suspend fun uploadVitals(body: Map<String, Any?>): Boolean = postOk("vitals", body)
    suspend fun uploadSymptom(body: Map<String, Any?>): Boolean = postOk("symptoms", body)
    suspend fun uploadReport(body: Map<String, Any?>): Boolean = postOk("reports", body)
    suspend fun getReports(): JSONArray? = getArray("reports")

    // ── Referrals ──
    suspend fun uploadReferral(body: Map<String, Any?>): Boolean = postOk("referrals", body)
    suspend fun getPendingReferrals(): JSONArray? = getArray("referrals/pending")

    // ── Health Tips ──
    suspend fun getHealthTips(): JSONArray? = getArray("health-tips")

    // ── Sync ──
    suspend fun syncBatch(body: List<Map<String, Any?>>): JSONObject? =
        postObject("sync/batch", JSONArray(body.map { toJsonObject(it) }).toString())

    // ── Consultation requests (telemedicine tracker) ──
    suspend fun uploadConsultation(body: Map<String, Any?>): Boolean = postOk("consultations", body)

    // ── AI Fallback (remote Gemma second opinion) ──
    suspend fun getAiFallback(body: Map<String, Any?>): JSONObject? =
        postObject("ai/fallback", toJsonObject(body).toString())

    // ── Core helpers ──

    private fun url(path: String): String = BuildConfig.API_BASE_URL + path

    private suspend fun send(method: HttpMethod, path: String, body: String?): HttpResponse {
        sessionTokenProvider.ensureDeviceId()
        val content = body?.let { TextContent(it, ContentType.Application.Json) }
        return when (method) {
            HttpMethod.Post -> httpClient.post(url(path)) { content?.let { setBody(it) } }
            HttpMethod.Put -> httpClient.put(url(path)) { content?.let { setBody(it) } }
            else -> httpClient.get(url(path))
        }
    }

    private suspend fun postOk(path: String, body: Map<String, Any?>): Boolean =
        try {
            send(HttpMethod.Post, path, toJsonObject(body).toString()).status.isSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }

    private suspend fun putOk(path: String, body: Map<String, Any?>): Boolean =
        try {
            send(HttpMethod.Put, path, toJsonObject(body).toString()).status.isSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }

    private suspend fun postObject(path: String, json: String): JSONObject? =
        try {
            val text = send(HttpMethod.Post, path, json).bodyAsText()
            if (text.isBlank()) null else runCatching { JSONObject(text) }.getOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    private suspend fun getArray(path: String): JSONArray? =
        try {
            val text = send(HttpMethod.Get, path, null).bodyAsText()
            if (text.isBlank()) null else runCatching { JSONArray(text) }.getOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    private fun toJsonObject(body: Map<String, Any?>): JSONObject {
        val obj = JSONObject()
        body.forEach { (key, value) -> obj.put(key, wrap(value)) }
        return obj
    }

    private fun wrap(value: Any?): Any? = when (value) {
        null -> JSONObject.NULL
        is JSONObject, is JSONArray, is String, is Boolean -> value
        is Number -> value
        is Map<*, *> -> {
            val nested = JSONObject()
            value.forEach { (key, child) -> nested.put(key.toString(), wrap(child)) }
            nested
        }
        is List<*> -> JSONArray(value.map { wrap(it) })
        is Array<*> -> JSONArray(value.map { wrap(it) })
        else -> value.toString()
    }
}