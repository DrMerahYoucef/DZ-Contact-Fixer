package com.example.data

import android.provider.ContactsContract.CommonDataKinds.Phone
import org.json.JSONArray
import org.json.JSONObject

object AlgerianNumberNormalizer {

    const val LOCAL_CUSTOM_LABEL = "DZ Local"
    const val LEGACY_TOYOTA_LABEL = "Toyota"
    val REVERTIBLE_CUSTOM_LABELS: Set<String> = setOf(LOCAL_CUSTOM_LABEL, LEGACY_TOYOTA_LABEL)

    private val STRIP_REGEX = Regex("[\\s\\-().\\u00A0\\u200B\\u200E\\u200F\\u202A\\u202C]+")
    private val INTL_MOBILE_REGEX = Regex("^\\+213([567]\\d{8})$")
    private val NAT_MOBILE_REGEX = Regex("^0([567]\\d{8})$")

    fun stripFormatting(raw: String): String = raw.replace(STRIP_REGEX, "")

    fun classify(rawNumber: String): NumberClassification {
        val stripped = stripFormatting(rawNumber)
        INTL_MOBILE_REGEX.matchEntire(stripped)?.let { match ->
            val core9 = match.groupValues[1]
            return NumberClassification.AlgerianMobile(
                core9 = core9,
                isInternational = true,
                isCleanUnspaced = (rawNumber == "+213$core9"),
            )
        }
        NAT_MOBILE_REGEX.matchEntire(stripped)?.let { match ->
            val core9 = match.groupValues[1]
            return NumberClassification.AlgerianMobile(
                core9 = core9,
                isInternational = false,
                isCleanUnspaced = (rawNumber == "0$core9"),
            )
        }
        val reason = when {
            stripped.startsWith("+") && !stripped.startsWith("+213") -> SkipReason.FOREIGN_NUMBER
            stripped.startsWith("+213") ||
                stripped.startsWith("0") ||
                (stripped.isNotEmpty() && stripped.all { it.isDigit() }) -> SkipReason.LANDLINE_OR_SHORT
            else -> SkipReason.UNRECOGNIZED
        }
        return NumberClassification.Ignored(reason = reason, strippedValue = stripped)
    }

    fun isAddedLocalEntry(entry: RawPhoneEntry): Boolean {
        if (entry.type != Phone.TYPE_CUSTOM || entry.label !in REVERTIBLE_CUSTOM_LABELS) {
            return false
        }
        val stripped = stripFormatting(entry.rawNumber)
        return NAT_MOBILE_REGEX.matches(stripped)
    }

    fun detectPrimaryWritableAccount(records: List<RawContactRecord>): WritableAccountTarget {
        val writableRecords = records.filter { !it.isReadOnly }
        if (writableRecords.isEmpty()) {
            return WritableAccountTarget(accountType = null, accountName = null)
        }
        val counts = writableRecords.groupingBy {
            WritableAccountTarget(it.accountType, it.accountName)
        }.eachCount()

        val bestGoogle = counts.entries
            .filter { it.key.accountType == "com.google" }
            .maxByOrNull { it.value }
            ?.key
        if (bestGoogle != null) return bestGoogle

        return counts.maxByOrNull { it.value }?.key
            ?: WritableAccountTarget(accountType = null, accountName = null)
    }

    fun buildPlans(records: List<RawContactRecord>, mode: CleanupMode): List<ContactFixPlan> {
        val contactIdsWithWritable = records
            .asSequence()
            .filter { !it.isReadOnly && it.contactId != -1L }
            .map { it.contactId }
            .toHashSet()
        val primaryWritableAccount = detectPrimaryWritableAccount(records)

        return records.map { record ->
            buildPlanForContact(
                record = record,
                mode = mode,
                hasLinkedWritableContact = record.contactId != -1L && contactIdsWithWritable.contains(record.contactId),
                primaryWritableAccount = primaryWritableAccount,
            )
        }
    }

