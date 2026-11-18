package com.gem.steereo.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val ROOT_TREE_URI = stringPreferencesKey("root_tree_uri")
        val APP_VOLUME = floatPreferencesKey("app_volume")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
    }

    val rootTreeUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ROOT_TREE_URI]
    }

    val appVolume: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_VOLUME] ?: 1f
    }

    val repeatMode: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.REPEAT_MODE] ?: Player.REPEAT_MODE_OFF
    }

    val shuffleEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHUFFLE_ENABLED] ?: false
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

    suspend fun setRepeatMode(mode: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REPEAT_MODE] = mode
        }
    }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHUFFLE_ENABLED] = enabled
        }
    }
}
