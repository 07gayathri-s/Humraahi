package com.humraahi.ui.itinerary

import androidx.lifecycle.ViewModelStore
import com.humraahi.model.Itinerary
import com.humraahi.model.ItineraryDay
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

    private fun createViewModel(updates: Flow<Itinerary?>): ItineraryViewModel =
        ItineraryViewModel(updates).also { viewModelStore.put("itinerary", it) }

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
}
