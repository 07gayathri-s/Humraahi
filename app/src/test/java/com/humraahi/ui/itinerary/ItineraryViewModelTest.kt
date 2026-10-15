package com.humraahi.ui.itinerary

import androidx.lifecycle.ViewModelStore
import com.humraahi.model.Itinerary
import com.humraahi.model.ItineraryActivity
import com.humraahi.model.ItineraryDay
import com.humraahi.model.TripDateRange
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
        updateActivity: suspend (ActivityEditTarget, String, ItineraryActivity) -> Unit = { _, _, _ -> },
        removeActivity: suspend (ActivityEditTarget) -> Unit = {},
        addActivity: suspend (String, ItineraryActivity) -> Unit = { _, _ -> }
    ): ItineraryViewModel =
        ItineraryViewModel(updates, addActivity, updateActivity, removeActivity)
            .also { viewModelStore.put("itinerary", it) }

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
    fun invalidDatesDoNotSave() = runTest(dispatcher) {
        var saves = 0
        val viewModel = createViewModel(flowOf(null)) { _, _ -> saves++ }

        listOf("", "1", "abc", "2026-02-30", "2026-13-01", "2026-1-1").forEach { date ->
            viewModel.saveActivity(date, "Beach visit", "", "", "")
            runCurrent()
            assertEquals(
                ItinerarySaveState.Error("Choose a valid activity date."),
                viewModel.saveState.value
            )
        }
        assertEquals(0, saves)
    }

    @Test
    fun blankActivityNameDoesNotSave() = runTest(dispatcher) {
        var saves = 0
        val viewModel = createViewModel(flowOf(null)) { _, _ -> saves++ }

        viewModel.saveActivity("2026-10-20", "   ", "", "", "")
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
            viewModel.saveActivity("2026-10-20", "Beach visit", time, "", "")
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
        var savedDate: String? = null
        var savedActivity: ItineraryActivity? = null
        val viewModel = createViewModel(flowOf(null)) { date, activity ->
            savedDate = date
            savedActivity = activity
        }
        assertEquals(ItinerarySaveState.Idle, viewModel.saveState.value)

        viewModel.saveActivity("2026-10-21", " Beach visit ", " 18:00 ", " Goa ", " Bring water ")
        assertEquals(ItinerarySaveState.Saving, viewModel.saveState.value)
        runCurrent()

        assertEquals("2026-10-21", savedDate)
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

        viewModel.saveActivity("2026-10-20", "Explore", " ", " ", " ")
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
            viewModel.saveActivity("2026-10-20", "Explore", time, "", "")
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

        viewModel.saveActivity("2026-10-20", "Explore", "", "", "")
        runCurrent()
        assertEquals(
            ItinerarySaveState.Error("Connection failed"),
            viewModel.saveState.value
        )

        viewModel.saveActivity("2026-10-20", "Explore", "", "", "")
        runCurrent()
        assertEquals(2, attempts)
        assertEquals(ItinerarySaveState.Success, viewModel.saveState.value)
    }

    @Test
    fun saveFailureWithoutMessageShowsHelpfulError() = runTest(dispatcher) {
        val viewModel = createViewModel(flowOf(null)) { _, _ -> throw IllegalStateException() }

        viewModel.saveActivity("2026-10-20", "Explore", "", "", "")
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

        viewModel.saveActivity("2026-10-20", "Explore", "", "", "")
        runCurrent()
        viewModel.resetSaveState()
        viewModel.saveActivity("2026-10-20", "Explore", "", "", "")
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
        val viewModel = createViewModel(updates) { date, activity ->
            updates.value = (updates.value ?: Itinerary(tripId = "trip-1"))
                .withActivity(TripDateRange("2026-10-20", "2026-10-22").dayNumber(date), activity, date)
        }
        runCurrent()
        assertEquals(ItineraryUiState.Empty, viewModel.uiState.value)

        viewModel.saveActivity("2026-10-20", "Explore", "", "", "")
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
        viewModel.saveActivity("2026-10-20", "Explore", "", "", "")
        runCurrent()

        viewModelStore.clear()
        runCurrent()

        assertEquals(true, cancelled)
        assertEquals(ItinerarySaveState.Saving, viewModel.saveState.value)
    }

    @Test
    fun editingPreservesIdAndOriginalSnapshotAndDoesNotAddNewActivity() = runTest(dispatcher) {
        val original = ItineraryActivity(
            id = "activity-1", title = "Beach visit", startTime = "18:00", notes = "Bring water"
        )
        val target = ActivityEditTarget("day-1", "2026-10-20", original)
        var additions = 0
        var editedTarget: ActivityEditTarget? = null
        var editedDate: String? = null
        var editedActivity: ItineraryActivity? = null
        val viewModel = createViewModel(
            flowOf(itinerary),
            updateActivity = { selected, date, activity ->
                editedTarget = selected
                editedDate = date
                editedActivity = activity
            },
            addActivity = { _, _ -> additions++ }
        )

        viewModel.saveActivity("2026-10-21", " Sunset ", " 19:00 ", " Goa ", "", target)
        runCurrent()

        assertEquals(0, additions)
        assertEquals(target, editedTarget)
        assertEquals("2026-10-21", editedDate)
        assertEquals(
            original.copy(title = "Sunset", startTime = "19:00", location = "Goa", notes = null),
            editedActivity
        )
        assertEquals(ItinerarySaveState.Success, viewModel.saveState.value)
    }

    @Test
    fun invalidEditDoesNotWriteAndConflictIsShown() = runTest(dispatcher) {
        val target = ActivityEditTarget(
            "day-1", "2026-10-20", ItineraryActivity(id = "activity-1", title = "Beach visit")
        )
        var edits = 0
        val viewModel = createViewModel(
            flowOf(itinerary),
            updateActivity = { _, _, _ ->
                edits++
                throw IllegalStateException("Activity changed; reopen it.")
            }
        )

        viewModel.saveActivity("2026-10-20", " ", "", "", "", target)
        runCurrent()
        assertEquals(0, edits)
        viewModel.saveActivity("2026-10-21", "Sunset", "", "", "", target)
        runCurrent()

        assertEquals(1, edits)
        assertEquals(
            ItinerarySaveState.Error("Activity changed; reopen it."),
            viewModel.saveState.value
        )
    }

    private val deleteTarget = ActivityEditTarget(
        "day-1", "2026-10-20", ItineraryActivity(id = "activity-1", title = "Beach")
    )

    @Test
    fun deletionPassesOriginalSnapshotAndFinalDeletionShowsEmptyThroughObservation() = runTest(dispatcher) {
        val updates = MutableStateFlow<Itinerary?>(
            itinerary.copy(days = listOf(itinerary.days.single().copy(
                activities = listOf(deleteTarget.activity)
            )))
        )
        var deleted: ActivityEditTarget? = null
        val viewModel = createViewModel(
            updates,
            removeActivity = { target ->
                deleted = target
                updates.value = updates.value!!.withoutActivity(target.dayId, target.activity)
            },
            addActivity = { _, _ -> error("Deletion must not add an activity") },
            updateActivity = { _, _, _ -> error("Deletion must not edit an activity") }
        )
        runCurrent()

        viewModel.deleteActivity(deleteTarget)
        assertEquals(ItinerarySaveState.Saving, viewModel.saveState.value)
        runCurrent()

        assertEquals(deleteTarget, deleted)
        assertEquals(ItinerarySaveState.Success, viewModel.saveState.value)
        assertEquals(ItineraryUiState.Empty, viewModel.uiState.value)
        viewModel.resetSaveState()
        assertEquals(ItinerarySaveState.Idle, viewModel.saveState.value)
    }

    @Test
    fun deletionConflictIsShownAndCanBeRetried() = runTest(dispatcher) {
        var attempts = 0
        val viewModel = createViewModel(flowOf(itinerary), removeActivity = {
            attempts++
            if (attempts == 1) throw IllegalStateException("Activity changed; reopen it.")
        })

        viewModel.deleteActivity(deleteTarget)
        runCurrent()
        assertEquals(
            ItinerarySaveState.Error("Activity changed; reopen it."),
            viewModel.saveState.value
        )
        assertEquals(ItineraryUiState.Success(itinerary), viewModel.uiState.value)

        viewModel.deleteActivity(deleteTarget)
        runCurrent()
        assertEquals(2, attempts)
        assertEquals(ItinerarySaveState.Success, viewModel.saveState.value)
    }

    @Test
    fun deletionFailureWithoutMessageUsesDeleteSpecificError() = runTest(dispatcher) {
        val viewModel = createViewModel(flowOf(itinerary), removeActivity = {
            throw IllegalStateException()
        })

        viewModel.deleteActivity(deleteTarget)
        runCurrent()

        assertEquals(
            ItinerarySaveState.Error("Activity could not be deleted. Please try again."),
            viewModel.saveState.value
        )
    }

    @Test
    fun pendingDeletionBlocksDuplicateDeletesSavesAndReset() = runTest(dispatcher) {
        var deletions = 0
        val completion = CompletableDeferred<Unit>()
        val viewModel = createViewModel(
            flowOf(itinerary),
            removeActivity = { deletions++; completion.await() },
            addActivity = { _, _ -> error("Save must be blocked during deletion") }
        )

        viewModel.deleteActivity(deleteTarget)
        runCurrent()
        viewModel.deleteActivity(deleteTarget)
        viewModel.saveActivity("2026-10-20", "Explore", "", "", "")
        viewModel.resetSaveState()
        runCurrent()

        assertEquals(1, deletions)
        assertEquals(ItinerarySaveState.Saving, viewModel.saveState.value)
        completion.complete(Unit)
        runCurrent()
        assertEquals(ItinerarySaveState.Success, viewModel.saveState.value)
    }

    @Test
    fun pendingSaveBlocksDeletion() = runTest(dispatcher) {
        val completion = CompletableDeferred<Unit>()
        val viewModel = createViewModel(
            flowOf(itinerary),
            removeActivity = { error("Deletion must be blocked during save") },
            addActivity = { _, _ -> completion.await() }
        )

        viewModel.saveActivity("2026-10-20", "Explore", "", "", "")
        runCurrent()
        viewModel.deleteActivity(deleteTarget)
        runCurrent()
        assertEquals(ItinerarySaveState.Saving, viewModel.saveState.value)
        completion.complete(Unit)
        runCurrent()
        assertEquals(ItinerarySaveState.Success, viewModel.saveState.value)
    }

    @Test
    fun clearingViewModelCancelsDeletionWithoutReportingAnError() = runTest(dispatcher) {
        var cancelled = false
        val viewModel = createViewModel(flowOf(itinerary), removeActivity = {
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        })
        viewModel.deleteActivity(deleteTarget)
        runCurrent()

        viewModelStore.clear()
        runCurrent()

        assertEquals(true, cancelled)
        assertEquals(ItinerarySaveState.Saving, viewModel.saveState.value)
    }
}
