# Fire TV Deeplink Launcher

Third-party Fire TV app that loads deeplinks from a demo feeds API and opens them in your production app (simulates an external partner app).

**Package:** `com.firetv.deeplinktester`

## Required setup

Before running the app, you must configure feed URL and Basic Auth values in:

`app/src/main/res/values/strings.xml`

Set these fields:

- `feed_auth_username`
- `feed_auth_password`
- `feeds_url`

If these are left blank, the app shows a helper message on screen and skips feed loading.

## Features

- Compose-for-TV UI with focusable buttons and D-pad navigation
- Fetches feed from configurable URL with Basic Auth
- Scrollable list: **title**, **Open URL**, **Edit**
- Uses each item’s **`firetvUrl`** as the deeplink
- **Edit** overrides URL locally (saved per feed id); **Reset to feed** restores API value
- **Open URL** fires `ACTION_VIEW` with the raw `firetvUrl` (no package forcing, no URL rewriting)
- Add custom feed items on-screen with the **Add Feed** button

## Project structure

```text
firetv-deeplink-tester/
├── app/
│   ├── src/main/java/com/firetv/deeplinktester/
│   │   ├── MainActivity.kt          # Compose TV UI and feed screen
│   │   ├── FeedRepository.kt        # HTTP fetch + Basic Auth header
│   │   ├── FeedParser.kt            # JSON parsing for nested feed structures
│   │   ├── DeeplinkLauncher.kt      # ACTION_VIEW launcher
│   │   ├── DeeplinkOverrides.kt     # Local edited URL storage
│   │   └── FeedItem.kt              # Feed model
│   ├── src/main/res/
│   │   └── values/strings.xml       # feed_auth_username/password/feeds_url
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

## Feed JSON support

Parser supports nested payloads and extracts `firetvUrl` (also accepts `fireTvUrl`, `fire_tv_url`), including structures like:

- `events -> airings -> links -> firetvUrl`
- `feeds/items/data/results/...`

Example:

```json
{
  "events": [
    {
      "title": "WNBA Deeplink Test",
      "airings": [
        {
          "links": {
            "firetvUrl": "demo://amazonfiretv?deep_link_value=/game/...&p=com.example.app"
          }
        }
      ]
    }
  ]
}
```

## Build & install

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- `401 Unauthorized` usually means missing/invalid `feed_auth_username`, `feed_auth_password`, or an expired token in `feeds_url`.
- If multiple apps handle the same deeplink scheme/host, Android may show a chooser (this app does not force package selection).
