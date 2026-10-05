package io.github.adam_yam.sensetouch

import org.junit.Assert.*
import org.junit.Test

class AssistantRecoveryTest {
    private class Store : RecoveryStore {
        override var pending = false
        override var original: String? = null
        var canSave = true
        override fun save(value: String?): Boolean {
            if (!canSave) return false
            original = value; pending = true; return true
        }
        override fun clear(): Boolean { pending = false; original = null; return true }
    }
    @Test fun restoresNullAndEmptyAndConfiguredValues() {
        for (before in listOf(null, "", "assistant/component")) {
            val store = Store()
            var setting = before
            val recovery = AssistantRecovery(store, "temporary", { setting }, { setting = it })
            recovery.begin()
            assertTrue(store.pending)
            assertEquals(before, store.original)
            assertEquals("temporary", setting)
            recovery.restore()
            assertEquals(before, setting)
            assertFalse(store.pending)
        }
    }
    @Test fun failedJournalNeverChangesSystemSetting() {
        val store = Store().apply { canSave = false }
        var written = false
        val recovery = AssistantRecovery(store, "temporary", { "original" }, { written = true })
        assertThrows(IllegalStateException::class.java) { recovery.begin() }
        assertFalse(written)
    }
    @Test fun failedRestoreRetainsJournalForNextInstance() {
        val store = Store()
        var setting: String? = "original"
        val recovery = AssistantRecovery(store, "temporary", { setting }, { setting = it })
        recovery.begin()
        val broken = AssistantRecovery(store, "temporary", { setting }, { throw SecurityException() })
        assertThrows(SecurityException::class.java) { broken.restore() }
        assertTrue(store.pending)
        recovery.restore()
        assertEquals("original", setting)
        assertFalse(store.pending)
    }
    @Test fun preservesUserChangeWhileRecoveryWasPending() {
        val store = Store()
        var setting: String? = "original"
        val recovery = AssistantRecovery(store, "temporary", { setting }, { setting = it })
        recovery.begin()
        setting = "new-user-choice"
        recovery.restore()
        assertEquals("new-user-choice", setting)
        assertFalse(store.pending)
    }
    @Test fun partialWriteFailureCanRecover() {
        val store = Store()
        var setting: String? = "original"
        val broken = AssistantRecovery(store, "temporary", { setting }, { setting = it; throw IllegalStateException() })
        assertThrows(IllegalStateException::class.java) { broken.begin() }
        assertTrue(store.pending)
        AssistantRecovery(store, "temporary", { setting }, { setting = it }).restore()
        assertEquals("original", setting)
    }
    @Test fun pendingJournalCannotBeOverwritten() {
        val store = Store().apply { save("original") }
        val recovery = AssistantRecovery(store, "temporary", { "temporary" }, {})
        assertThrows(IllegalStateException::class.java) { recovery.begin() }
        assertEquals("original", store.original)
    }

    @Test fun failureReadingOriginalDoesNotCreateJournalOrWrite() {
        val store = Store()
        var written = false
        val recovery = AssistantRecovery(store, "temporary", { throw SecurityException() }, { written = true })
        assertThrows(SecurityException::class.java) { recovery.begin() }
        assertFalse(store.pending)
        assertFalse(written)
    }
    @Test fun failureReadingCurrentRetainsRecoveryJournal() {
        val store = Store().apply { save("original") }
        val recovery = AssistantRecovery(store, "temporary", { throw SecurityException() }, {})
        assertThrows(SecurityException::class.java) { recovery.restore() }
        assertTrue(store.pending)
        assertEquals("original", store.original)
    }
    @Test fun repeatedRestoreIsHarmless() {
        val store = Store()
        var setting: String? = "original"
        var writes = 0
        val recovery = AssistantRecovery(store, "temporary", { setting }, { setting = it; writes++ })
        recovery.begin()
        recovery.restore()
        recovery.restore()
        assertEquals(2, writes)
        assertEquals("original", setting)
    }
    @Test fun refusesAlreadyTemporaryAssistantWithoutOverwritingIt() {
        val store = Store()
        val recovery = AssistantRecovery(store, "temporary", { "temporary" }, { fail("Must not write") })
        assertThrows(IllegalStateException::class.java) { recovery.begin() }
        assertFalse(store.pending)
    }
}
