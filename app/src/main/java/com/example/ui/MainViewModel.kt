package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AlgerianNumberNormalizer
import com.example.data.BackupResult
import com.example.data.BatchPhase
import com.example.data.BatchProgress
import com.example.data.CleanupMode
import com.example.data.ContactFixPlan
import com.example.data.ContactFixStatus
import com.example.data.ContactRepository
import com.example.data.PreviewFilter
import com.example.data.RawContactRecord
import com.example.data.ScanSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val hasContactsPermission: Boolean = false,
    val permissionPermanentlyDenied: Boolean = false,
    val isScanning: Boolean = false,
    val hasCompletedInitialScan: Boolean = false,
    val cleanupMode: CleanupMode = CleanupMode.CLEAN_AND_NORMALIZE,
    val activeFilter: PreviewFilter = PreviewFilter.ALL_CHANGES,
    val searchQuery: String = "",
    val summary: ScanSummary = ScanSummary(),
    val allPlans: List<ContactFixPlan> = emptyList(),
    val filteredPlans: List<ContactFixPlan> = emptyList(),
    val excludedRawContactIds: Set<Long> = emptySet(),
    val filterCounts: Map<PreviewFilter, Int> = emptyMap(),
    val selectedModifiableCount: Int = 0,
    val selectedOperationsCount: Int = 0,
    val batchProgress: BatchProgress = BatchProgress(),
    val lastBackupResult: BackupResult? = null,
    val showApplyConfirmation: Boolean = false,
    val showRevertConfirmation: Boolean = false,
    val errorMessage: String? = null,
)

sealed class MainUiEvent {
    data class ShowMessage(val message: String) : MainUiEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContactRepository(application.applicationContext)

    private var cachedRecords: List<RawContactRecord> = emptyList()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainUiEvent>()
    val events: SharedFlow<MainUiEvent> = _events.asSharedFlow()

    fun onPermissionStateUpdated(granted: Boolean, permanentlyDenied: Boolean = false) {
        val wasGranted = _uiState.value.hasContactsPermission
        _uiState.update {
            it.copy(
                hasContactsPermission = granted,
                permissionPermanentlyDenied = if (granted) false else permanentlyDenied,
            )
        }
        if (granted && (!wasGranted || !_uiState.value.hasCompletedInitialScan)) {
            scanContacts()
        }
    }

