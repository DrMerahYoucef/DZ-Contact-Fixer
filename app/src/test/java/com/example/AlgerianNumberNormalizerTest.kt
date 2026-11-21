package com.example

import android.provider.ContactsContract.CommonDataKinds.Phone
import com.example.data.AlgerianNumberNormalizer
import com.example.data.CleanupMode
import com.example.data.ContactFixStatus
import com.example.data.NumberClassification
import com.example.data.PlannedOperation
import com.example.data.RawContactRecord
import com.example.data.RawPhoneEntry
import com.example.data.SkipReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlgerianNumberNormalizerTest {

    private fun record(
        rawId: Long = 1L,
        name: String = "Test Contact",
        isReadOnly: Boolean = false,
        vararg numbers: Pair<Long, String>,
    ) = RawContactRecord(
        rawContactId = rawId,
        contactId = rawId,
        displayName = name,
        accountType = "com.google",
        accountName = "user@gmail.com",
        isReadOnly = isReadOnly,
        phoneEntries = numbers.map { (id, num) ->
            RawPhoneEntry(dataId = id, rawNumber = num, type = Phone.TYPE_MOBILE, label = null)
        },
    )

    @Test
    fun `classifies international and national Algerian mobile numbers across prefixes 5 6 7`() {
        val c1 = AlgerianNumberNormalizer.classify("+213798927676") as NumberClassification.AlgerianMobile
        assertEquals("798927676", c1.core9)
        assertTrue(c1.isInternational)
        assertTrue(c1.isCleanUnspaced)

        val c2 = AlgerianNumberNormalizer.classify("+213 771 33 81 52") as NumberClassification.AlgerianMobile
        assertEquals("771338152", c2.core9)
        assertTrue(c2.isInternational)
        assertFalse(c2.isCleanUnspaced)

        val c3 = AlgerianNumberNormalizer.classify("0661411961") as NumberClassification.AlgerianMobile
        assertEquals("661411961", c3.core9)
        assertFalse(c3.isInternational)
        assertTrue(c3.isCleanUnspaced)

        val c4 = AlgerianNumberNormalizer.classify("0561 88 86 24") as NumberClassification.AlgerianMobile
        assertEquals("561888624", c4.core9)
        assertFalse(c4.isInternational)
        assertFalse(c4.isCleanUnspaced)
    }

    @Test
    fun `ignores foreign numbers Algerian landlines and toll-free numbers`() {
        val foreignSamples = listOf("+905523361006", "+33 7 58 60 69 03", "+41779494734", "+34631738758")
        foreignSamples.forEach { num ->
            val result = AlgerianNumberNormalizer.classify(num)
            assertTrue("Expected Ignored for $num", result is NumberClassification.Ignored)
            assertEquals(SkipReason.FOREIGN_NUMBER, (result as NumberClassification.Ignored).reason)
        }

        val landlineSamples = listOf("041403131", "+21323839803", "1800407267864")
        landlineSamples.forEach { num ->
            val result = AlgerianNumberNormalizer.classify(num)
            assertTrue("Expected Ignored for $num", result is NumberClassification.Ignored)
            assertEquals(SkipReason.LANDLINE_OR_SHORT, (result as NumberClassification.Ignored).reason)
        }
    }

    @Test
    fun `mode B collapses spaced duplicate and adds missing national local entry`() {
        // Mirrors "+213 542 09 98 32" + "+213542099832"
        val rec = record(
            rawId = 10L,
            name = "Sample Intl Duplicate",
            numbers = arrayOf(101L to "+213 542 09 98 32", 102L to "+213542099832"),
        )
        val plan = AlgerianNumberNormalizer.buildPlanForContact(rec, CleanupMode.CLEAN_AND_NORMALIZE)
        assertEquals(ContactFixStatus.NEEDS_FIX, plan.status)
        assertEquals(2, plan.operations.size)
        assertTrue(plan.operations.any { it is PlannedOperation.DeleteSpacedDuplicate && it.dataId == 101L })
        assertTrue(plan.operations.any { it is PlannedOperation.InsertNationalLocal && it.targetNumber == "0542099832" })
    }

    @Test
    fun `mode A safe append only keeps spaced duplicate and only inserts missing counterpart`() {
        val rec = record(
            rawId = 11L,
            name = "Sample Intl Duplicate",
            numbers = arrayOf(101L to "+213 542 09 98 32", 102L to "+213542099832"),
        )
        val plan = AlgerianNumberNormalizer.buildPlanForContact(rec, CleanupMode.SAFE_APPEND_ONLY)
        assertEquals(1, plan.operations.size)
        val op = plan.operations.single() as PlannedOperation.InsertNationalLocal
        assertEquals("0542099832", op.targetNumber)
    }

    @Test
    fun `mode B handles multiple distinct numbers and mixed foreign number on same contact`() {
        // Mirrors multi-number contact with spaced duplicate on 1st core and clean 2nd core, plus foreign number
        val rec = record(
            rawId = 12L,
            name = "Multi Number",
            numbers = arrayOf(
                201L to "0550 46 49 51",
                202L to "0550464951",
                203L to "0774782026",
                204L to "+905388766789",
            ),
        )
        val plan = AlgerianNumberNormalizer.buildPlanForContact(rec, CleanupMode.CLEAN_AND_NORMALIZE)
        assertEquals(ContactFixStatus.NEEDS_FIX, plan.status)
        assertEquals(2, plan.algerianCoresCount)
        assertEquals(1, plan.untouchedEntries.size)
        assertTrue(plan.operations.any { it is PlannedOperation.DeleteSpacedDuplicate && it.dataId == 201L })
        assertTrue(plan.operations.any { it is PlannedOperation.InsertInternational && it.targetNumber == "+213550464951" })
        assertTrue(plan.operations.any { it is PlannedOperation.InsertInternational && it.targetNumber == "+213774782026" })
    }

    @Test
    fun `compliant contact with clean pair reports COMPLIANT and zero operations`() {
        val rec = record(
            rawId = 13L,
            name = "Already Ready",
            numbers = arrayOf(301L to "+213667879054", 302L to "0667879054"),
        )
        val plan = AlgerianNumberNormalizer.buildPlanForContact(rec, CleanupMode.CLEAN_AND_NORMALIZE)
        assertEquals(ContactFixStatus.COMPLIANT, plan.status)
        assertTrue(plan.operations.isEmpty())
    }

    @Test
    fun `local custom label detection matches DZ Local and legacy Toyota 0X while preserving user originals`() {
        val addedDzLocal = RawPhoneEntry(1L, "0558460474", Phone.TYPE_CUSTOM, AlgerianNumberNormalizer.LOCAL_CUSTOM_LABEL)
        val addedLegacyToyota = RawPhoneEntry(2L, "0771338152", Phone.TYPE_CUSTOM, AlgerianNumberNormalizer.LEGACY_TOYOTA_LABEL)
        val userMobile = RawPhoneEntry(3L, "0661411961", Phone.TYPE_MOBILE, null)
        val localIntl = RawPhoneEntry(4L, "+213558460474", Phone.TYPE_CUSTOM, AlgerianNumberNormalizer.LOCAL_CUSTOM_LABEL)

        assertTrue(AlgerianNumberNormalizer.isAddedLocalEntry(addedDzLocal))
        assertTrue(AlgerianNumberNormalizer.isAddedLocalEntry(addedLegacyToyota))
        assertFalse(AlgerianNumberNormalizer.isAddedLocalEntry(userMobile))
        assertFalse(AlgerianNumberNormalizer.isAddedLocalEntry(localIntl))
    }

    @Test
    fun `backup serialization outputs valid vCard 3_0 and JSON structure`() {
        val records = listOf(
            record(
                rawId = 99L,
                name = "Test, Driver",
                numbers = arrayOf(1L to "+213558460474"),
            )
        )
        val vcf = AlgerianNumberNormalizer.buildBackupVcf(records)
        assertTrue(vcf.contains("BEGIN:VCARD"))
        assertTrue(vcf.contains("VERSION:3.0"))
        assertTrue(vcf.contains("FN:Test\\, Driver"))
        assertTrue(vcf.contains("TEL;TYPE=CELL:+213558460474"))
        assertTrue(vcf.contains("END:VCARD"))

        val json = AlgerianNumberNormalizer.buildBackupJson(records, "2026-11-13T03:00:00+0100")
        assertTrue(json.contains("\"DZ Contact Fixer\""))
        assertTrue(json.contains("\"+213558460474\""))
    }

    @Test
    fun `linked read-only WhatsApp contact sharing contactId with writable contact is marked COVERED_BY_WRITABLE`() {
        val googleContact = RawContactRecord(
            rawContactId = 1L,
            contactId = 500L,
            displayName = "2 Os Avant Bras",
            accountType = "com.google",
            accountName = "youcef@gmail.com",
            isReadOnly = false,
            phoneEntries = listOf(RawPhoneEntry(11L, "+213798927676", Phone.TYPE_MOBILE, null)),
        )
        val whatsappMirror = RawContactRecord(
            rawContactId = 2L,
            contactId = 500L,
            displayName = "2 Os Avant Bras",
            accountType = "com.whatsapp",
            accountName = "WhatsApp",
            isReadOnly = true,
            phoneEntries = listOf(RawPhoneEntry(22L, "+213798927676", Phone.TYPE_MOBILE, null)),
        )

        val plans = AlgerianNumberNormalizer.buildPlans(
            listOf(googleContact, whatsappMirror),
            CleanupMode.CLEAN_AND_NORMALIZE,
        )
        val googlePlan = plans.first { it.rawContactId == 1L }
        val whatsappPlan = plans.first { it.rawContactId == 2L }

        assertEquals(ContactFixStatus.NEEDS_FIX, googlePlan.status)
        assertEquals(ContactFixStatus.COVERED_BY_WRITABLE, whatsappPlan.status)
        assertTrue(whatsappPlan.operations.isEmpty())
    }

    @Test
    fun `orphan read-only contact creates writable mirror in primary Google account automatically`() {
        val googleContact = RawContactRecord(
            rawContactId = 1L,
            contactId = 500L,
            displayName = "Existing Google",
            accountType = "com.google",
            accountName = "youcef@gmail.com",
            isReadOnly = false,
            phoneEntries = listOf(RawPhoneEntry(11L, "+213798927676", Phone.TYPE_MOBILE, null)),
        )
        val orphanWhatsapp = RawContactRecord(
            rawContactId = 3L,
            contactId = 777L,
            displayName = "Orphan WhatsApp",
            accountType = "com.whatsapp",
            accountName = "WhatsApp",
            isReadOnly = true,
            phoneEntries = listOf(
                RawPhoneEntry(31L, "+213 558 46 04 74", Phone.TYPE_MOBILE, null),
                RawPhoneEntry(32L, "+33753168302", Phone.TYPE_MOBILE, null),
            ),
        )

        val plans = AlgerianNumberNormalizer.buildPlans(
            listOf(googleContact, orphanWhatsapp),
            CleanupMode.CLEAN_AND_NORMALIZE,
        )
        val orphanPlan = plans.first { it.rawContactId == 3L }

        assertEquals(ContactFixStatus.NEEDS_FIX, orphanPlan.status)
        assertEquals(1, orphanPlan.untouchedEntries.size)
        val mirrorOp = orphanPlan.operations.single() as PlannedOperation.CreateWritableMirrorContact
        assertEquals("Orphan WhatsApp", mirrorOp.displayName)
        assertEquals("com.google", mirrorOp.targetAccount.accountType)
        assertEquals("youcef@gmail.com", mirrorOp.targetAccount.accountName)
        assertEquals(listOf("558460474"), mirrorOp.cores)
        assertEquals(4, mirrorOp.cpoCount())
    }
}