    fun buildPlanForContact(
        record: RawContactRecord,
        mode: CleanupMode,
        hasLinkedWritableContact: Boolean = false,
        primaryWritableAccount: WritableAccountTarget = WritableAccountTarget(null, null),
    ): ContactFixPlan {
        val entriesByCore = LinkedHashMap<String, MutableList<Pair<RawPhoneEntry, NumberClassification.AlgerianMobile>>>()
        val untouched = mutableListOf<UntouchedNumberEntry>()
        var hasAddedLocal = false

        for (entry in record.phoneEntries) {
            if (isAddedLocalEntry(entry)) {
                hasAddedLocal = true
            }
            when (val classification = classify(entry.rawNumber)) {
                is NumberClassification.AlgerianMobile -> {
                    entriesByCore.getOrPut(classification.core9) { mutableListOf() }
                        .add(entry to classification)
                }
                is NumberClassification.Ignored -> {
                    untouched.add(UntouchedNumberEntry(entry = entry, reason = classification.reason))
                }
            }
        }

        if (entriesByCore.isEmpty()) {
            return ContactFixPlan(
                record = record,
                operations = emptyList(),
                untouchedEntries = untouched,
                algerianCoresCount = 0,
                missingNationalCount = 0,
                missingInternationalCount = 0,
                spacedCleanupCount = 0,
                hasAddedLocalEntry = hasAddedLocal,
                status = ContactFixStatus.SKIPPED_ONLY,
            )
        }

        if (record.isReadOnly) {
            if (hasLinkedWritableContact) {
                return ContactFixPlan(
                    record = record,
                    operations = emptyList(),
                    untouchedEntries = untouched,
                    algerianCoresCount = entriesByCore.size,
                    missingNationalCount = 0,
                    missingInternationalCount = 0,
                    spacedCleanupCount = 0,
                    hasAddedLocalEntry = hasAddedLocal,
                    status = ContactFixStatus.COVERED_BY_WRITABLE,
                )
            }
            val cores = entriesByCore.keys.toList()
            return ContactFixPlan(
                record = record,
                operations = listOf(
                    PlannedOperation.CreateWritableMirrorContact(
                        displayName = record.displayName,
                        targetAccount = primaryWritableAccount,
                        cores = cores,
                    )
                ),
                untouchedEntries = untouched,
                algerianCoresCount = cores.size,
                missingNationalCount = cores.size,
                missingInternationalCount = cores.size,
                spacedCleanupCount = 0,
                hasAddedLocalEntry = hasAddedLocal,
                status = ContactFixStatus.NEEDS_FIX,
            )
        }

        val operations = mutableListOf<PlannedOperation>()
        var missingNational = 0
        var missingInternational = 0
        var spacedCleanups = 0

        for ((core9, items) in entriesByCore) {
            val intlTarget = "+213$core9"
            val natTarget = "0$core9"

            val intlItems = items.filter { it.second.isInternational }
            val natItems = items.filter { !it.second.isInternational }

            when (mode) {
                CleanupMode.CLEAN_AND_NORMALIZE -> {
                    // International side (+213XXXXXXXXX)
                    val cleanIntl = intlItems.filter { it.second.isCleanUnspaced }
                    val dirtyIntl = intlItems.filter { !it.second.isCleanUnspaced }

                    when {
                        cleanIntl.isNotEmpty() -> {
                            // Keep first clean entry; delete redundant extra clean or spaced duplicates
                            cleanIntl.drop(1).forEach { (entry, _) ->
                                operations.add(
                                    PlannedOperation.DeleteSpacedDuplicate(
                                        dataId = entry.dataId,
                                        originalNumber = entry.rawNumber,
                                    )
                                )
                                spacedCleanups++
                            }
                            dirtyIntl.forEach { (entry, _) ->
                                operations.add(
                                    PlannedOperation.DeleteSpacedDuplicate(
                                        dataId = entry.dataId,
                                        originalNumber = entry.rawNumber,
                                    )
                                )
                                spacedCleanups++
                            }
                        }
                        dirtyIntl.isNotEmpty() -> {
                            // Normalize the first spaced international entry in place
                            val primary = dirtyIntl.first().first
                            operations.add(
                                PlannedOperation.NormalizeExistingToClean(
                                    dataId = primary.dataId,
                                    originalNumber = primary.rawNumber,
                                    targetNumber = intlTarget,
                                    originalType = primary.type,
                                    originalLabel = primary.label,
                                )
                            )
                            spacedCleanups++
                            dirtyIntl.drop(1).forEach { (entry, _) ->
                                operations.add(
                                    PlannedOperation.DeleteSpacedDuplicate(
                                        dataId = entry.dataId,
                                        originalNumber = entry.rawNumber,
                                    )
                                )
                                spacedCleanups++
                            }
                        }
                        else -> {
                            operations.add(
                                PlannedOperation.InsertInternational(
                                    core9 = core9,
                                    targetNumber = intlTarget,
                                )
                            )
                            missingInternational++
                        }
                    }

                    // National side (0XXXXXXXXX)
                    val cleanNat = natItems.filter { it.second.isCleanUnspaced }
                    val dirtyNat = natItems.filter { !it.second.isCleanUnspaced }

                    when {
                        cleanNat.isNotEmpty() -> {
                            cleanNat.drop(1).forEach { (entry, _) ->
                                operations.add(
                                    PlannedOperation.DeleteSpacedDuplicate(
                                        dataId = entry.dataId,
                                        originalNumber = entry.rawNumber,
                                    )
                                )
                                spacedCleanups++
                            }
                            dirtyNat.forEach { (entry, _) ->
                                operations.add(
                                    PlannedOperation.DeleteSpacedDuplicate(
                                        dataId = entry.dataId,
                                        originalNumber = entry.rawNumber,
                                    )
                                )
                                spacedCleanups++
                            }
                        }
                        dirtyNat.isNotEmpty() -> {
                            val primary = dirtyNat.first().first
                            operations.add(
                                PlannedOperation.NormalizeExistingToClean(
                                    dataId = primary.dataId,
                                    originalNumber = primary.rawNumber,
                                    targetNumber = natTarget,
                                    originalType = primary.type,
                                    originalLabel = primary.label,
                                )
                            )
                            spacedCleanups++
                            dirtyNat.drop(1).forEach { (entry, _) ->
                                operations.add(
                                    PlannedOperation.DeleteSpacedDuplicate(
                                        dataId = entry.dataId,
                                        originalNumber = entry.rawNumber,
                                    )
                                )
                                spacedCleanups++
                            }
                        }
                        else -> {
                            operations.add(
                                PlannedOperation.InsertNationalLocal(
                                    core9 = core9,
                                    targetNumber = natTarget,
                                )
                            )
                            missingNational++
                        }
                    }
                }

                CleanupMode.SAFE_APPEND_ONLY -> {
                    val hasCleanIntl = intlItems.any { it.second.isCleanUnspaced }
                    val hasCleanNat = natItems.any { it.second.isCleanUnspaced }

                    if (!hasCleanIntl) {
                        operations.add(
                            PlannedOperation.InsertInternational(
                                core9 = core9,
                                targetNumber = intlTarget,
                            )
                        )
                        missingInternational++
                    }
                    if (!hasCleanNat) {
                        operations.add(
                            PlannedOperation.InsertNationalLocal(
                                core9 = core9,
                                targetNumber = natTarget,
                            )
                        )
                        missingNational++
                    }
                }
            }
        }

        val status = when {
            operations.isEmpty() -> ContactFixStatus.COMPLIANT
            record.isReadOnly -> ContactFixStatus.READ_ONLY
            else -> ContactFixStatus.NEEDS_FIX
        }

        return ContactFixPlan(
            record = record,
            operations = if (record.isReadOnly) emptyList() else operations,
            untouchedEntries = untouched,
            algerianCoresCount = entriesByCore.size,
            missingNationalCount = missingNational,
            missingInternationalCount = missingInternational,
            spacedCleanupCount = if (record.isReadOnly) 0 else spacedCleanups,
            hasAddedLocalEntry = hasAddedLocal,
            status = status,
        )
    }

