package com.swasthai.app.data.remote

import com.swasthai.app.data.local.datastore.UserPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies the anonymous device identity used for backend sync.
 *
 * SwasthAI deliberately has no login/account: the stable per-device user id
 * (from [UserPreferences.stableUserId]) doubles as the device id sent as the
 * `X-Device-Id` header so records stay attributed to the right device across
 * app restarts. The id is resolved once then cached; Ktor's `defaultRequest`
 * reads the cache synchronously.
 */
@Singleton
class SessionTokenProvider @Inject constructor(
    private val userPreferences: UserPreferences
) {

    @Volatile
    var cachedDeviceId: String = ""
        private set

    /**
     * Returns the stable device id, resolving it from DataStore on first use.
     * Safe to call after the cache is primed (e.g. from each API call).
     */
    suspend fun ensureDeviceId(): String {
        if (cachedDeviceId.isBlank()) {
            cachedDeviceId = userPreferences.stableUserId()
        }
        return cachedDeviceId
    }
}