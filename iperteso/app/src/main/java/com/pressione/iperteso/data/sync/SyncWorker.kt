package com.pressione.iperteso.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pressione.iperteso.IperTesoApplication
import com.pressione.iperteso.data.remote.api.MedicationApi
import com.pressione.iperteso.data.remote.api.ReadingsApi
import com.pressione.iperteso.data.repository.MedicationRepository
import com.pressione.iperteso.data.repository.ReadingRepository
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = IperTesoApplication.instance.database
            val readingRepo = ReadingRepository(ReadingsApi(), db.readingDao())
            val medicationRepo = MedicationRepository(MedicationApi(), db.medicationDao())

            val prefs = applicationContext.getSharedPreferences("iperteso_prefs", Context.MODE_PRIVATE)
            val username = prefs.getString("active_username", null) ?: return Result.success()

            readingRepo.syncPendingReadings(username)
            readingRepo.refreshFromServer(username)
            medicationRepo.syncPending(username)
            medicationRepo.refreshFromServer(username)

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "iperteso_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
