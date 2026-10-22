package io.cybersnippets.shodan

/**
 * A single Shodan query drawn from the cheat-sheet (README.md).
 *
 * [description] explains what the query surfaces; [query] is the exact
 * Shodan search string that gets sent to the REST API.
 */
data class Dork(
    val title: String,
    val description: String,
    val query: String,
)

data class DorkCategory(
    val name: String,
    val dorks: List<Dork>,
)

/**
 * Every request type listed in the Shodan cheat-sheet, grouped by section.
 * These are the publicly documented Shodan search filters; the app simply
 * runs them against the Shodan REST API using the user's own API key.
 */
object DorkCatalog {

    val categories: List<DorkCategory> = listOf(
        DorkCategory(
            name = "Basic Filters",
            dorks = listOf(
                Dork("city", "Find devices in a particular city.", "city:\"Bangalore\""),
                Dork("country", "Find devices in a particular country.", "country:\"IN\""),
                Dork("geo", "Find devices by geographical coordinates.", "geo:\"56.913055,118.250862\""),
                Dork("hostname", "Find devices matching the hostname.", "server: \"gws\" hostname:\"google\""),
                Dork("net", "Find devices based on an IP address or /x CIDR.", "net:210.214.0.0/16"),
                Dork("os", "Find devices based on operating system.", "os:\"windows 7\""),
                Dork("port", "Find devices based on open ports.", "proftpd port:21"),
                Dork("before / after", "Find devices between a given time window.", "apache after:22/02/2009 before:14/3/2010"),
            )
        ),
        DorkCategory(
            name = "Exposed Services & Devices",
            dorks = listOf(
                Dork("Citrix", "Find Citrix Gateway.", "title:\"citrix gateway\""),
                Dork("Wifi Passwords", "Cleartext wifi passwords surfaced in Shodan.", "html:\"def_wirelesspassword\""),
                Dork("Surveillance Cams", "Surveillance cameras (uc-httpd).", "NETSurveillance uc-httpd"),
                Dork("Fuel Pumps", "Fuel pumps with a CLI terminal exposed.", "\"privileged command\" GET"),
                Dork("Windows RDP", "Windows RDP handshake banner.", "\"\\x03\\x00\\x00\\x0b\\x06\\xd0\\x00\\x00\\x124\\x00\""),
                Dork("MongoDB", "MongoDB servers exposing server information.", "\"MongoDB Server Information\" port:27017 -authentication"),
                Dork("FTP Anonymous (banner)", "FTP servers allowing anonymous access.", "\"220\" \"230 Login successful.\" port:21"),
                Dork("Jenkins", "Unrestricted Jenkins dashboards.", "x-jenkins 200"),
                Dork("Hacked Routers", "Routers that got compromised.", "hacked-router-help-sos"),
                Dork("Open ATM", "ATM availability (NCR, SNMP).", "NCR Port:\"161\""),
                Dork("Telnet Access", "Telnet gateways with no password.", "port:23 console gateway"),
                Dork("Misconfigured WordPress", "Exposed wp-config.php creation script.", "http.html:\"* The wp-config.php creation script uses this file\""),
                Dork("Hiring", "Sites advertising recruiting headers.", "\"X-Recruiting:\""),
                Dork("Android Debug Bridge", "Android root bridges on port 5555.", "\"Android Debug Bridge\" \"Device\" port:5555"),
                Dork("Ethereum Miners", "Miners running ETH.", "\"ETH - Total speed\""),
                Dork("Tesla PowerPack", "Charging status of Tesla PowerPacks.", "http.title:\"Tesla PowerPack System\" http.component:\"d3\" -ga3ca4f2"),
                Dork("Default Password", "Devices advertising a default password.", "\"default password\""),
                Dork("Windows RDP + Screenshots", "Windows servers with RDP and screenshots.", "os:windows port:3389 has_screenshot:yes"),
                Dork("MySQL in London", "MySQL servers in London, GB.", "country:gb city:london product:MySQL"),
                Dork("Google Expired SSL", "Google-owned hosts with expired SSL certs.", "org:google ssl.cert.expired:true"),
                Dork("Unprotected VNC", "VNC with authentication disabled.", "\"authentication disabled\" \"RFB 003.008\""),
                Dork("FTP Anonymous Login", "FTP anonymous login OK.", "anonymous@ login ok. port:\"21\""),
                Dork("SMB No Authentication", "SMB with authentication disabled.", "port:\"445\" Authentication: \"disabled\""),
                Dork("SMB No Auth — DC", "Domain controllers with SMB auth disabled.", "\"Authentication: disabled\" NETLOGON SYSVOL -unix port:445"),
                Dork("Cisco No Authentication", "Cisco devices advertising the cisco/cisco notice.", "\"This feature requires the one-time use of the username \\\"cisco\\\" with the password \\\"cisco\\\"\""),
                Dork("HP Printers", "HP printers via embedded HTTP server.", "\"Serial Number:\" \"Built:\" \"Server: HP HTTP\""),
                Dork("Xerox/Epson Printers (Linux UPnP)", "Epson Linux UPnP print servers.", "\"SERVER: EPSON_Linux UPnP\" \"200 OK\""),
                Dork("Xerox/Epson Printers (HTTP)", "Epson HTTP print servers.", "\"Server: EPSON-HTTP\" \"200 OK\""),
            )
        ),
        DorkCategory(
            name = "Device Types",
            dorks = listOf(
                Dork("Firewall", "device:firewall", "device:firewall"),
                Dork("Router", "device:router", "device:router"),
                Dork("WAP", "device:wap", "device:wap"),
                Dork("Webcam", "device:webcam", "device:webcam"),
                Dork("Media", "device:media", "device:media"),
                Dork("Broadband Router", "device:\"broadband router\"", "device:\"broadband router\""),
                Dork("PBX", "device:pbx", "device:pbx"),
                Dork("Printer", "device:printer", "device:printer"),
                Dork("Switch", "device:switch", "device:switch"),
                Dork("Storage", "device:storage", "device:storage"),
                Dork("Specialized", "device:specialized", "device:specialized"),
                Dork("Phone", "device:phone", "device:phone"),
                Dork("VoIP", "device:\"voip\"", "device:\"voip\""),
                Dork("VoIP Phone", "device:\"voip phone\"", "device:\"voip phone\""),
                Dork("VoIP Adaptor", "device:\"voip adaptor\"", "device:\"voip adaptor\""),
                Dork("Load Balancer", "device:\"load balancer\"", "device:\"load balancer\""),
                Dork("Print Server", "device:\"print server\"", "device:\"print server\""),
                Dork("Terminal", "device:terminal", "device:terminal"),
                Dork("Remote", "device:remote", "device:remote"),
                Dork("Telecom", "device:telecom", "device:telecom"),
                Dork("Power", "device:power", "device:power"),
                Dork("Proxy", "device:proxy", "device:proxy"),
                Dork("PDA", "device:pda", "device:pda"),
                Dork("Bridge", "device:bridge", "device:bridge"),
            )
        ),
    )

    /** Flat list of every dork, used for the search index. */
    val all: List<Dork> = categories.flatMap { it.dorks }
}
