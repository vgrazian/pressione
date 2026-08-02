// Statistics computation utilities

/**
 * Compute statistics for a set of readings
 */
export function computeStatistics(readings) {
    if (!readings || readings.length === 0) {
        return {
            avgSystolic: 0, avgDiastolic: 0, avgHeartRate: 0,
            minSystolic: 0, maxSystolic: 0,
            minDiastolic: 0, maxDiastolic: 0,
            minHeartRate: 0, maxHeartRate: 0,
            readingsCount: 0,
            categoryDistribution: {},
            timeOfDayDistribution: {},
            averageMorningSystolic: null,
            averageAfternoonSystolic: null,
            averageEveningSystolic: null,
            averageNightSystolic: null
        }
    }

    const systolic = readings.map(r => r.systolic)
    const diastolic = readings.map(r => r.diastolic)
    const heartRates = readings.map(r => r.heartRate)

    const avg = arr => arr.reduce((a, b) => a + b, 0) / arr.length

    // Category distribution
    const catDist = {}
    readings.forEach(r => {
        const cat = r.category || 'UNCLASSIFIED'
        catDist[cat] = (catDist[cat] || 0) + 1
    })

    // Time of day distribution
    const todDist = { MORNING: 0, AFTERNOON: 0, EVENING: 0, NIGHT: 0 }
    const todSystolic = { MORNING: [], AFTERNOON: [], EVENING: [], NIGHT: [] }

    readings.forEach(r => {
        const hour = new Date(r.timestamp).getHours()
        let tod
        if (hour >= 6 && hour < 12) tod = 'MORNING'
        else if (hour >= 12 && hour < 17) tod = 'AFTERNOON'
        else if (hour >= 17 && hour < 22) tod = 'EVENING'
        else tod = 'NIGHT'

        todDist[tod] = (todDist[tod] || 0) + 1
        todSystolic[tod].push(r.systolic)
    })

    return {
        avgSystolic: Math.round(avg(systolic) * 10) / 10,
        avgDiastolic: Math.round(avg(diastolic) * 10) / 10,
        avgHeartRate: Math.round(avg(heartRates) * 10) / 10,
        minSystolic: Math.min(...systolic),
        maxSystolic: Math.max(...systolic),
        minDiastolic: Math.min(...diastolic),
        maxDiastolic: Math.max(...diastolic),
        minHeartRate: Math.min(...heartRates),
        maxHeartRate: Math.max(...heartRates),
        readingsCount: readings.length,
        categoryDistribution: catDist,
        timeOfDayDistribution: todDist,
        averageMorningSystolic: todSystolic.MORNING.length ? Math.round(avg(todSystolic.MORNING) * 10) / 10 : null,
        averageAfternoonSystolic: todSystolic.AFTERNOON.length ? Math.round(avg(todSystolic.AFTERNOON) * 10) / 10 : null,
        averageEveningSystolic: todSystolic.EVENING.length ? Math.round(avg(todSystolic.EVENING) * 10) / 10 : null,
        averageNightSystolic: todSystolic.NIGHT.length ? Math.round(avg(todSystolic.NIGHT) * 10) / 10 : null
    }
}

/**
 * Linear regression for trend lines
 */
export function linearRegression(points) {
    const n = points.length
    if (n < 2) return null

    let sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0
    for (const p of points) {
        sumX += p.x
        sumY += p.y
        sumXY += p.x * p.y
        sumX2 += p.x * p.x
    }

    const slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
    const intercept = (sumY - slope * sumX) / n

    return { slope, intercept }
}

/**
 * Moving average smoothing
 */
export function movingAverage(values, window = 3) {
    const result = []
    for (let i = 0; i < values.length; i++) {
        const start = Math.max(0, i - Math.floor(window / 2))
        const end = Math.min(values.length, i + Math.floor(window / 2) + 1)
        const slice = values.slice(start, end)
        result.push(slice.reduce((a, b) => a + b, 0) / slice.length)
    }
    return result
}
