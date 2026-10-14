package com.humraahi.ui.itinerary

import androidx.lifecycle.ViewModelStore
import com.humraahi.model.Itinerary
import com.humraahi.model.ItineraryActivity
import com.humraahi.model.ItineraryDay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItineraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val viewModelStore = ViewModelStore()
    private val itinerary = Itinerary(
        tripId = "trip-1",
        days = listOf(ItineraryDay(id = "day-1", dayNumber = 1, title = "Arrival"))
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        updates: Flow<Itinerary?>,
        addActivity: suspend (Int, ItineraryActivity) -> Unit = { _, _ -> }
    ): ItineraryViewModel =
        ItineraryViewModel(updates, addActivity).also { viewModelStore.put("itinerary", it) }

    @Test
    fun startsLoadingUntilFirstEmission() = runTest(dispatcher) {
        val viewModel = createViewModel(flow { awaitCancellation() })

        assertEquals(ItineraryUiState.Loading, viewModel.uiState.value)
        runCurrent()
        assertEquals(ItineraryUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun missingDocumentShowsEmpty() = runTest(dispatcher) {
        val viewModel = createViewModel(flowOf(null))

        runCurrent()

        assertEquals(ItineraryUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun planWithoutDaysShowsEmpty() = runTest(dispatcher) {
        val viewModel = createViewModel(flowOf(Itinerary(tripId = "trip-1")))

        runCurrent()

        assertEquals(ItineraryUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun successTracksUpdatesAndDocumentRemoval() = runTest(dispatcher) {
        val updates = MutableStateFlow<Itinerary?>(itinerary)
        val viewModel = createViewModel(updates)
        runCurrent()
        assertEquals(ItineraryUiState.Success(itinerary), viewModel.uiState.value)

        val updated = itinerary.copy(days = listOf(itinerary.days.single().copy(title = "Beach day")))
        updates.value = updated
        runCurrent()
        assertEquals(ItineraryUiState.Success(updated), viewModel.uiState.value)

        updates.value = null
        runCurrent()
        assertEquals(ItineraryUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun observationFailureShowsError() = runTest(dispatcher) {
        val viewModel = createViewModel(flow { throw IllegalStateException("Permission denied") })

        runCurrent()

        assertEquals(ItineraryUiState.Error("Permission denied"), viewModel.uiState.value)
    }

    @Test
    fun errorWithoutMessageUsesHelpfulDescription() = runTest(dispatcher) {
        val viewModel = createViewModel(flow { throw IllegalStateException() })

        runCurrent()

        assertEquals(
            ItineraryUiState.Error("Itinerary could not be loaded."),
            viewModel.uiState.value
        )
    }

    @Test
    fun retryReturnsToLoadingAndStartsNewObservation() = runTest(dispatcher) {
        var attempts = 0
        val viewModel = createViewModel(flow {
            attempts++
            if (attempts == 1) {
                throw IllegalStateException("Connection failed")
            }
            emit(itinerary)
        })
        runCurrent()
        assertEquals(ItineraryUiState.Error("Connection failed"), viewModel.uiState.value)

        viewModel.retry()
        assertEquals(ItineraryUiState.Loading, viewModel.uiState.value)
        runCurrent()

        assertEquals(2, attempts)
        assertEquals(ItineraryUiState.Success(itinerary), viewModel.uiState.value)
    }

    @Test
    fun retryCancelsPreviousObservationAndClearingViewModelCleansUp() = runTest(dispatcher) {
        var activeCollectors = 0
        var collections = 0
        var cancellations = 0
        val viewModel = createViewModel(flow {
            collections++
            activeCollectors++
            try {
                emit(itinerary)
                awaitCancellation()
            } finally {
                activeCollectors--
                cancellations++
            }
        })
        runCurrent()
        assertEquals(1, activeCollectors)

        viewModel.retry()
        runCurrent()
        assertEquals(2, collections)
        assertEquals(1, cancellations)
        assertEquals(1, activeCollectors)
        assertEquals(ItineraryUiState.Success(itinerary), viewModel.uiState.value)

        viewModelStore.clear()
        runCurrent()
        assertEquals(2, cancellations)
        assertEquals(0, activeCollectors)
    }

    @Test
    fun invalidDayNumbersDoNotSave() = runTest(dispatcher) {
        var saves = 0
        val viewModel = createViewModel(flowOf(null)) { _, _ -> saves++ }

        listOf("", "0", "-1", "abc", "2147483648").forEach { day ->
            viewModel.saveActivity(day, "Beach visit", "", "", "")
            runCurrent()
            assertEquals(
                ItinerarySaveState.Error("Day number must be at least 1."),
                viewModel.saveState.value
            )
        }
        assertEquals(0, saves)
    }

    @Test
    fun blankActivityNameDoesNotSave() = runTest(dispatcher) {
        var saves = 0
        val viewModel = createViewModel(flowOf(null)) { _, _ -> saves++ }

        viewModel.saveActivity("1", "   ", "", "", "")
        runCurrent()

        assertEquals(
            ItinerarySaveState.Error("Activity name cannot be empty."),
            viewModel.saveState.value
        )
        assertEquals(0, saves)
    }

    @Test
    fun invalidTimesDoNotSave() = runTest(dispatcher) {
        var saves = 0
        val viewModel = createViewModel(flowOf(null)) { _, _ -> saves++ }

        listOf("24:00", "18:60", "8:00", "sunset").forEach { time ->
            viewModel.saveActivity("1", "Beach visit", time, "", "")
            runCurrent()
            assertEquals(
                ItinerarySaveState.Error(
                    "Enter time in 24-hour HH:mm format, for example 18:00."
                ),
                viewModel.saveState.value
            )
        }
        assertEquals(0, saves)
    }

    @Test
    fun savesTrimmedFieldsAndShowsSuccess() = runTest(dispatcher) {
        var savedDay: Int? = null
        var savedActivity: ItineraryActivity? = null
        val viewModel = createViewModel(flowOf(null)) { day, activity ->
            savedDay = day
            savedActivity = activity
        }
        assertEquals(ItinerarySaveState.Idle, viewModel.saveState.value)

        viewModel.saveActivity(" 2 ", " Beach visit ", " 18:00 ", " Goa ", " Bring water ")
        assertEquals(ItinerarySaveState.Saving, viewModel.saveState.value)
        runCurrent()

        assertEquals(2, savedDay)
        assertNotNull(savedActivity)
        assertEquals("Beach visit", savedActivity?.title)
        assertEquals("18:00", savedActivity?.startTime)
        assertEquals("Goa", savedActivity?.location)
        assertEquals("Bring water", savedActivity?.notes)
        assertEquals(ItinerarySaveState.Success, viewModel.saveState.value)
        viewModel.resetSaveState()
        assertEquals(ItinerarySaveState.Idle, viewModel.saveState.value)
    }

    @Test
    fun blankOptionalFieldsAreStoredAsNull() = runTest(dispatcher) {
        var savedActivity: ItineraryActivity? = null
        val viewModel = createViewModel(flowOf(null)) { _, activity -> savedActivity = activity }

        viewModel.saveActivity("1", "Explore", " ", " ", " ")
        runCurrent()

        assertNotNull(savedActivity)
        assertNull(savedActivity?.startTime)
        assertNull(savedActivity?.location)
        assertNull(savedActivity?.notes)
        assertEquals(ItinerarySaveState.Success, viewModel.saveState.value)
    }

    @Test
    fun acceptsMidnightAndLastMinuteOfDay() = runTest(dispatcher) {
        val savedTimes = mutableListOf<String?>()
        val viewModel = createViewModel(flowOf(null)) { _, activity ->
            savedTimes.add(activity.startTime)
        }

        listOf("00:00", "23:59").forEach { time ->
            viewModel.saveActivity("1", "Explore", time, "", "")
            runCurrent()
            assertEquals(ItinerarySaveState.Success, viewModel.saveState.value)
        }
        assertEquals(listOf("00:00", "23:59"), savedTimes)
    }

    @Test
    fun saveFailureShowsErrorAndCanBeRetried() = runTest(dispatcher) {
        var attempts = 0
        val viewModel = createViewModel(flowOf(null)) { _, _ ->
            attempts++
            if (attempts == 1) throw IllegalStateException("Connection failed")
        }

        viewModel.saveActivity("1", "Explore", "", "", "")
        runCurrent()
        assertEquals(
            ItinerarySaveState.Error("Connection failed"),
            viewModel.saveState.value
        )

        viewModel.saveActivity("1", "Explore", "", "", "")
        runCurrent()
        assertEquals(2, attempts)
        assertEquals(ItinerarySaveState.Success, viewModel.saveState.value)
    }

    @Test
    fun saveFailureWithoutMessageShowsHelpfulError() = runTest(dispatcher) {
        val viewModel = createViewModel(flowOf(null)) { _, _ -> throw IllegalStateException() }

        viewModel.saveActivity("1", "Explore", "", "", "")
        runCurrent()

        assertEquals(
            ItinerarySaveState.Error("Activity could not be saved. Please try again."),
            viewModel.saveState.value
        )
    }

    @Test
    fun repeatedSaveAndResetWhileSavingDoNotStartAnotherWrite() = runTest(dispatcher) {
        var saves = 0
        val completion = CompletableDeferred<Unit>()
        val viewModel = createViewModel(flowOf(null)) { _, _ ->
            saves++
            completion.await()
        }

        viewModel.saveActivity("1", "Explore", "", "", "")
        runCurrent()
        viewModel.resetSaveState()
        viewModel.saveActivity("1", "Explore", "", "", "")
        runCurrent()
        assertEquals(1, saves)
        assertEquals(ItinerarySaveState.Saving, viewModel.saveState.value)

        completion.complete(Unit)
        runCurrent()
        assertEquals(ItinerarySaveState.Success, viewModel.saveState.value)
    }

    @Test
    fun savedActivityAppearsThroughObservationWithoutManualReload() = runTest(dispatcher) {
        val updates = MutableStateFlow<Itinerary?>(null)
        val viewModel = createViewModel(updates) { day, activity ->
            updates.value = (updates.value ?: Itinerary(tripId = "trip-1"))
                .withActivity(day, activity)
        }
        runCurrent()
        assertEquals(ItineraryUiState.Empty, viewModel.uiState.value)

        viewModel.saveActivity("1", "Explore", "", "", "")
        runCurrent()

        assertEquals(ItinerarySaveState.Success, viewModel.saveState.value)
        assertEquals(ItineraryUiState.Success(updates.value!!), viewModel.uiState.value)
        assertEquals("Explore", updates.value?.days?.single()?.activities?.single()?.title)
    }

    @Test
    fun clearingViewModelCancelsSaveWithoutReportingAnError() = runTest(dispatcher) {
        var cancelled = false
        val viewModel = createViewModel(flowOf(null)) { _, _ ->
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
        viewModel.saveActivity("1", "Explore", "", "", "")
        runCurrent()

        viewModelStore.clear()
        runCurrent()

        assertEquals(true, cancelled)
        assertEquals(ItinerarySaveState.Saving, viewModel.saveState.value)
    }
}
