# VTLOsh0d_ — Android App

An Android client that runs **every request type listed in this repo's
[`README.md`](../README.md)** against the official
[Shodan REST API](https://developer.shodan.io/api). You supply your own
Shodan API key; the app is just a mobile front-end for the documented
`host/search` endpoint.

## What it does

- Browse the full cheat-sheet grouped into **Basic Filters**,
  **Exposed Services & Devices**, and **Device Types** (every dork from the
  root README is included — see [`Dorks.kt`](app/src/main/java/io/cybersnippets/shodan/Dorks.kt)).
- **Tap any dork** to run it; results (IP:port, org, product, location,
  hostnames, service banner) are listed with paging ("Load more").
- Type a **custom Shodan query** and run it.
- **Filter results** with a collapsible Filters panel — **country, city, org,
  port, product, OS, and device type** (dropdown). Any filter you set is
  AND-ed onto whatever dork or custom query you run (e.g. tapping *Webcam*
  with `country:US` set searches `device:"webcam" country:"US"`), and the
  combined query is shown above the results. "Clear" resets them.
- **Check key** validates your API key and shows your plan and remaining
  query credits (`/api-info`).
- The API key is stored **only on-device** via Jetpack DataStore and is sent
  only to `api.shodan.io` over HTTPS.

### Integrated browser (findings opener)

- Each result has an **Open** button that launches a built-in WebView browser
  pointed at that finding. The URL is derived from the host — HTTPS ports
  (443/8443/9443/10443/4443) use `https://`, everything else `http://`, and
  the port is kept explicit so non-standard services load.
- **Automated user-agent setup:** the browser applies your configured
  user-agent automatically on open — no per-page fiddling. Presets are
  *System default*, *Desktop Chrome*, *Android Chrome*, *iPhone Safari*,
  *Googlebot*, or a *Custom* string. Set it once under the **gear/key icon →
  Settings**; switch it live for the current page from the globe menu in the
  browser toolbar (it reloads with the new UA).
- Cleartext HTTP is allowed (`usesCleartextTraffic`) because many Shodan
  findings are plain-HTTP services. The toolbar also offers reload and
  *open in your external browser*.

## Prebuilt APK

A ready-to-install debug build is at
[`dist/shodan-dorks-debug.apk`](dist/shodan-dorks-debug.apk).

```bash
adb install dist/shodan-dorks-debug.apk
```

It is signed with the standard Android **debug** key (fine for sideloading/
testing, not for Play Store distribution).

## Build it yourself

Requirements: JDK 17, Android SDK (platform 34, build-tools 34.0.0),
Gradle 8.7+ (or open the `android/` folder in Android Studio).

```bash
cd android
echo "sdk.dir=/path/to/Android/sdk" > local.properties
gradle :app:assembleDebug          # or ./gradlew if you add the wrapper
# output: app/build/outputs/apk/debug/app-debug.apk
```

## Using the app

1. Launch **VTLOsh0d_**.
2. Tap the **key icon** (top-right) to open **Settings**: paste your Shodan
   API key and pick a browser user agent, then **Save**. (Grab the key from
   your Shodan account page.)
3. Optionally tap **Check key** to confirm it works and see your credits.
4. Tap any listed dork, or type a custom query and tap **Run**.
5. On any result, tap **Open** to load it in the integrated browser (your
   chosen user agent is applied automatically).

## Tech

- Kotlin + Jetpack Compose (Material 3), single-activity.
- `HttpURLConnection` + bundled `org.json` for networking — no third-party
  HTTP library.
- MVVM with `AndroidViewModel` + Kotlin coroutines.

## Notes

These are the publicly documented Shodan search filters. The app performs
read-only searches through Shodan's own API — it does not scan, connect to,
or interact with any discovered host. Searching consumes query credits on
your Shodan account; use it in line with Shodan's terms of service and only
for authorized security research.
