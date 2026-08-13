package com.pressione.iperteso.services

import com.pressione.iperteso.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhrasesTest {

    @Test
    fun `returns a phrase for grade 2 most of the time`() {
        var nonNull = 0
        repeat(200) {
            if (Phrases.getRandomPhrase(Category.GRADE_2, null) != null) nonNull++
        }
        // ~70% expected; assert at least 50% to avoid flakiness
        assertTrue("Expected >100 non-null phrases, got $nonNull", nonNull > 100)
    }

    @Test
    fun `never repeats the last phrase for the same category`() {
        var last: String? = null
        var violations = 0
        repeat(500) {
            val p = Phrases.getRandomPhrase(Category.GRADE_2, null)
            if (p != null) {
                if (p == last) violations++
                last = p
            }
        }
        assertEquals(0, violations)
    }

    @Test
    fun `returns non-blank phrases for every category`() {
        val categories = Category.entries
        for (cat in categories) {
            var sawNonBlank = false
            repeat(60) {
                Phrases.getRandomPhrase(cat, null)?.let {
                    assertTrue(it.isNotBlank())
                    sawNonBlank = true
                }
            }
            assertTrue("No phrase returned for $cat", sawNonBlank)
        }
    }
}
