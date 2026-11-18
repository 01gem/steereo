package com.gem.steereo.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val ROOT_TREE_URI = stringPreferencesKey("root_tree_uri")
        val APP_VOLUME = floatPreferencesKey("app_volume")
    }

    val rootTreeUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ROOT_TREE_URI]
    }

    val appVolume: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_VOLUME] ?: 1f
    }

    suspend fun setRootTreeUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri != null) {
                prefs[Keys.ROOT_TREE_URI] = uri
            } else {
                prefs.remove(Keys.ROOT_TREE_URI)
            }
        }
    }

    suspend fun setAppVolume(v: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_VOLUME] = v.coerceIn(0f, 1f)
        }
    }
}
