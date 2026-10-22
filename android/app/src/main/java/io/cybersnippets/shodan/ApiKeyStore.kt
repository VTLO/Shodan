package io.cybersnippets.shodan

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "shodan_prefs")

/** Persists the user's Shodan API key on-device via DataStore. */
class ApiKeyStore(private val context: Context) {

    private val keyPref = stringPreferencesKey("api_key")

    val apiKey: Flow<String> = context.dataStore.data.map { it[keyPref] ?: "" }

    suspend fun setApiKey(value: String) {
        context.dataStore.edit { it[keyPref] = value.trim() }
    }
}
