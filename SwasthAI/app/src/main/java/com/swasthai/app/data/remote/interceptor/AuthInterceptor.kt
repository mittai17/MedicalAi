package com.swasthai.app.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that adds authentication headers to API requests.
 *
 * Currently uses a simple token-based approach. In production,
 * this would integrate with Firebase Auth tokens.
 */
class AuthInterceptor : Interceptor {

    @Volatile
    private var authToken: String? = null

    fun setToken(token: String?) {
        authToken = token
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val request = if (authToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $authToken")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()
        } else {
            originalRequest.newBuilder()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()
        }

        return chain.proceed(request)
    }
}
