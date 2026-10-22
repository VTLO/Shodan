package io.cybersnippets.shodan

/**
 * Optional refinements the user can layer on top of any dork or custom query.
 * Each non-blank field is turned into the matching Shodan search filter and
 * AND-ed onto the base query server-side.
 */
data class QueryFilters(
    val country: String = "",
    val city: String = "",
    val org: String = "",
    val product: String = "",
    val os: String = "",
    val port: String = "",
    val device: String = "",
) {
    fun isEmpty(): Boolean = tokens().isEmpty()

    /** Count of active filters, for the UI badge. */
    fun activeCount(): Int = tokens().size

    /**
     * Combine the filters with [base] into a single Shodan query string.
     * String values are quoted; [port] is emitted bare so it stays numeric.
     */
    fun applyTo(base: String): String {
        val parts = buildList {
            val b = base.trim()
            if (b.isNotEmpty()) add(b)
            addAll(tokens())
        }
        return parts.joinToString(" ")
    }

    private fun tokens(): List<String> = buildList {
        quoted("country", country)?.let { add(it) }
        quoted("city", city)?.let { add(it) }
        quoted("org", org)?.let { add(it) }
        quoted("product", product)?.let { add(it) }
        quoted("os", os)?.let { add(it) }
        port.trim().takeIf { it.isNotEmpty() }?.let { add("port:$it") }
        quoted("device", device)?.let { add(it) }
    }

    private fun quoted(key: String, value: String): String? {
        val v = value.trim()
        return if (v.isEmpty()) null else "$key:\"$v\""
    }

    companion object {
        /** Device types supported by Shodan's `device:` filter (blank = any). */
        val DEVICE_OPTIONS = listOf(
            "", "firewall", "router", "wap", "webcam", "media",
            "broadband router", "pbx", "printer", "switch", "storage",
            "specialized", "phone", "voip", "voip phone", "voip adaptor",
            "load balancer", "print server", "terminal", "remote", "telecom",
            "power", "proxy", "pda", "bridge",
        )
    }
}
