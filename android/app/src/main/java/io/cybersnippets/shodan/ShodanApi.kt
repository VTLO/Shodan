package io.cybersnippets.shodan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One host returned by a Shodan search. */
data class HostResult(
    val ip: String,
    val port: Int,
    val transport: String,
    val org: String,
    val product: String,
    val location: String,
    val hostnames: List<String>,
    val timestamp: String,
    val banner: String,
)

/** A full search response (one page). */
data class SearchResponse(
    val total: Int,
    val page: Int,
    val matches: List<HostResult>,
)

/** Account/plan information returned by /api-info. */
data class ApiInfo(
    val plan: String,
    val queryCredits: Int,
    val scanCredits: Int,
    val monitoredIps: Int?,
    val https: Boolean,
)

sealed interface ShodanResult<out T> {
    data class Ok<T>(val value: T) : ShodanResult<T>
    data class Err(val message: String) : ShodanResult<Nothing>
}

/** Ports that are conventionally served over TLS. */
private val HTTPS_PORTS = setOf(443, 8443, 9443, 10443, 4443)

/**
 * Build a browsable URL for a finding. HTTPS ports get `https://`, everything
 * else `http://`; the port is kept explicit so non-standard services load.
 */
fun HostResult.toUrl(): String {
    val scheme = if (port in HTTPS_PORTS) "https" else "http"
    return if (port > 0) "$scheme://$ip:$port" else "$scheme://$ip"
}

/**
 * Thin client over the Shodan REST API (https://developer.shodan.io/api).
 * The user supplies their own API key; this app is just a mobile front-end
 * for the documented search endpoint.
 */
object ShodanApi {

    private const val BASE = "https://api.shodan.io"
    private const val TIMEOUT_MS = 20_000

    suspend fun apiInfo(apiKey: String): ShodanResult<ApiInfo> =
        request("/api-info", mapOf("key" to apiKey)) { json ->
            ApiInfo(
                plan = json.optString("plan", "unknown"),
                queryCredits = json.optInt("query_credits", 0),
                scanCredits = json.optInt("scan_credits", 0),
                monitoredIps = if (json.has("monitored_ips")) json.optInt("monitored_ips") else null,
                https = json.optBoolean("https", false),
            )
        }

    suspend fun search(apiKey: String, query: String, page: Int = 1): ShodanResult<SearchResponse> =
        request(
            "/shodan/host/search",
            mapOf("key" to apiKey, "query" to query, "page" to page.toString()),
        ) { json ->
            val matchesArr = json.optJSONArray("matches")
            val matches = buildList {
                if (matchesArr != null) {
                    for (i in 0 until matchesArr.length()) {
                        add(parseHost(matchesArr.getJSONObject(i)))
                    }
                }
            }
            SearchResponse(
                total = json.optInt("total", matches.size),
                page = page,
                matches = matches,
            )
        }

    private fun parseHost(o: JSONObject): HostResult {
        val loc = o.optJSONObject("location")
        val locationParts = buildList {
            loc?.optString("city").takeUnless { it.isNullOrBlank() || it == "null" }?.let { add(it) }
            loc?.optString("country_name").takeUnless { it.isNullOrBlank() || it == "null" }?.let { add(it) }
        }
        val hostnamesArr = o.optJSONArray("hostnames")
        val hostnames = buildList {
            if (hostnamesArr != null) {
                for (i in 0 until hostnamesArr.length()) add(hostnamesArr.getString(i))
            }
        }
        val banner = o.optString("data", "").trim().take(600)
        return HostResult(
            ip = o.optString("ip_str", "?"),
            port = o.optInt("port", 0),
            transport = o.optString("transport", "tcp"),
            org = o.optString("org", "").takeUnless { it == "null" } ?: "",
            product = o.optString("product", "").takeUnless { it == "null" } ?: "",
            location = locationParts.joinToString(", "),
            hostnames = hostnames,
            timestamp = o.optString("timestamp", ""),
            banner = banner,
        )
    }

    private suspend fun <T> request(
        path: String,
        params: Map<String, String>,
        parse: (JSONObject) -> T,
    ): ShodanResult<T> = withContext(Dispatchers.IO) {
        if (params["key"].isNullOrBlank()) {
            return@withContext ShodanResult.Err("No API key set. Add it in Settings.")
        }
        val query = params.entries.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, "UTF-8")}"
        }
        val url = URL("$BASE$path?$query")
        var conn: HttpURLConnection? = null
        try {
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code in 200..299) {
                val json = JSONObject(if (body.isBlank()) "{}" else body)
                ShodanResult.Ok(parse(json))
            } else {
                val message = runCatching { JSONObject(body).optString("error") }
                    .getOrNull()
                    ?.takeUnless { it.isBlank() }
                    ?: httpMessage(code)
                ShodanResult.Err(message)
            }
        } catch (e: Exception) {
            ShodanResult.Err(e.message ?: "Network error")
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpMessage(code: Int): String = when (code) {
        401 -> "Invalid API key (401)."
        403 -> "Access denied — your plan may not allow search filters (403)."
        429 -> "Rate limit reached. Slow down (429)."
        else -> "Request failed (HTTP $code)."
    }
}
