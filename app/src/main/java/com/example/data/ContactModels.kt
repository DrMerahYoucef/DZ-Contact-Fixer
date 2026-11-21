package com.example.data

enum class CleanupMode {
    /** Mode B (Default): Collapse spaced duplicates and pair clean +213 and 0X entries. */
    CLEAN_AND_NORMALIZE,

    /** Mode A: Safe append only — add missing unspaced +213 or 0X without deleting or modifying existing rows. */
    SAFE_APPEND_ONLY,
}

enum class SkipReason {
    FOREIGN_NUMBER,
    LANDLINE_OR_SHORT,
    UNRECOGNIZED,
}

sealed class NumberClassification {
    data class AlgerianMobile(
        val core9: String,
        val isInternational: Boolean,
        val isCleanUnspaced: Boolean,
    ) : NumberClassification()

    data class Ignored(
        val reason: SkipReason,
        val strippedValue: String,
    ) : NumberClassification()
}

data class RawPhoneEntry(
    val dataId: Long,
    val rawNumber: String,
    val type: Int,
    val label: String?,
)

data class RawContactRecord(
    val rawContactId: Long,
    val contactId: Long,
    val displayName: String,
    val accountType: String?,
    val accountName: String?,
    val isReadOnly: Boolean,
    val phoneEntries: List<RawPhoneEntry>,
)

data class WritableAccountTarget(
    val accountType: String?,
    val accountName: String?,
)

sealed class PlannedOperation {
    abstract val targetNumber: String

    data class InsertInternational(
        val core9: String,
        override val targetNumber: String,
    ) : PlannedOperation()

    data class InsertNationalLocal(
        val core9: String,
        override val targetNumber: String,
    ) : PlannedOperation()

    data class NormalizeExistingToClean(
        val dataId: Long,
        val originalNumber: String,
        override val targetNumber: String,
        val originalType: Int,
        val originalLabel: String?,
    ) : PlannedOperation()

    data class DeleteSpacedDuplicate(
        val dataId: Long,
        val originalNumber: String,
        override val targetNumber: String = originalNumber,
    ) : PlannedOperation()

    data class CreateWritableMirrorContact(
        val displayName: String,
        val targetAccount: WritableAccountTarget,
        val cores: List<String>,
        override val targetNumber: String = cores.firstOrNull()?.let { "+213$it" } ?: "",
    ) : PlannedOperation()

    fun cpoCount(): Int = when (this) {
        is CreateWritableMirrorContact -> 2 + (cores.size * 2)
        else -> 1
    }
}

data class UntouchedNumberEntry(
    val entry: RawPhoneEntry,
    val reason: SkipReason,
)

enum class ContactFixStatus {
    NEEDS_FIX,
    COMPLIANT,
    COVERED_BY_WRITABLE,
    SKIPPED_ONLY,
    READ_ONLY,
}

data class ContactFixPlan(
    val record: RawContactRecord,
    val operations: List<PlannedOperation>,
    val untouchedEntries: List<UntouchedNumberEntry>,
    val algerianCoresCount: Int,
    val missingNationalCount: Int,
    val missingInternationalCount: Int,
    val spacedCleanupCount: Int,
    val hasAddedLocalEntry: Boolean,
    val status: ContactFixStatus,
) {
    val rawContactId: Long get() = record.rawContactId
    val isModifiable: Boolean get() = status == ContactFixStatus.NEEDS_FIX && operations.isNotEmpty()
}

data class ScanSummary(
    val totalContactsScanned: Int = 0,
    val alreadyCompliantCount: Int = 0,
    val contactsNeedingFixCount: Int = 0,
    val missingNationalCount: Int = 0,
    val missingInternationalCount: Int = 0,
    val spacedDuplicatesCount: Int = 0,
    val ignoredNumbersCount: Int = 0,
    val addedLocalEntriesCount: Int = 0,
)

data class BackupResult(
    val vcfFileName: String,
    val jsonFileName: String,
    val contactsExported: Int,
    val timestampFormatted: String,
)

enum class BatchPhase {
    IDLE,
    EXPORTING_BACKUP,
    APPLYING_FIXES,
    REVERTING_LOCAL,
}

data class BatchProgress(
    val currentOps: Int = 0,
    val totalOps: Int = 0,
    val phase: BatchPhase = BatchPhase.IDLE,
) {
    val fraction: Float
        get() = if (totalOps <= 0) 0f else (currentOps.toFloat() / totalOps.toFloat()).coerceIn(0f, 1f)
}

data class BatchExecutionResult(
    val executedOperations: Int,
    val batchesExecuted: Int,
    val insertedInternational: Int,
    val insertedNationalLocal: Int,
    val normalizedInPlace: Int,
    val deletedDuplicates: Int,
)

enum class PreviewFilter {
    ALL_CHANGES,
    MISSING_0X,
    MISSING_213,
    SPACED_CLEANUPS,
    COMPLIANT,
    SKIPPED,
}
