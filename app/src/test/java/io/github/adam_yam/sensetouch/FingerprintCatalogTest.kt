package io.github.adam_yam.sensetouch

import org.junit.Assert.*
import org.junit.Test

class FingerprintCatalogTest {
    @Test fun keepsActualIdsAndCustomNames() {
        val entries = listOf(RegisteredFingerprint(7, "왼쪽 엄지"), RegisteredFingerprint(12, "오른쪽 검지"))
        assertEquals(entries, FingerprintCatalog.validate(entries))
    }
    @Test fun fillsMissingNameWithoutChangingId() {
        assertEquals(RegisteredFingerprint(9, "등록 지문 1"), FingerprintCatalog.validate(listOf(RegisteredFingerprint(9, " "))).single())
    }
    @Test fun emptyListStaysEmpty() { assertTrue(FingerprintCatalog.validate(emptyList()).isEmpty()) }
    @Test fun duplicateIdIsAnError() {
        assertThrows(IllegalStateException::class.java) {
            FingerprintCatalog.validate(listOf(RegisteredFingerprint(2, "A"), RegisteredFingerprint(2, "B")))
        }
    }
    @Test fun invalidIdIsAnError() {
        assertThrows(IllegalStateException::class.java) { FingerprintCatalog.validate(listOf(RegisteredFingerprint(0, "A"))) }
    }
}
