package com.pressione.iperteso.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pressione.iperteso.data.local.AppDatabase
import com.pressione.iperteso.data.local.entity.SettingEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var settingsDao: SettingsDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        settingsDao = database.settingsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun setAndGetSetting() = runTest {
        settingsDao.setSetting(
            SettingEntity("testuser", "theme", "dark", System.currentTimeMillis())
        )

        val value = settingsDao.getSetting("testuser", "theme")
        assertEquals("dark", value)
    }

    @Test
    fun setSettingReplacesExisting() = runTest {
        settingsDao.setSetting(SettingEntity("testuser", "lang", "it"))
        settingsDao.setSetting(SettingEntity("testuser", "lang", "en"))

        val value = settingsDao.getSetting("testuser", "lang")
        assertEquals("en", value)
    }

    @Test
    fun getSettingReturnsNullForMissing() = runTest {
        val value = settingsDao.getSetting("unknown", "missing_key")
        assertNull(value)
    }

    @Test
    fun getUserSettingsReturnsAllForUser() = runTest {
        settingsDao.setSetting(SettingEntity("userA", "theme", "dark"))
        settingsDao.setSetting(SettingEntity("userA", "lang", "it"))
        settingsDao.setSetting(SettingEntity("userB", "theme", "light"))

        val userASettings = settingsDao.getUserSettings("userA")
        assertEquals(2, userASettings.size)

        val userBSettings = settingsDao.getUserSettings("userB")
        assertEquals(1, userBSettings.size)
    }

    @Test
    fun deleteSettingRemovesKey() = runTest {
        settingsDao.setSetting(SettingEntity("testuser", "temp", "value"))
        assertNotNull(settingsDao.getSetting("testuser", "temp"))

        settingsDao.deleteSetting("testuser", "temp")

        assertNull(settingsDao.getSetting("testuser", "temp"))
    }

    @Test
    fun deleteAllForUserRemovesAll() = runTest {
        settingsDao.setSetting(SettingEntity("testuser", "k1", "v1"))
        settingsDao.setSetting(SettingEntity("testuser", "k2", "v2"))
        settingsDao.setSetting(SettingEntity("other", "k1", "v1"))

        settingsDao.deleteAllForUser("testuser")

        assertEquals(0, settingsDao.getUserSettings("testuser").size)
        assertEquals(1, settingsDao.getUserSettings("other").size)
    }
}
