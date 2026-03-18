package org.kalpeshbkundanani.burnmate.presentation.logging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.kalpeshbkundanani.burnmate.logging.domain.CalorieEntryFactory
import org.kalpeshbkundanani.burnmate.logging.model.CalorieAmount
import org.kalpeshbkundanani.burnmate.logging.model.CalorieEntry
import org.kalpeshbkundanani.burnmate.logging.model.EntryDate
import org.kalpeshbkundanani.burnmate.logging.model.EntryId
import org.kalpeshbkundanani.burnmate.logging.repository.EntryRepository
import org.kalpeshbkundanani.burnmate.presentation.shared.LoadableUiState

class DailyLoggingViewModelTest {

    @Test
    fun `t04 loads entries for the selected date in descending creation order`() {
        val selectedDate = LocalDate(2026, 3, 16)
        val repository = FakeEntryRepository(
            entriesByDate = mutableMapOf(
                selectedDate to mutableListOf(
                    calorieEntry("older", selectedDate, 400, "2026-03-16T08:00:00Z"),
                    calorieEntry("newer", selectedDate, -250, "2026-03-16T10:00:00Z")
                )
            )
        )
        val viewModel = DailyLoggingViewModel(
            repository = repository,
            factory = FakeCalorieEntryFactory(),
            initialDate = selectedDate
        )

        val state = viewModel.uiState.value

        assertEquals(LoadableUiState.Content, state.status)
        assertEquals(listOf("newer", "older"), state.entries.map { it.id })
        assertTrue(state.entries.first().isBurn)
        assertEquals("-250 kcal", state.entries.first().formattedCalories)
    }

    @Test
    fun `t05 add intake refreshes entries and clears the draft`() {
        val selectedDate = LocalDate(2026, 3, 16)
        val repository = FakeEntryRepository()
        val viewModel = DailyLoggingViewModel(
            repository = repository,
            factory = FakeCalorieEntryFactory(),
            initialDate = selectedDate
        )

        viewModel.onEvent(DailyLoggingEvent.CalorieInputChanged("500"))
        viewModel.onEvent(DailyLoggingEvent.AddIntakeTapped)

        val state = viewModel.uiState.value

        assertEquals(LoadableUiState.Content, state.status)
        assertEquals("", state.entryDraft.amountInput)
        assertFalse(state.entryDraft.hasError)
        assertEquals(1, state.entries.size)
        assertEquals("+500 kcal", state.entries.single().formattedCalories)
    }

    @Test
    fun `t06 add burn saves negative calorie entry and refreshes state`() {
        val selectedDate = LocalDate(2026, 3, 16)
        val repository = FakeEntryRepository()
        val viewModel = DailyLoggingViewModel(
            repository = repository,
            factory = FakeCalorieEntryFactory(),
            initialDate = selectedDate
        )

        viewModel.onEvent(DailyLoggingEvent.CalorieInputChanged("300"))
        viewModel.onEvent(DailyLoggingEvent.AddBurnTapped)

        val state = viewModel.uiState.value

        assertFalse(state.entryDraft.hasError)
        assertEquals("", state.entryDraft.amountInput)
        assertEquals(LoadableUiState.Content, state.status)
        assertEquals("-300 kcal", state.entries.single().formattedCalories)
    }

    @Test
    fun `t07 delete entry removes it after refresh`() {
        val selectedDate = LocalDate(2026, 3, 16)
        val repository = FakeEntryRepository(
            entriesByDate = mutableMapOf(
                selectedDate to mutableListOf(calorieEntry("delete-me", selectedDate, 200, "2026-03-16T08:00:00Z"))
            )
        )
        val viewModel = DailyLoggingViewModel(
            repository = repository,
            factory = FakeCalorieEntryFactory(),
            initialDate = selectedDate
        )

        viewModel.onEvent(DailyLoggingEvent.DeleteEntryTapped("delete-me"))

        val state = viewModel.uiState.value

        assertEquals(LoadableUiState.Empty, state.status)
        assertTrue(state.entries.isEmpty())
    }

    @Test
    fun `t08 and t09 date navigation reloads and empty state is explicit`() {
        val firstDate = LocalDate(2026, 3, 16)
        val previousDate = LocalDate(2026, 3, 15)
        val repository = FakeEntryRepository(
            entriesByDate = mutableMapOf(
                firstDate to mutableListOf(calorieEntry("existing", firstDate, 100, "2026-03-16T08:00:00Z")),
                previousDate to mutableListOf()
            )
        )
        val viewModel = DailyLoggingViewModel(
            repository = repository,
            factory = FakeCalorieEntryFactory(),
            initialDate = firstDate
        )

        viewModel.onEvent(DailyLoggingEvent.PreviousDayTapped)

        val state = viewModel.uiState.value

        assertEquals(previousDate, state.selectedDate)
        assertEquals(LoadableUiState.Empty, state.status)
        assertEquals("No activity logged.", state.emptyMessage?.message)
        assertEquals(listOf(firstDate, previousDate), repository.fetchRequests)
    }
}

private class FakeEntryRepository(
    private val entriesByDate: MutableMap<LocalDate, MutableList<CalorieEntry>> = mutableMapOf()
) : EntryRepository {
    val fetchRequests = mutableListOf<LocalDate>()

    override fun create(entry: CalorieEntry): Result<CalorieEntry> {
        entriesByDate.getOrPut(entry.date.value) { mutableListOf() }.add(entry)
        return Result.success(entry)
    }

    override fun deleteById(id: EntryId): Result<Boolean> {
        entriesByDate.values.forEach { entries ->
            val removed = entries.removeAll { it.id == id }
            if (removed) {
                return Result.success(true)
            }
        }
        return Result.success(false)
    }

    override fun fetchByDateRange(startDate: EntryDate, endDate: EntryDate): Result<List<CalorieEntry>> {
        return Result.success(emptyList())
    }

    override fun fetchByDate(date: EntryDate): Result<List<CalorieEntry>> {
        fetchRequests += date.value
        return Result.success(entriesByDate[date.value].orEmpty().toList())
    }
}

private class FakeCalorieEntryFactory : CalorieEntryFactory {
    override fun create(date: EntryDate, amount: CalorieAmount): Result<CalorieEntry> {
        return Result.success(
            calorieEntry(
                id = "generated-${amount.value}",
                date = date.value,
                amount = amount.value,
                createdAt = "2026-03-16T12:00:00Z"
            )
        )
    }
}

private fun calorieEntry(
    id: String,
    date: LocalDate,
    amount: Int,
    createdAt: String
): CalorieEntry {
    return CalorieEntry(
        id = EntryId(id),
        date = EntryDate(date),
        amount = CalorieAmount(amount),
        createdAt = Instant.parse(createdAt)
    )
}
