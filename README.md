# Fire TV Deeplink Launcher

Third-party Fire TV app that loads deeplinks from a Viewlift feeds API and opens them in your production app (simulates an external partner app).

**Package:** `com.firetv.deeplinktester`

## Features

- Fetches feed from configurable URL (`feeds_url` in `strings.xml`)
- Scrollable list: **title**, **Open URL**, **Edit**
- Uses each item’s **`firetvUrl`** as the deeplink
- **Edit** overrides URL locally (saved per feed id); **Reset to feed** restores API value
- **Open URL** fires `ACTION_VIEW` with the raw `firetvUrl` (no package forcing, no URL rewriting)

## Feed JSON

Parser looks for arrays under `feeds`, `items`, `data`, etc. Each object must include **`firetvUrl`** (also accepts `fireTvUrl`, `fire_tv_url`).

Example:

```json
{
  "feeds": [
    {
      "title": "Game deeplink",
      "firetvUrl": "viewlift://amazonfiretv?deep_link_value=/game/...&p=com.example.app"
    }
  ]
}
```

## Build & install

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Change feed URL

Edit `app/src/main/res/values/strings.xml` → `feeds_url`.

If the feed returns `401 Unauthorized`, update the `token` query parameter from your staging CMS.
