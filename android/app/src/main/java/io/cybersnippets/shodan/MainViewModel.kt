package io.cybersnippets.shodan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class UiState(
    val apiKey: String = "",
    val userAgent: UserAgentSetting = UserAgentSetting(),
    val filters: QueryFilters = QueryFilters(),
    val activeQuery: String = "",
    val running: Boolean = false,
    val error: String? = null,
    val total: Int = 0,
    val page: Int = 1,
    val results: List<HostResult> = emptyList(),
    val apiInfo: ApiInfo? = null,
    val infoMessage: String? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ApiKeyStore(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val key = store.apiKey.first()
            val ua = store.currentUserAgent()
            _state.value = _state.value.copy(apiKey = key, userAgent = ua)
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            store.setApiKey(key)
            _state.value = _state.value.copy(apiKey = key.trim(), error = null)
        }
    }

    fun saveUserAgent(preset: UserAgentPreset, custom: String) {
        viewModelScope.launch {
            store.setUserAgent(preset, custom)
            _state.value = _state.value.copy(userAgent = UserAgentSetting(preset, custom.trim()))
        }
    }

    fun checkAccount() {
        val key = _state.value.apiKey
        viewModelScope.launch {
            _state.value = _state.value.copy(running = true, error = null, infoMessage = null)
            when (val r = ShodanApi.apiInfo(key)) {
                is ShodanResult.Ok -> _state.value = _state.value.copy(
                    running = false,
                    apiInfo = r.value,
                    infoMessage = "Key valid — plan: ${r.value.plan}, ${r.value.queryCredits} query credits.",
                )
                is ShodanResult.Err -> _state.value = _state.value.copy(
                    running = false, apiInfo = null, error = r.message,
                )
            }
        }
    }

    fun updateFilters(filters: QueryFilters) {
        _state.value = _state.value.copy(filters = filters)
    }

    fun clearFilters() {
        _state.value = _state.value.copy(filters = QueryFilters())
    }

    fun runDork(dork: Dork) = runQuery(dork.query)

    fun runQuery(query: String, page: Int = 1) {
        // Layer the active filters (country, city, device, …) onto the base query.
        val trimmed = if (page == 1) {
            _state.value.filters.applyTo(query)
        } else {
            query.trim()
        }
        if (trimmed.isEmpty()) {
            _state.value = _state.value.copy(error = "Query is empty.")
            return
        }
        val key = _state.value.apiKey
        viewModelScope.launch {
            _state.value = _state.value.copy(
                running = true,
                error = null,
                infoMessage = null,
                activeQuery = trimmed,
                page = page,
                results = if (page == 1) emptyList() else _state.value.results,
            )
            when (val r = ShodanApi.search(key, trimmed, page)) {
                is ShodanResult.Ok -> {
                    val merged = if (page == 1) r.value.matches
                    else _state.value.results + r.value.matches
                    _state.value = _state.value.copy(
                        running = false,
                        total = r.value.total,
                        page = r.value.page,
                        results = merged,
                    )
                }
                is ShodanResult.Err -> _state.value = _state.value.copy(
                    running = false, error = r.message,
                )
            }
        }
    }

    fun loadNextPage() {
        val s = _state.value
        if (s.running || s.activeQuery.isBlank()) return
        if (s.results.size >= s.total) return
        runQuery(s.activeQuery, s.page + 1)
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, infoMessage = null)
    }
}
