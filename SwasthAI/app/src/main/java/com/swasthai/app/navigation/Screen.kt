package com.swasthai.app.navigation

/**
 * Sealed class defining all navigation routes in SwasthAI.
 *
 * Organized into three groups:
 * 1. Onboarding / Auth routes (shared)
 * 2. Citizen routes (blue theme)
 * 3. Health Worker routes (green theme)
 */
sealed class Screen(val route: String) {

    // ═══════════════════════════════════════
    // ONBOARDING
    // ═══════════════════════════════════════

    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object LanguageSelection : Screen("language_selection")
    data object RoleSelection : Screen("role_selection")

    // ═══════════════════════════════════════
    // CITIZEN ROUTES
    // ═══════════════════════════════════════

    data object CitizenHome : Screen("citizen_home")
    data object CitizenDashboard : Screen("citizen_dashboard")
    data object HealthCheck : Screen("health_check?mode={mode}") {
        /** Defaults to the symptoms (typed) method when [mode] is omitted. */
        fun createRoute(mode: String = "symptoms"): String =
            if (mode == "symptoms") "health_check" else "health_check?mode=$mode"
    }
    data object SymptomCheck : Screen("symptom_check")
    data object VoiceCommand : Screen("voice_command")
    data object ImageCheck : Screen("image_check")
    data object VitalsInput : Screen("vitals_input")
    data object ScreeningResult : Screen("screening_result")
    data object ConnectProviders : Screen("connect_providers")
    data object OnlineConsultation : Screen("online_consultation")
    data object HealthRecords : Screen("health_records")
    data object RecordDetail : Screen("record_detail/{screeningId}") {
        fun createRoute(screeningId: String) = "record_detail/$screeningId"
    }
    data object ShareReport : Screen("share_report/{screeningId}") {
        fun createRoute(screeningId: String) = "share_report/$screeningId"
    }
    data object AlertsReminders : Screen("alerts_reminders")
    data object HealthTips : Screen("health_tips")
    data object CitizenProfile : Screen("citizen_profile")
    data object EditProfile : Screen("edit_profile")
    data object AIChat : Screen("ai_chat")

    // ═══════════════════════════════════════
    // HEALTH WORKER ROUTES
    // ═══════════════════════════════════════

    data object HWDashboard : Screen("hw_dashboard")
    data object PatientList : Screen("patient_list")
    data object PatientDetail : Screen("patient_detail/{patientId}") {
        fun createRoute(patientId: String) = "patient_detail/$patientId"
    }
    data object AddPatient : Screen("add_patient")
    data object HWScreening : Screen("hw_screening/{patientId}") {
        fun createRoute(patientId: String) = "hw_screening/$patientId"
    }
    data object HWScreeningResult : Screen("hw_screening_result")
    data object FollowUpDecision : Screen("follow_up_decision")
    data object ScheduleFollowUp : Screen("schedule_follow_up")
    data object ReferToPHC : Screen("refer_to_phc")
    data object HWReports : Screen("hw_reports")
    data object SyncData : Screen("sync_data")
    data object HWAlerts : Screen("hw_alerts")
    data object HWProfile : Screen("hw_profile")
    data object Settings : Screen("settings")
    data object HelpSupport : Screen("help_support")
}

/**
 * Bottom navigation destinations for Citizen.
 *
 * Home, Records, Health Check, Connect and AI are tabs. Health Check sits in
 * the centre of the bar as the primary action. Alerts and Profile are surfaced
 * from the dashboard top bar (bell + avatar) instead, so they are NOT
 * bottom-nav destinations anymore.
 */
enum class CitizenBottomNav(val route: String, val label: String, val icon: String) {
    HOME(Screen.CitizenDashboard.route, "Home", "home"),
    RECORDS(Screen.HealthRecords.route, "Records", "description"),
    HEALTH(Screen.HealthCheck.route, "Health Check", "health_and_safety"),
    CONNECT(Screen.ConnectProviders.route, "Connect", "favorite"),
    AI(Screen.AIChat.route, "AI", "auto_awesome")
}

/**
 * Bottom navigation destinations for Health Worker.
 */
enum class HWBottomNav(val route: String, val label: String, val icon: String) {
    HOME(Screen.HWDashboard.route, "Home", "home"),
    PATIENTS(Screen.PatientList.route, "Patients", "people"),
    REPORTS(Screen.HWReports.route, "Reports", "assessment"),
    ALERTS(Screen.HWAlerts.route, "Alerts", "notifications"),
    MORE("more", "More", "more_horiz")
}
