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
    // ONBOARDING & AUTH
    // ═══════════════════════════════════════

    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object LanguageSelection : Screen("language_selection")
    data object RoleSelection : Screen("role_selection")
    data object Login : Screen("login")
    data object Register : Screen("register")

    // ═══════════════════════════════════════
    // CITIZEN ROUTES
    // ═══════════════════════════════════════

    data object CitizenDashboard : Screen("citizen_dashboard")
    data object SymptomCheck : Screen("symptom_check")
    data object VoiceAssistant : Screen("voice_assistant")
    data object ImageCheck : Screen("image_check")
    data object VitalsInput : Screen("vitals_input")
    data object ScreeningResult : Screen("screening_result")
    data object ConnectProviders : Screen("connect_providers")
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
 */
enum class CitizenBottomNav(val route: String, val label: String, val icon: String) {
    HOME(Screen.CitizenDashboard.route, "Home", "home"),
    RECORDS(Screen.HealthRecords.route, "Records", "description"),
    CONNECT(Screen.ConnectProviders.route, "Connect", "favorite"),
    ALERTS(Screen.AlertsReminders.route, "Alerts", "notifications"),
    PROFILE(Screen.CitizenProfile.route, "Profile", "person")
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
