package com.swasthai.app.feature.healthworker.patients

import com.swasthai.app.domain.model.Patient
import com.swasthai.app.domain.repository.PatientRepository
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
class PatientViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var patientRepository: PatientRepository
    private lateinit var viewModel: PatientViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        patientRepository = mockk(relaxed = true)

        every { patientRepository.getAllPatients() } returns flowOf(
            listOf(
                Patient(id = "1", name = "John Doe", age = 30, gender = "Male", village = "Village A"),
                Patient(id = "2", name = "Jane Smith", age = 25, gender = "Female", village = "Village B")
            )
        )
        every { patientRepository.searchPatients(any()) } returns flowOf(
            listOf(
                Patient(id = "1", name = "John Doe", age = 30, gender = "Male", village = "Village A")
            )
        )

        viewModel = PatientViewModel(patientRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState() = runTest {
        val state = viewModel.listUiState.value
        assertEquals("", state.searchQuery)
        assertTrue(state.isLoading)
    }

    @Test
    fun testLoadPatientsUpdatesState() = runTest {
        testScheduler.advanceUntilIdle()
        val state = viewModel.listUiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.patients.size)
        assertEquals("John Doe", state.patients[0].name)
    }

    @Test
    fun testUpdateSearchQuery() = runTest {
        viewModel.updateSearchQuery("John")
        testScheduler.advanceUntilIdle()
        val state = viewModel.listUiState.value
        assertEquals("John", state.searchQuery)
        assertEquals(1, state.patients.size)
        assertEquals("John Doe", state.patients[0].name)
    }

    @Test
    fun testFormValidation() {
        assertFalse(viewModel.isAddPatientFormValid())

        viewModel.updateName("Test Patient")
        viewModel.updateAge("30")
        assertTrue(viewModel.isAddPatientFormValid())
    }

    @Test
    fun testSavePatientSuccess() = runTest {
        viewModel.updateName("New Patient")
        viewModel.updateAge("40")
        viewModel.updateGender("Female")
        viewModel.updateVillage("Village C")
        viewModel.updatePhone("9876543210")
        viewModel.updateAadhar("123456789012")

        coEvery { patientRepository.savePatient(any()) } returns Result.success(Unit)

        viewModel.savePatient()
        testScheduler.advanceUntilIdle()

        val addState = viewModel.addUiState.value
        assertNull(addState.error)
        assertFalse(addState.isSaving)
        assertNotNull(addState.savedPatientId)
    }
}
