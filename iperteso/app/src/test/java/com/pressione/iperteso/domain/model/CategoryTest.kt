package com.pressione.iperteso.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTest {

    @Test
    fun `classify optimal blood pressure`() {
        assertEquals(Category.OPTIMAL, Category.classify(110, 70))
        assertEquals(Category.OPTIMAL, Category.classify(119, 79))
        assertEquals(Category.OPTIMAL, Category.classify(90, 60))
    }

    @Test
    fun `classify normal blood pressure`() {
        assertEquals(Category.NORMAL, Category.classify(120, 80))
        assertEquals(Category.NORMAL, Category.classify(129, 84))
        assertEquals(Category.NORMAL, Category.classify(125, 82))
    }

    @Test
    fun `classify high normal blood pressure`() {
        assertEquals(Category.HIGH_NORMAL, Category.classify(130, 85))
        assertEquals(Category.HIGH_NORMAL, Category.classify(139, 89))
        assertEquals(Category.HIGH_NORMAL, Category.classify(135, 87))
    }

    @Test
    fun `classify grade 1 hypertension`() {
        assertEquals(Category.GRADE_1, Category.classify(140, 90))
        assertEquals(Category.GRADE_1, Category.classify(159, 99))
        assertEquals(Category.GRADE_1, Category.classify(150, 95))
    }

    @Test
    fun `classify grade 2 hypertension`() {
        assertEquals(Category.GRADE_2, Category.classify(160, 100))
        assertEquals(Category.GRADE_2, Category.classify(179, 109))
        assertEquals(Category.GRADE_2, Category.classify(170, 105))
    }

    @Test
    fun `classify grade 3 hypertension`() {
        // GRADE_3: Systolic ≥ 180 OR diastolic ≥ 110 (but NOT both > threshold = not crisis)
        assertEquals(Category.GRADE_3, Category.classify(190, 105)) // sys=190, dia=105: sys≥180, dia<110
        assertEquals(Category.GRADE_3, Category.classify(170, 112)) // sys=170, dia=112: sys<180, dia≥110
        // Isolated diastolic ≥ 110
        assertEquals(Category.GRADE_3, Category.classify(120, 115))
        assertEquals(Category.GRADE_3, Category.classify(135, 110))
    }

    @Test
    fun `classify crisis when both values are extreme`() {
        assertEquals(Category.CRISIS, Category.classify(185, 115))
        assertEquals(Category.CRISIS, Category.classify(200, 130))
    }

    @Test

    fun `all categories have labels`() {
        Category.entries.forEach {
            assert(it.label.isNotBlank()) { "Missing label for $it" }
            assert(it.labelEn.isNotBlank()) { "Missing English label for $it" }
        }
    }

    @Test
    fun `category count matches ESC-ESH 2024 guidelines`() {
        assertEquals(7, Category.entries.size)
    }
}
