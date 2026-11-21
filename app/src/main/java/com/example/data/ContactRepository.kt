package com.example.data

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ContactRepository(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    companion object {
        const val MAX_BATCH_SIZE = 400
    }

    private data class RawContactMeta(
        val rawContactId: Long,
        val contactId: Long,
        val displayName: String,
        val accountType: String?,
        val accountName: String?,
        val isReadOnly: Boolean,
    )

    suspend fun scanContacts(): List<RawContactRecord> = withContext(Dispatchers.IO) {
        val rawMetaMap = LinkedHashMap<Long, RawContactMeta>()

        val rawProjection = arrayOf(
            RawContacts._ID,
            RawContacts.CONTACT_ID,
            RawContacts.DISPLAY_NAME_PRIMARY,
            RawContacts.ACCOUNT_TYPE,
            RawContacts.ACCOUNT_NAME,
        )

        contentResolver.query(
            RawContacts.CONTENT_URI,
            rawProjection,
            "${RawContacts.DELETED} = 0",
            null,
            null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(RawContacts._ID)
            val contactIdIdx = cursor.getColumnIndex(RawContacts.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(RawContacts.DISPLAY_NAME_PRIMARY)
            val accTypeIdx = cursor.getColumnIndex(RawContacts.ACCOUNT_TYPE)
            val accNameIdx = cursor.getColumnIndex(RawContacts.ACCOUNT_NAME)

            while (cursor.moveToNext()) {
                if (idIdx < 0) continue
                val rawId = cursor.getLong(idIdx)
                val contactId = if (contactIdIdx >= 0 && !cursor.isNull(contactIdIdx)) {
                    cursor.getLong(contactIdIdx)
                } else {
                    -1L
                }
                val displayName = if (nameIdx >= 0) {
                    cursor.getString(nameIdx)?.takeIf { it.isNotBlank() } ?: "Unnamed Contact"
                } else {
                    "Unnamed Contact"
                }
                val accountType = if (accTypeIdx >= 0) cursor.getString(accTypeIdx) else null
                val accountName = if (accNameIdx >= 0) cursor.getString(accNameIdx) else null
                val isSyncOnlyReadOnly = isReadOnlyAccountType(accountType)
                rawMetaMap[rawId] = RawContactMeta(
                    rawContactId = rawId,
                    contactId = contactId,
                    displayName = displayName,
                    accountType = accountType,
                    accountName = accountName,
                    isReadOnly = isSyncOnlyReadOnly,
                )
            }
        }

        val phonesByRawId = LinkedHashMap<Long, MutableList<RawPhoneEntry>>()
        val fallbackNames = HashMap<Long, String>()
        val fallbackContactIds = HashMap<Long, Long>()

        val phoneProjection = arrayOf(
            Data._ID,
            Data.RAW_CONTACT_ID,
            Data.CONTACT_ID,
            Data.DISPLAY_NAME,
            Phone.NUMBER,
            Phone.TYPE,
            Phone.LABEL,
        )

        contentResolver.query(
            Data.CONTENT_URI,
            phoneProjection,
            "${Data.MIMETYPE} = ?",
            arrayOf(Phone.CONTENT_ITEM_TYPE),
            "${Data.DISPLAY_NAME} COLLATE NOCASE ASC, ${Data._ID} ASC",
        )?.use { cursor ->
            val dataIdIdx = cursor.getColumnIndex(Data._ID)
            val rawIdIdx = cursor.getColumnIndex(Data.RAW_CONTACT_ID)
            val contactIdIdx = cursor.getColumnIndex(Data.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(Data.DISPLAY_NAME)
            val numberIdx = cursor.getColumnIndex(Phone.NUMBER)
            val typeIdx = cursor.getColumnIndex(Phone.TYPE)
            val labelIdx = cursor.getColumnIndex(Phone.LABEL)

            while (cursor.moveToNext()) {
                if (dataIdIdx < 0 || rawIdIdx < 0 || numberIdx < 0) continue
                val number = cursor.getString(numberIdx)?.takeIf { it.isNotBlank() } ?: continue
                val dataId = cursor.getLong(dataIdIdx)
                val rawId = cursor.getLong(rawIdIdx)
                val contactId = if (contactIdIdx >= 0 && !cursor.isNull(contactIdIdx)) {
                    cursor.getLong(contactIdIdx)
                } else {
                    -1L
                }
                val displayName = if (nameIdx >= 0) {
                    cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
                val type = if (typeIdx >= 0 && !cursor.isNull(typeIdx)) {
                    cursor.getInt(typeIdx)
                } else {
                    Phone.TYPE_OTHER
                }
                val label = if (labelIdx >= 0) cursor.getString(labelIdx) else null

                if (displayName != null && !fallbackNames.containsKey(rawId)) {
                    fallbackNames[rawId] = displayName
                }
                if (contactId != -1L) {
                    fallbackContactIds[rawId] = contactId
                }

                phonesByRawId.getOrPut(rawId) { mutableListOf() }.add(
                    RawPhoneEntry(
                        dataId = dataId,
                        rawNumber = number,
                        type = type,
                        label = label,
                    )
                )
            }
        }

        phonesByRawId.entries
            .filter { it.value.isNotEmpty() }
            .map { (rawId, phoneList) ->
                val meta = rawMetaMap[rawId]
                RawContactRecord(
                    rawContactId = rawId,
                    contactId = meta?.contactId ?: fallbackContactIds[rawId] ?: -1L,
                    displayName = meta?.displayName ?: fallbackNames[rawId] ?: "Unnamed Contact",
                    accountType = meta?.accountType,
                    accountName = meta?.accountName,
                    isReadOnly = meta?.isReadOnly ?: false,
                    phoneEntries = phoneList,
                )
            }
            .sortedBy { it.displayName.lowercase(Locale.getDefault()) }
    }

    suspend fun applyPlans(
        plans: List<ContactFixPlan>,
        onProgress: (completedOps: Int, totalOps: Int) -> Unit,
    ): BatchExecutionResult = withContext(Dispatchers.IO) {
        val batches = mutableListOf<ArrayList<ContentProviderOperation>>()
        var currentBatch = ArrayList<ContentProviderOperation>()
        var totalOps = 0
        var insertedIntl = 0
        var insertedNat = 0
        var normalized = 0
        var deleted = 0

        fun ensureBatchCapacity(neededOps: Int) {
            if (currentBatch.isNotEmpty() && currentBatch.size + neededOps > MAX_BATCH_SIZE) {
                batches.add(currentBatch)
                currentBatch = ArrayList()
            }
        }

        for (plan in plans) {
            if (!plan.isModifiable) continue
            for (op in plan.operations) {
                when (op) {
                    is PlannedOperation.InsertInternational -> {
                        ensureBatchCapacity(1)
                        currentBatch.add(
                            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                                .withValue(Data.RAW_CONTACT_ID, plan.rawContactId)
                                .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                                .withValue(Phone.NUMBER, op.targetNumber)
                                .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                                .build()
                        )
                        totalOps++
                        insertedIntl++
                    }

                    is PlannedOperation.InsertNationalLocal -> {
                        ensureBatchCapacity(1)
                        currentBatch.add(
                            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                                .withValue(Data.RAW_CONTACT_ID, plan.rawContactId)
                                .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                                .withValue(Phone.NUMBER, op.targetNumber)
                                .withValue(Phone.TYPE, Phone.TYPE_CUSTOM)
                                .withValue(Phone.LABEL, AlgerianNumberNormalizer.LOCAL_CUSTOM_LABEL)
                                .build()
                        )
                        totalOps++
                        insertedNat++
                    }

                    is PlannedOperation.NormalizeExistingToClean -> {
                        ensureBatchCapacity(1)
                        currentBatch.add(
                            ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                                .withSelection("${Data._ID} = ?", arrayOf(op.dataId.toString()))
                                .withValue(Phone.NUMBER, op.targetNumber)
                                .build()
                        )
                        totalOps++
                        normalized++
                    }

                    is PlannedOperation.DeleteSpacedDuplicate -> {
                        ensureBatchCapacity(1)
                        currentBatch.add(
                            ContentProviderOperation.newDelete(Data.CONTENT_URI)
                                .withSelection("${Data._ID} = ?", arrayOf(op.dataId.toString()))
                                .build()
                        )
                        totalOps++
                        deleted++
                    }

                    is PlannedOperation.CreateWritableMirrorContact -> {
                        val needed = op.cpoCount()
                        ensureBatchCapacity(needed)
                        val rawContactInsertIndex = currentBatch.size

                        currentBatch.add(
                            ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                                .withValue(RawContacts.ACCOUNT_TYPE, op.targetAccount.accountType)
                                .withValue(RawContacts.ACCOUNT_NAME, op.targetAccount.accountName)
                                .withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DEFAULT)
                                .build()
                        )
                        currentBatch.add(
                            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                                .withValue(StructuredName.DISPLAY_NAME, op.displayName)
                                .build()
                        )
                        for (core9 in op.cores) {
                            currentBatch.add(
                                ContentProviderOperation.newInsert(Data.CONTENT_URI)
                                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                                    .withValue(Phone.NUMBER, "+213$core9")
                                    .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                                    .build()
                            )
                            currentBatch.add(
                                ContentProviderOperation.newInsert(Data.CONTENT_URI)
                                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                                    .withValue(Phone.NUMBER, "0$core9")
                                    .withValue(Phone.TYPE, Phone.TYPE_CUSTOM)
                                    .withValue(Phone.LABEL, AlgerianNumberNormalizer.LOCAL_CUSTOM_LABEL)
                                    .build()
                            )
                            insertedIntl++
                            insertedNat++
                        }
                        totalOps += needed
                    }
                }
            }
        }

        if (currentBatch.isNotEmpty()) {
            batches.add(currentBatch)
        }

        onProgress(0, totalOps)
        if (totalOps == 0) {
            return@withContext BatchExecutionResult(0, 0, 0, 0, 0, 0)
        }

        var completed = 0
        var executedBatches = 0
        for (batch in batches) {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, batch)
            completed += batch.size
            executedBatches++
            onProgress(completed, totalOps)
        }

        BatchExecutionResult(
            executedOperations = completed,
            batchesExecuted = executedBatches,
            insertedInternational = insertedIntl,
            insertedNationalLocal = insertedNat,
            normalizedInPlace = normalized,
            deletedDuplicates = deleted,
        )
    }

    suspend fun removeAddedLocalNumbers(
        onProgress: (completedOps: Int, totalOps: Int) -> Unit,
    ): BatchExecutionResult = withContext(Dispatchers.IO) {
        val dataIdsToDelete = mutableListOf<Long>()
        val projection = arrayOf(Data._ID, Phone.NUMBER, Phone.TYPE, Phone.LABEL)
        val revertLabels = AlgerianNumberNormalizer.REVERTIBLE_CUSTOM_LABELS.toList()
        val labelPlaceholders = revertLabels.joinToString(",") { "?" }
        val selectionArgs = arrayOf(
            Phone.CONTENT_ITEM_TYPE,
            Phone.TYPE_CUSTOM.toString(),
            *revertLabels.toTypedArray(),
        )

        contentResolver.query(
            Data.CONTENT_URI,
            projection,
            "${Data.MIMETYPE} = ? AND ${Phone.TYPE} = ? AND ${Phone.LABEL} IN ($labelPlaceholders)",
            selectionArgs,
            null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(Data._ID)
            val numIdx = cursor.getColumnIndex(Phone.NUMBER)
            val typeIdx = cursor.getColumnIndex(Phone.TYPE)
            val labelIdx = cursor.getColumnIndex(Phone.LABEL)

            while (cursor.moveToNext()) {
                if (idIdx < 0 || numIdx < 0 || typeIdx < 0 || labelIdx < 0) continue
                val entry = RawPhoneEntry(
                    dataId = cursor.getLong(idIdx),
                    rawNumber = cursor.getString(numIdx) ?: "",
                    type = cursor.getInt(typeIdx),
                    label = cursor.getString(labelIdx),
                )
                if (AlgerianNumberNormalizer.isAddedLocalEntry(entry)) {
                    dataIdsToDelete.add(entry.dataId)
                }
            }
        }

        val ops = dataIdsToDelete.map { dataId ->
            ContentProviderOperation.newDelete(Data.CONTENT_URI)
                .withSelection("${Data._ID} = ?", arrayOf(dataId.toString()))
                .build()
        }

        val totalOps = ops.size
        onProgress(0, totalOps)
        if (totalOps == 0) {
            return@withContext BatchExecutionResult(0, 0, 0, 0, 0, 0)
        }

        var completed = 0
        var batches = 0
        for (chunk in ops.chunked(MAX_BATCH_SIZE)) {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ArrayList(chunk))
            completed += chunk.size
            batches++
            onProgress(completed, totalOps)
        }

        BatchExecutionResult(
            executedOperations = completed,
            batchesExecuted = batches,
            insertedInternational = 0,
            insertedNationalLocal = 0,
            normalizedInPlace = 0,
            deletedDuplicates = completed,
        )
    }

    suspend fun exportBackupToDownloads(records: List<RawContactRecord>): BackupResult =
        withContext(Dispatchers.IO) {
            val now = Date()
            val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(now)
            val isoTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(now)
            val displayTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)

            val vcfFileName = "contacts_backup_$dateStamp.vcf"
            val jsonFileName = "contacts_backup_$dateStamp.json"

            val vcfContent = AlgerianNumberNormalizer.buildBackupVcf(records)
            val jsonContent = AlgerianNumberNormalizer.buildBackupJson(records, isoTimestamp)

            val savedVcfName = writeToDownloads(
                fileName = vcfFileName,
                mimeType = "text/x-vcard",
                content = vcfContent,
            )
            val savedJsonName = writeToDownloads(
                fileName = jsonFileName,
                mimeType = "application/json",
                content = jsonContent,
            )

            BackupResult(
                vcfFileName = savedVcfName,
                jsonFileName = savedJsonName,
                contactsExported = records.size,
                timestampFormatted = displayTime,
            )
        }

    private fun writeToDownloads(fileName: String, mimeType: String, content: String): String {
        val bytes = content.toByteArray(Charsets.UTF_8)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Unable to create Downloads entry for $fileName")

            contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(bytes)
                stream.flush()
            } ?: throw IllegalStateException("Unable to open output stream for $fileName")

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)

            // Read back the actual assigned DISPLAY_NAME (MediaStore appends (1) on collision)
            contentResolver.query(
                uri,
                arrayOf(MediaStore.Downloads.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0) ?: fileName
                }
            }
            return fileName
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val targetFile = resolveUniqueLegacyFile(downloadsDir, fileName)
            targetFile.writeBytes(bytes)
            return targetFile.name
        }
    }

    private fun resolveUniqueLegacyFile(dir: File, fileName: String): File {
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate
        val dotIdx = fileName.lastIndexOf('.')
        val base = if (dotIdx > 0) fileName.substring(0, dotIdx) else fileName
        val ext = if (dotIdx > 0) fileName.substring(dotIdx) else ""
        var counter = 1
        while (candidate.exists()) {
            candidate = File(dir, "${base}_($counter)$ext")
            counter++
        }
        return candidate
    }

    private fun isReadOnlyAccountType(accountType: String?): Boolean {
        if (accountType == null) return false
        return accountType == "com.whatsapp" ||
            accountType == "com.whatsapp.w4b" ||
            accountType == "org.telegram.messenger" ||
            accountType == "com.viber.voip" ||
            accountType == "org.thoughtcrime.securesms" ||
            accountType == "com.facebook.orca" ||
            accountType == "com.facebook.katana" ||
            accountType == "com.skype.raider" ||
            accountType == "com.snapchat.android"
    }
}
