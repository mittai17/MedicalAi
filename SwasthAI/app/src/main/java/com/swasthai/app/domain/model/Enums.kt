package com.swasthai.app.domain.model

/**
 * User roles in SwasthAI.
 *
 * CITIZEN — Self-screening users (blue theme)
 * HEALTH_WORKER — ASHA/CHW workers (green theme)
 */
enum class UserRole {
    CITIZEN,
    HEALTH_WORKER
}

/**
 * Risk classification levels for medical screening results.
 */
enum class RiskLevel {
    LOW,
    MODERATE,
    HIGH
}

/**
 * Status of a screening session.
 */
enum class ScreeningStatus {
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

/**
 * Type of screening method used.
 */
enum class ScreeningType {
    SYMPTOM_CHECK,
    VOICE_ASSISTANT,
    IMAGE_CHECK,
    COMBINED
}

/**
 * Source from which a symptom was recorded.
 */
enum class SymptomSource {
    MANUAL,
    VOICE,
    AI_EXTRACTED
}

/**
 * Status of a sync queue item.
 */
enum class SyncStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

/**
 * Type of referral action.
 */
enum class ReferralStatus {
    PENDING,
    ACCEPTED,
    COMPLETED,
    CANCELLED
}