    fun scanContacts() {
        if (!_uiState.value.hasContactsPermission) return
        if (_uiState.value.isScanning || _uiState.value.batchProgress.phase != BatchPhase.IDLE) return

        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, errorMessage = null) }
            try {
                val records = repository.scanContacts()
                cachedRecords = records
                recomputePlans(records = records, resetExclusions = true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = e.localizedMessage ?: "Failed to scan contacts.",
                    )
                }
            }
        }
    }

    fun toggleCleanupMode(cleanAndNormalize: Boolean) {
        val newMode = if (cleanAndNormalize) {
            CleanupMode.CLEAN_AND_NORMALIZE
        } else {
            CleanupMode.SAFE_APPEND_ONLY
        }
        if (_uiState.value.cleanupMode == newMode) return

        viewModelScope.launch {
            _uiState.update { it.copy(cleanupMode = newMode) }
            recomputePlans(records = cachedRecords, resetExclusions = false)
        }
    }

    fun setFilter(filter: PreviewFilter) {
        _uiState.update { state ->
            val filtered = applyFilterAndSearch(state.allPlans, filter, state.searchQuery)
            state.copy(
                activeFilter = filter,
                filteredPlans = filtered,
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = applyFilterAndSearch(state.allPlans, state.activeFilter, query)
            state.copy(
                searchQuery = query,
                filteredPlans = filtered,
            )
        }
    }

    fun toggleContactSelection(rawContactId: Long) {
        _uiState.update { state ->
            val updatedExcluded = state.excludedRawContactIds.toMutableSet()
            if (updatedExcluded.contains(rawContactId)) {
                updatedExcluded.remove(rawContactId)
            } else {
                updatedExcluded.add(rawContactId)
            }
            val (selectedContacts, selectedOps) = countSelected(state.allPlans, updatedExcluded)
            state.copy(
                excludedRawContactIds = updatedExcluded,
                selectedModifiableCount = selectedContacts,
                selectedOperationsCount = selectedOps,
            )
        }
    }

    fun toggleSelectAllVisible() {
        _uiState.update { state ->
            val visibleModifiableIds = state.filteredPlans
                .filter { it.isModifiable }
                .map { it.rawContactId }
            if (visibleModifiableIds.isEmpty()) return@update state

            val allCurrentlySelected = visibleModifiableIds.all { !state.excludedRawContactIds.contains(it) }
            val updatedExcluded = state.excludedRawContactIds.toMutableSet()
            if (allCurrentlySelected) {
                updatedExcluded.addAll(visibleModifiableIds)
            } else {
                updatedExcluded.removeAll(visibleModifiableIds.toSet())
            }
            val (selectedContacts, selectedOps) = countSelected(state.allPlans, updatedExcluded)
            state.copy(
                excludedRawContactIds = updatedExcluded,
                selectedModifiableCount = selectedContacts,
                selectedOperationsCount = selectedOps,
            )
        }
    }

    fun backupContacts() {
        if (!_uiState.value.hasContactsPermission) return
        if (_uiState.value.batchProgress.phase != BatchPhase.IDLE) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    batchProgress = BatchProgress(0, 1, BatchPhase.EXPORTING_BACKUP),
                    errorMessage = null,
                )
            }
            try {
                val recordsToExport = if (cachedRecords.isNotEmpty()) {
                    cachedRecords
                } else {
                    repository.scanContacts().also { cachedRecords = it }
                }
                val result = repository.exportBackupToDownloads(recordsToExport)
                _uiState.update {
                    it.copy(
                        lastBackupResult = result,
                        batchProgress = BatchProgress(),
                    )
                }
                _events.emit(
                    MainUiEvent.ShowMessage(
                        "Backup saved to Downloads: ${result.vcfFileName} & ${result.jsonFileName}"
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(batchProgress = BatchProgress()) }
                _events.emit(
                    MainUiEvent.ShowMessage(
                        "Backup failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                )
            }
        }
    }

    fun requestApplyFixes() {
        if (_uiState.value.selectedModifiableCount == 0) return
        _uiState.update { it.copy(showApplyConfirmation = true) }
    }

    fun dismissApplyConfirmation() {
        _uiState.update { it.copy(showApplyConfirmation = false) }
    }

    fun confirmApplyFixes() {
        val currentState = _uiState.value
        val selectedPlans = currentState.allPlans.filter {
            it.isModifiable && !currentState.excludedRawContactIds.contains(it.rawContactId)
        }
        _uiState.update { it.copy(showApplyConfirmation = false) }
        if (selectedPlans.isEmpty()) return

        viewModelScope.launch {
            val totalOps = selectedPlans.sumOf { plan -> plan.operations.sumOf { it.cpoCount() } }
            _uiState.update {
                it.copy(
                    batchProgress = BatchProgress(0, totalOps, BatchPhase.APPLYING_FIXES),
                    errorMessage = null,
                )
            }
            try {
                val result = repository.applyPlans(selectedPlans) { done, total ->
                    _uiState.update {
                        it.copy(batchProgress = BatchProgress(done, total, BatchPhase.APPLYING_FIXES))
                    }
                }
                _uiState.update { it.copy(batchProgress = BatchProgress()) }
                _events.emit(
                    MainUiEvent.ShowMessage(
                        "Updated ${selectedPlans.size} contacts (${result.executedOperations} ops across ${result.batchesExecuted} batch(es))."
                    )
                )
                scanContacts()
            } catch (e: Exception) {
                _uiState.update { it.copy(batchProgress = BatchProgress()) }
                _events.emit(
                    MainUiEvent.ShowMessage(
                        "Update failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                )
            }
        }
    }

    fun requestRevertLocalNumbers() {
        if (_uiState.value.summary.addedLocalEntriesCount == 0) {
            viewModelScope.launch {
                _events.emit(MainUiEvent.ShowMessage("No \"DZ Local\" or legacy \"Toyota\" 0X numbers found to remove."))
            }
            return
        }
        _uiState.update { it.copy(showRevertConfirmation = true) }
    }

    fun dismissRevertConfirmation() {
        _uiState.update { it.copy(showRevertConfirmation = false) }
    }

    fun confirmRevertLocalNumbers() {
        val expectedCount = _uiState.value.summary.addedLocalEntriesCount
        _uiState.update { it.copy(showRevertConfirmation = false) }
        if (expectedCount == 0) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    batchProgress = BatchProgress(0, expectedCount, BatchPhase.REVERTING_LOCAL),
                    errorMessage = null,
                )
            }
            try {
                val result = repository.removeAddedLocalNumbers { done, total ->
                    _uiState.update {
                        it.copy(batchProgress = BatchProgress(done, total, BatchPhase.REVERTING_LOCAL))
                    }
                }
                _uiState.update { it.copy(batchProgress = BatchProgress()) }
                _events.emit(
                    MainUiEvent.ShowMessage(
                        "Removed ${result.deletedDuplicates} added Local (0X) number(s)."
                    )
                )
                scanContacts()
            } catch (e: Exception) {
                _uiState.update { it.copy(batchProgress = BatchProgress()) }
                _events.emit(
                    MainUiEvent.ShowMessage(
                        "Revert failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                )
            }
        }
    }

    private suspend fun recomputePlans(records: List<RawContactRecord>, resetExclusions: Boolean) {
        val mode = _uiState.value.cleanupMode
        val (plans, summary, filterCounts) = withContext(Dispatchers.Default) {
            val computedPlans = AlgerianNumberNormalizer.buildPlans(records, mode)
            val computedSummary = AlgerianNumberNormalizer.buildSummary(computedPlans)
            val counts = mapOf(
                PreviewFilter.ALL_CHANGES to computedPlans.count { it.isModifiable },
                PreviewFilter.MISSING_0X to computedPlans.count { it.isModifiable && it.missingNationalCount > 0 },
                PreviewFilter.MISSING_213 to computedPlans.count { it.isModifiable && it.missingInternationalCount > 0 },
                PreviewFilter.SPACED_CLEANUPS to computedPlans.count { it.isModifiable && it.spacedCleanupCount > 0 },
                PreviewFilter.COMPLIANT to computedPlans.count { it.status == ContactFixStatus.COMPLIANT },
                PreviewFilter.SKIPPED to computedPlans.count {
                    it.untouchedEntries.isNotEmpty() ||
                        it.status == ContactFixStatus.SKIPPED_ONLY ||
                        it.status == ContactFixStatus.READ_ONLY ||
                        it.status == ContactFixStatus.COVERED_BY_WRITABLE
                },
            )
            Triple(computedPlans, computedSummary, counts)
        }

        _uiState.update { state ->
            val excluded = if (resetExclusions) emptySet() else state.excludedRawContactIds
            val defaultFilter = if (
                resetExclusions &&
                filterCounts[PreviewFilter.ALL_CHANGES] == 0 &&
                (filterCounts[PreviewFilter.COMPLIANT] ?: 0) > 0
            ) {
                PreviewFilter.COMPLIANT
            } else {
                state.activeFilter
            }
            val filtered = applyFilterAndSearch(plans, defaultFilter, state.searchQuery)
            val (selectedContacts, selectedOps) = countSelected(plans, excluded)

            state.copy(
                isScanning = false,
                hasCompletedInitialScan = true,
                summary = summary,
                allPlans = plans,
                filteredPlans = filtered,
                excludedRawContactIds = excluded,
                filterCounts = filterCounts,
                activeFilter = defaultFilter,
                selectedModifiableCount = selectedContacts,
                selectedOperationsCount = selectedOps,
            )
        }
    }

    private fun applyFilterAndSearch(
        plans: List<ContactFixPlan>,
        filter: PreviewFilter,
        query: String,
    ): List<ContactFixPlan> {
        val trimmedQuery = query.trim().lowercase()
        val strippedDigitQuery = AlgerianNumberNormalizer.stripFormatting(trimmedQuery)

        return plans.asSequence()
            .filter { plan ->
                when (filter) {
                    PreviewFilter.ALL_CHANGES -> plan.isModifiable
                    PreviewFilter.MISSING_0X -> plan.isModifiable && plan.missingNationalCount > 0
                    PreviewFilter.MISSING_213 -> plan.isModifiable && plan.missingInternationalCount > 0
                    PreviewFilter.SPACED_CLEANUPS -> plan.isModifiable && plan.spacedCleanupCount > 0
                    PreviewFilter.COMPLIANT -> plan.status == ContactFixStatus.COMPLIANT
                    PreviewFilter.SKIPPED ->
                        plan.untouchedEntries.isNotEmpty() ||
                            plan.status == ContactFixStatus.SKIPPED_ONLY ||
                            plan.status == ContactFixStatus.READ_ONLY ||
                            plan.status == ContactFixStatus.COVERED_BY_WRITABLE
                }
            }
            .filter { plan ->
                if (trimmedQuery.isEmpty()) {
                    true
                } else {
                    plan.record.displayName.lowercase().contains(trimmedQuery) ||
                        plan.record.phoneEntries.any { entry ->
                            entry.rawNumber.lowercase().contains(trimmedQuery) ||
                                (strippedDigitQuery.isNotEmpty() &&
                                    AlgerianNumberNormalizer.stripFormatting(entry.rawNumber)
                                        .contains(strippedDigitQuery))
                        } ||
                        plan.operations.any { op ->
                            op.targetNumber.lowercase().contains(trimmedQuery)
                        }
                }
            }
            .toList()
    }

    private fun countSelected(
        plans: List<ContactFixPlan>,
        excludedIds: Set<Long>,
    ): Pair<Int, Int> {
        var contacts = 0
        var ops = 0
        for (plan in plans) {
            if (plan.isModifiable && !excludedIds.contains(plan.rawContactId)) {
                contacts++
                ops += plan.operations.sumOf { it.cpoCount() }
            }
        }
        return contacts to ops
    }
}
