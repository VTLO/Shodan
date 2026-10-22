package io.cybersnippets.shodan

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "shodan_prefs")

/** On-device setting for the browser's user agent. */
data class UserAgentSetting(
    val preset: UserAgentPreset = UserAgentPreset.SYSTEM,
    val custom: String = "",
) {
    /** The effective UA string, or null to keep the WebView default. */
    fun resolved(): String? = UserAgentPreset.resolve(preset, custom)
}

/**
 * Persists the user's Shodan API key and integrated-browser user-agent
 * preference on-device via DataStore.
 */
class ApiKeyStore(private val context: Context) {

    private val keyPref = stringPreferencesKey("api_key")
    private val uaPresetPref = stringPreferencesKey("ua_preset")
    private val uaCustomPref = stringPreferencesKey("ua_custom")

    val apiKey: Flow<String> = context.dataStore.data.map { it[keyPref] ?: "" }

    val userAgent: Flow<UserAgentSetting> = context.dataStore.data.map { prefs ->
        UserAgentSetting(
            preset = UserAgentPreset.fromKey(prefs[uaPresetPref]),
            custom = prefs[uaCustomPref] ?: "",
        )
    }

    suspend fun setApiKey(value: String) {
        context.dataStore.edit { it[keyPref] = value.trim() }
    }

    suspend fun setUserAgent(preset: UserAgentPreset, custom: String) {
        context.dataStore.edit {
            it[uaPresetPref] = preset.name
            it[uaCustomPref] = custom.trim()
        }
    }

    /** One-shot read used by the browser activity when it starts. */
    suspend fun currentUserAgent(): UserAgentSetting = userAgent.first()
}
