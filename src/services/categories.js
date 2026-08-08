// Blood pressure category classification (ESC/ESH guidelines)
export const ReadingCategory = Object.freeze({
    NORMAL: 'NORMAL',
    ELEVATED: 'ELEVATED',
    HYPERTENSION_STAGE_1: 'HYPERTENSION_STAGE_1',
    HYPERTENSION_STAGE_2: 'HYPERTENSION_STAGE_2',
    HYPERTENSIVE_CRISIS: 'HYPERTENSIVE_CRISIS',
    HYPOTENSION: 'HYPOTENSION',
    UNCLASSIFIED: 'UNCLASSIFIED'
})

const CATEGORY_CONFIG = {
    [ReadingCategory.NORMAL]: {
        label: 'Normale',
        color: '#3B5D45',
        bgColor: '#DCE8DF',
        severity: 0
    },
    [ReadingCategory.ELEVATED]: {
        label: 'Elevata',
        color: '#7C6900',
        bgColor: '#FFF9C4',
        severity: 1
    },
    [ReadingCategory.HYPERTENSION_STAGE_1]: {
        label: 'Ipertensione Stadio 1',
        color: '#BA4900',
        bgColor: '#FFE0B2',
        severity: 2
    },
    [ReadingCategory.HYPERTENSION_STAGE_2]: {
        label: 'Ipertensione Stadio 2',
        color: '#BA1A1A',
        bgColor: '#FFCDD2',
        severity: 3
    },
    [ReadingCategory.HYPERTENSIVE_CRISIS]: {
        label: 'Crisi Ipertensiva',
        color: '#690005',
        bgColor: '#FFB4AB',
        severity: 4
    },
    [ReadingCategory.HYPOTENSION]: {
        label: 'Ipotensione',
        color: '#004C99',
        bgColor: '#CCE5FF',
        severity: 2
    },
    [ReadingCategory.UNCLASSIFIED]: {
        label: 'Non Classificata',
        color: '#666666',
        bgColor: '#E0E0E0',
        severity: -1
    }
}

/**
 * Classify a blood pressure reading based on ESC/ESH guidelines
 */
export function classifyReading(systolic, diastolic) {
    if (!systolic || !diastolic) return ReadingCategory.UNCLASSIFIED

    if (systolic >= 180 || diastolic >= 120) return ReadingCategory.HYPERTENSIVE_CRISIS
    if (systolic >= 140 || diastolic >= 90) return ReadingCategory.HYPERTENSION_STAGE_2
    if (systolic >= 130 || diastolic >= 80) return ReadingCategory.HYPERTENSION_STAGE_1
    if (systolic >= 120 && diastolic < 80) return ReadingCategory.ELEVATED
    if (systolic < 90 || diastolic < 60) return ReadingCategory.HYPOTENSION
    if (systolic < 120 && diastolic < 80) return ReadingCategory.NORMAL

    return ReadingCategory.UNCLASSIFIED
}

export function getCategoryConfig(category) {
    return CATEGORY_CONFIG[category] || CATEGORY_CONFIG[ReadingCategory.UNCLASSIFIED]
}

export function getCategoryLabel(category) {
    return getCategoryConfig(category).label
}

export function getCategoryColor(category) {
    return getCategoryConfig(category).color
}

export function getCategoryBgColor(category) {
    return getCategoryConfig(category).bgColor
}

export const ALL_CATEGORIES = Object.values(ReadingCategory)
