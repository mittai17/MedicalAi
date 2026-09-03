package com.swasthai.app.core.voice

import com.swasthai.app.ai.engine.ScanType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandParserTest {

    private val parser = VoiceCommandParser()
    private val hour = 3600L * 1000L
    private val day = 24L * hour

    // ── Intent classification ──

    @Test
    fun greeting() {
        val cmd = parser.parse("good morning")
        assertTrue(cmd.intent is VoiceIntent.Greeting)
        assertTrue(cmd.reply.isNotBlank())
    }

    @Test
    fun hindiGreeting() {
        val cmd = parser.parse("namaste", "hi")
        assertTrue(cmd.intent is VoiceIntent.Greeting)
        assertTrue(cmd.reply.isNotBlank())
    }

    @Test
    fun cancel() {
        assertTrue(parser.parse("never mind, forget it").intent is VoiceIntent.Cancel)
    }

    @Test
    fun emergency() {
        assertTrue(parser.parse("I need emergency help right now").intent is VoiceIntent.Emergency)
    }

    @Test
    fun help() {
        val cmd = parser.parse("help")
        assertTrue(cmd.intent is VoiceIntent.Help)
    }

    @Test
    fun findCare() {
        assertTrue(parser.parse("find the nearest hospital").intent is VoiceIntent.FindCare)
    }

    @Test
    fun hindiFindCare() {
        assertTrue(parser.parse("sabse paas ka hospital dhundho", "hi").intent is VoiceIntent.FindCare)
    }

    @Test
    fun showRecords() {
        assertTrue(parser.parse("show my records").intent is VoiceIntent.ShowRecords)
    }

    @Test
    fun showReminders() {
        assertTrue(parser.parse("show my reminders").intent is VoiceIntent.ShowReminders)
    }

    @Test
    fun showRemindersHindiModeAcceptsEnglishPhrase() {
        val cmd = parser.parse("show my reminders", "hi")
        assertTrue(cmd.intent is VoiceIntent.ShowReminders)
    }

    @Test
    fun showRecordsHindiModeAcceptsEnglishPhrase() {
        assertTrue(parser.parse("show my records", "hi").intent is VoiceIntent.ShowRecords)
    }

    // ── Health check ──

    @Test
    fun healthCheckEnglish() {
        val cmd = parser.parse("I have fever and cough since 2 days")
        assertTrue(cmd.intent is VoiceIntent.HealthCheck)
        val hc = cmd.intent as VoiceIntent.HealthCheck
        assertNotNull(hc.symptoms)
        assertTrue(hc.symptoms.orEmpty().contains("fever"))
    }

    @Test
    fun healthCheckHindi() {
        assertTrue(parser.parse("mujhe bukhaar hai", "hi").intent is VoiceIntent.HealthCheck)
    }

    @Test
    fun helpInsideLongHealthCheckFallsThroughToHealth() {
        val cmd = parser.parse("can you help me I have fever and cough since last two days")
        assertTrue(cmd.intent is VoiceIntent.HealthCheck)
    }

    @Test
    fun unknown() {
        assertTrue(parser.parse("the sky is blue today").intent is VoiceIntent.Unknown)
    }

    @Test
    fun blank() {
        assertTrue(parser.parse(" ").intent is VoiceIntent.Unknown)
    }

    // ── Camera / scan type ──

    @Test
    fun cameraDefaultsToChest() {
        val cmd = parser.parse("take a photo")
        val cam = cmd.intent as? VoiceIntent.StartCamera
        assertNotNull(cam)
        assertEquals(ScanType.PNEUMONIA, cam?.scanType)
    }

    @Test
    fun photoOfChest() {
        val cmd = parser.parse("take a photo of my chest")
        assertEquals(ScanType.PNEUMONIA, (cmd.intent as VoiceIntent.StartCamera).scanType)
    }

    @Test
    fun scanSkin() {
        val cmd = parser.parse("scan my skin")
        assertEquals(ScanType.SKIN_LESION, (cmd.intent as VoiceIntent.StartCamera).scanType)
    }

    @Test
    fun pictureOfEye() {
        val cmd = parser.parse("take a picture of my eye")
        assertEquals(ScanType.RETINA, (cmd.intent as VoiceIntent.StartCamera).scanType)
    }

    @Test
    fun hindiCamera() {
        assertTrue(parser.parse("camera kholo photo lo", "hi").intent is VoiceIntent.StartCamera)
    }

    // ── Consultation ──

    @Test
    fun bookConsultation() {
        val cmd = parser.parse("book a consultation for fever")
        val con = cmd.intent as? VoiceIntent.BookConsultation
        assertNotNull(con)
        assertEquals("fever", con?.reason?.lowercase())
        assertEquals("NORMAL", con?.urgency)
    }

    @Test
    fun urgentConsultation() {
        val cmd = parser.parse("book an appointment urgent, it is critical")
        val con = cmd.intent as? VoiceIntent.BookConsultation
        assertNotNull(con)
        assertEquals("HIGH", con?.urgency)
    }

    @Test
    fun hindiConsultation() {
        val cmd = parser.parse("doctor se milna hai", "hi")
        assertTrue(cmd.intent is VoiceIntent.BookConsultation)
    }

    // ── Reminders ──

    @Test
    fun reminderWithTimeAndRepeat() {
        val cmd = parser.parse("remind me to take my medicine at 9 in the morning every day")
        val rem = cmd.intent as? VoiceIntent.SetReminder
        assertNotNull(rem)
        assertEquals("take my medicine", rem?.title)
        assertEquals(9L * hour, rem?.timeOfDayMillis)
        assertEquals(day, rem?.repeatIntervalMillis)
    }

    @Test
    fun reminderEvening() {
        val cmd = parser.parse("remind me to take my medicine at 7 in the evening")
        val rem = cmd.intent as? VoiceIntent.SetReminder
        assertEquals(19L * hour, rem?.timeOfDayMillis)
    }

    @Test
    fun reminderNoRepeat() {
        val cmd = parser.parse("set a reminder to drink water")
        val rem = cmd.intent as? VoiceIntent.SetReminder
        assertNotNull(rem)
        assertEquals(null, rem?.repeatIntervalMillis)
    }

    @Test
    fun hindiReminder() {
        val cmd = parser.parse("mujhe yaad dilao dawa lene ki", "hi")
        val rem = cmd.intent as? VoiceIntent.SetReminder
        assertNotNull(rem)
        assertEquals(null, rem?.repeatIntervalMillis)
    }

    // ── Time / repeat helpers ──

    @Test
    fun parseExactAmPm() {
        assertEquals(18L * hour, parser.parseTimeOfDay("please do it at 6 pm", "en"))
        assertEquals(9L * hour, parser.parseTimeOfDay("at 9 am", "en"))
    }

    @Test
    fun parsePeriodWords() {
        assertEquals(12L * hour, parser.parseTimeOfDay("at noon", "en"))
        assertEquals(21L * hour, parser.parseTimeOfDay("remind me at night", "en"))
    }

    @Test
    fun parseRepeat() {
        assertEquals(2L * hour, parser.parseRepeatInterval("every 2 hours", "en"))
        assertEquals(day, parser.parseRepeatInterval("every day", "en"))
        assertEquals(null, parser.parseRepeatInterval("no pattern here", "en"))
    }

    @Test
    fun confirmedReminderHindiUsesColonPhrasing() {
        val reply = parser.confirmedReminder("hi", "take my medicine", "9:00 AM", "har din.")
        assertTrue(reply.contains("Reminder set ho gaya: take my medicine. 9:00 AM."))
        assertTrue(reply.contains("har din"))
    }

    @Test
    fun confirmedReminderEnglish() {
        val reply = parser.confirmedReminder("en", "take my medicine", "9:00 AM", "every day")
        assertTrue(reply.contains("Reminder set for take my medicine. 9:00 AM."))
    }
}