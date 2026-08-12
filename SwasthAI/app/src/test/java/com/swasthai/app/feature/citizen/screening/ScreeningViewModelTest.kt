package com.swasthai.app.feature.citizen.screening

import com.swasthai.app.ai.engine.AIEngineManager
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.*
import com.swasthai.app.domain.repository.ScreeningRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreeningViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var screeningRepository: ScreeningRepository
    private lateinit var aiEngineManager: AIEngineManager
    private lateinit var userPreferences: UserPreferences
    private lateinit var viewModel: ScreeningViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        screeningRepository = mockk(relaxed = true)
        aiEngineManager = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)

        every { userPreferences.userPhoneFlow } returns flowOf("1234567890")
        every { userPreferences.userNameFlow } returns flowOf("Test User")

        viewModel = ScreeningViewModel(screeningRepository, aiEngineManager, userPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertEquals(ScreeningStep.SYMPTOM_SELECTION, state.currentStep)
        assertTrue(state.availableSymptoms.isNotEmpty())
        assertNull(state.selectedDuration)
        assertEquals("", state.temperature)
    }

    @Test
    fun testToggleSymptom() {
        val symptomName = "Fever"
        viewModel.toggleSymptom(symptomName)
        val state = viewModel.uiState.value
        val feverSymptom = state.availableSymptoms.find { it.name == symptomName }
        assertNotNull(feverSymptom)
        assertTrue(feverSymptom!!.isSelected)
    }

    @Test
    fun testNavigationFlow() {
        viewModel.goToNextStep()
        assertEquals(ScreeningStep.DURATION_SELECTION, viewModel.uiState.value.currentStep)

        viewModel.goToNextStep()
        assertEquals(ScreeningStep.VITALS_INPUT, viewModel.uiState.value.currentStep)

        viewModel.goToPreviousStep()
        assertEquals(ScreeningStep.DURATION_SELECTION, viewModel.uiState.value.currentStep)
    }
}
