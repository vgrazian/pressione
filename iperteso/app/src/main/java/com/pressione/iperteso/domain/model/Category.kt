package com.pressione.iperteso.domain.model

/**
 * ESC/ESH blood pressure categories matching the web app's classification.
 */
enum class Category(
    val label: String,
    val labelEn: String,
    val systolicRange: IntRange,
    val diastolicRange: IntRange
) {
    OPTIMAL("Ottimale", "Optimal", 0..119, 0..79),
    NORMAL("Normale", "Normal", 120..129, 80..84),
    HIGH_NORMAL("Normale-Alta", "High Normal", 130..139, 85..89),
    GRADE_1("Ipertensione Grado 1", "Grade 1 Hypertension", 140..159, 90..99),
    GRADE_2("Ipertensione Grado 2", "Grade 2 Hypertension", 160..179, 100..109),
    GRADE_3("Ipertensione Grado 3", "Grade 3 Hypertension", 180..299, 110..199),
    CRISIS("Crisi Ipertensiva", "Hypertensive Crisis", 0..299, 0..199);

    companion object {
        /**
         * Classify a reading based on ESC/ESH 2024 guidelines.
         * If SYS ≥ 180 or DIA ≥ 110 independently of the other value → GRADE_3.
         * If both SYS and DIA are in crisis range → CRISIS.
         */
        fun classify(systolic: Int, diastolic: Int): Category {
            // Crisis: SYS > 180 AND DIA > 110
            if (systolic > 180 && diastolic > 110) return CRISIS

            // Grade 3: SYS ≥ 180 OR DIA ≥ 110
            if (systolic >= 180 || diastolic >= 110) return GRADE_3

            // Standard classification
            return entries
                .filter { it != CRISIS && it != GRADE_3 }
                .lastOrNull { systolic in it.systolicRange && diastolic in it.diastolicRange }
                ?: if (systolic >= 140 || diastolic >= 90) GRADE_2
                else HIGH_NORMAL
        }
    }
}
