package com.gem.steereo

import android.app.Application
import com.gem.steereo.data.MusicRepository
import com.gem.steereo.data.db.AppDatabase
import com.gem.steereo.data.prefs.SettingsStore
import com.gem.steereo.data.scan.MusicScanner

class SteereoApp : Application() {
    val settings by lazy { SettingsStore(this) }
    val database by lazy { AppDatabase.build(this) }
    val repository by lazy { MusicRepository(database, MusicScanner(this)) }
}