    fun buildSummary(plans: List<ContactFixPlan>): ScanSummary {
        var compliant = 0
        var needingFix = 0
        var missingNationalContacts = 0
        var missingInternationalContacts = 0
        var spacedCleanupContacts = 0
        var ignoredNumbers = 0
        var addedLocalEntries = 0

        for (plan in plans) {
            ignoredNumbers += plan.untouchedEntries.size
            addedLocalEntries += plan.record.phoneEntries.count { isAddedLocalEntry(it) }

            when (plan.status) {
                ContactFixStatus.COMPLIANT -> compliant++
                ContactFixStatus.NEEDS_FIX -> {
                    needingFix++
                    if (plan.missingNationalCount > 0) missingNationalContacts++
                    if (plan.missingInternationalCount > 0) missingInternationalContacts++
                    if (plan.spacedCleanupCount > 0) spacedCleanupContacts++
                }
                ContactFixStatus.COVERED_BY_WRITABLE,
                ContactFixStatus.SKIPPED_ONLY,
                ContactFixStatus.READ_ONLY -> Unit
            }
        }

        return ScanSummary(
            totalContactsScanned = plans.size,
            alreadyCompliantCount = compliant,
            contactsNeedingFixCount = needingFix,
            missingNationalCount = missingNationalContacts,
            missingInternationalCount = missingInternationalContacts,
            spacedDuplicatesCount = spacedCleanupContacts,
            ignoredNumbersCount = ignoredNumbers,
            addedLocalEntriesCount = addedLocalEntries,
        )
    }

