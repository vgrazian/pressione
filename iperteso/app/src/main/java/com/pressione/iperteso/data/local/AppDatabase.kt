package com.pressione.iperteso.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pressione.iperteso.data.local.dao.MedicationDao
import com.pressione.iperteso.data.local.dao.ReadingDao
import com.pressione.iperteso.data.local.dao.SettingsDao
import com.pressione.iperteso.data.local.dao.UserDao
import com.pressione.iperteso.data.local.entity.MedicationEntity
import com.pressione.iperteso.data.local.entity.ReadingEntity
import com.pressione.iperteso.data.local.entity.SettingEntity
import com.pressione.iperteso.data.local.entity.UserEntity

@Database(
    entities = [
        ReadingEntity::class,
        UserEntity::class,
        SettingEntity::class,
        MedicationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun readingDao(): ReadingDao
    abstract fun userDao(): UserDao
    abstract fun settingsDao(): SettingsDao
    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "iperteso.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
