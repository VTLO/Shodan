package io.cybersnippets.shodan

/**
 * User-agent presets applied automatically by the integrated browser when a
 * finding is opened. [SYSTEM] keeps the WebView's own UA; [CUSTOM] uses the
 * string the user typed in Settings; the rest spoof a common client so that
 * device dashboards and login pages render the way they expect.
 */
enum class UserAgentPreset(val label: String, val value: String?) {
    SYSTEM("System default (WebView)", null),
    DESKTOP_CHROME(
        "Desktop Chrome",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
    ),
    ANDROID_CHROME(
        "Android Chrome",
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36",
    ),
    IPHONE_SAFARI(
        "iPhone Safari",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
    ),
    GOOGLEBOT(
        "Googlebot",
        "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
    ),
    CUSTOM("Custom…", null);

    companion object {
        fun fromKey(key: String?): UserAgentPreset =
            entries.firstOrNull { it.name == key } ?: SYSTEM

        /**
         * Resolve the effective UA string for a preset. Returns null to mean
         * "leave the WebView default untouched".
         */
        fun resolve(preset: UserAgentPreset, custom: String): String? = when (preset) {
            SYSTEM -> null
            CUSTOM -> custom.trim().ifBlank { null }
            else -> preset.value
        }
    }
}