    fun buildBackupVcf(records: List<RawContactRecord>): String {
        val sb = StringBuilder()
        for (record in records) {
            sb.append("BEGIN:VCARD\r\n")
            sb.append("VERSION:3.0\r\n")
            val escapedName = escapeVCardValue(record.displayName)
            sb.append("FN:").append(escapedName).append("\r\n")
            sb.append("N:;").append(escapedName).append(";;;\r\n")
            sb.append("X-RAW-CONTACT-ID:").append(record.rawContactId).append("\r\n")
            record.accountType?.let {
                sb.append("X-ACCOUNT-TYPE:").append(escapeVCardValue(it)).append("\r\n")
            }
            for (phone in record.phoneEntries) {
                val typeParam = when (phone.type) {
                    Phone.TYPE_HOME -> "HOME"
                    Phone.TYPE_WORK -> "WORK"
                    Phone.TYPE_MOBILE -> "CELL"
                    Phone.TYPE_CUSTOM -> phone.label?.takeIf { it.isNotBlank() }?.uppercase() ?: "OTHER"
                    else -> "VOICE"
                }
                sb.append("TEL;TYPE=").append(typeParam).append(":")
                    .append(phone.rawNumber.trim()).append("\r\n")
            }
            sb.append("END:VCARD\r\n")
        }
        return sb.toString()
    }

    fun buildBackupJson(records: List<RawContactRecord>, exportedAtIso: String): String {
        val root = JSONObject()
        root.put("app", "DZ Contact Fixer")
        root.put("exportedAt", exportedAtIso)
        root.put("rawContactsCount", records.size)

        val contactsArray = JSONArray()
        for (record in records) {
            val contactObj = JSONObject()
            contactObj.put("rawContactId", record.rawContactId)
            contactObj.put("contactId", record.contactId)
            contactObj.put("displayName", record.displayName)
            contactObj.put("accountType", record.accountType ?: JSONObject.NULL)
            contactObj.put("accountName", record.accountName ?: JSONObject.NULL)
            contactObj.put("isReadOnly", record.isReadOnly)

            val phonesArray = JSONArray()
            for (phone in record.phoneEntries) {
                val phoneObj = JSONObject()
                phoneObj.put("dataId", phone.dataId)
                phoneObj.put("number", phone.rawNumber)
                phoneObj.put("type", phone.type)
                phoneObj.put("label", phone.label ?: JSONObject.NULL)
                phonesArray.put(phoneObj)
            }
            contactObj.put("phones", phonesArray)
            contactsArray.put(contactObj)
        }
        root.put("contacts", contactsArray)
        return root.toString(2)
    }

    private fun escapeVCardValue(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "")
            .replace(";", "\\;")
            .replace(",", "\\,")
}
