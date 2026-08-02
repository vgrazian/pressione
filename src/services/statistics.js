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

/**
 * Compute discrete derivative dP/dt (mmHg/hour)
 * Applies moving average smoothing first, then calculates rate of change
 */
export function computeDerivatives(readings) {
  if (readings.length < 2) return { systolic: [], diastolic: [], timestamps: [], maxRate: 0, alarmSegments: [] }

  // Sort chronologically
  const sorted = [...readings].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))

  // Apply 3-point moving average to smooth noise
  const sysValues = sorted.map(r => r.systolic)
  const diaValues = sorted.map(r => r.diastolic)
  const smoothSys = movingAverage(sysValues, 3)
  const smoothDia = movingAverage(diaValues, 3)

  const systolic = []
  const diastolic = []
  const timestamps = []
  let maxRate = 0
  const alarmSegments = []

  for (let i = 1; i < sorted.length; i++) {
    const dtHours = (new Date(sorted[i].timestamp) - new Date(sorted[i - 1].timestamp)) / (1000 * 3600)
    if (dtHours <= 0) continue

    const ds = (smoothSys[i] - smoothSys[i - 1]) / dtHours
    const dd = (smoothDia[i] - smoothDia[i - 1]) / dtHours

    systolic.push(Math.round(ds * 10) / 10)
    diastolic.push(Math.round(dd * 10) / 10)
    timestamps.push(sorted[i].timestamp)
    maxRate = Math.max(maxRate, Math.abs(ds), Math.abs(dd))

    // Alarm: systolic rate > 10 mmHg/hour
    if (Math.abs(ds) > 10) {
      alarmSegments.push({
        index: i - 1,
        timestamp: sorted[i].timestamp,
        rate: Math.round(ds * 10) / 10,
        systolic: sorted[i].systolic,
        diastolic: sorted[i].diastolic
      })
    }
  }

  return { systolic, diastolic, timestamps, maxRate, alarmSegments }
}

/**
 * Morning surge: compare 06:00-09:00 vs 20:00-23:00
 */
export function computeMorningSurge(readings) {
  const morning = readings.filter(r => {
    const h = new Date(r.timestamp).getHours()
    return h >= 6 && h < 9
  })
  const evening = readings.filter(r => {
    const h = new Date(r.timestamp).getHours()
    return h >= 20 && h < 23
  })

  const avg = arr => arr.length ? arr.reduce((a, b) => a + b.systolic, 0) / arr.length : null
  const morningAvg = avg(morning)
  const eveningAvg = avg(evening)

  return {
    morningAvg: morningAvg ? Math.round(morningAvg) : null,
    eveningAvg: eveningAvg ? Math.round(eveningAvg) : null,
    delta: (morningAvg && eveningAvg) ? Math.round(morningAvg - eveningAvg) : null,
    morningCount: morning.length,
    eveningCount: evening.length,
    alert: (morningAvg && eveningAvg && (morningAvg - eveningAvg) > 10)
  }
}

/**
 * Hypertensive load: % of readings outside normal range
 */
export function computeHypertensiveLoad(readings) {
  if (!readings.length) return { percentage: 0, normal: 0, abnormal: 0, total: 0 }

  const abnormal = readings.filter(r =>
    r.systolic >= 140 || r.diastolic >= 90 || r.systolic < 90 || r.diastolic < 60
  ).length

  return {
    percentage: Math.round((abnormal / readings.length) * 100),
    normal: readings.length - abnormal,
    abnormal,
    total: readings.length
  }
}

/**
 * HRV simplified: standard deviation of heart rate
 */
export function computeHRV(readings) {
  if (readings.length < 2) return null
  const hrValues = readings.map(r => r.heartRate)
  const mean = hrValues.reduce((a, b) => a + b, 0) / hrValues.length
  const variance = hrValues.reduce((s, v) => s + (v - mean) ** 2, 0) / hrValues.length
  return Math.round(Math.sqrt(variance) * 10) / 10
}
