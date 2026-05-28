#!/usr/bin/env bash
# Simulates another app opening a deeplink via Android VIEW intent (real-world flow).
# Do NOT use "adb shell input text" for URLs — the device shell breaks on '&'.

set -euo pipefail

DEEPLINK='viewlift://amazonfiretv?asin=B011111111&s=utm_source=NBA&utm_medium=SchedulePage&utm_campaign=NBA_affiliate_2025&utm_id=affiliate&af_xp=custom&pid=nba&c=NBA%20attribution&deep_link_value=/game/12/17-at-8pm---cleveland-cavaliers-vs-chicago-bulls-1755506603&p=com.usanetwork.watcher'

# Target: production Viewlift Fire TV app (change package if your flavor differs)
PROD_PACKAGE="${PROD_PACKAGE:-com.usanetwork.watcher}"
PROD_ACTIVITY="${PROD_ACTIVITY:-com.viewlift.presentation.HomeActivity}"

# Target: this Deeplink Tester app (displays full received URI on screen)
TESTER_PACKAGE="com.firetv.deeplinktester"
TESTER_ACTIVITY="com.firetv.deeplinktester.MainActivity"

TARGET="${1:-prod}"

case "$TARGET" in
  prod|production)
    echo "Launching production app: $PROD_PACKAGE/$PROD_ACTIVITY"
    # setPackage via explicit component — same effect as ?p= in the tester app
    adb shell 'am start -W -a android.intent.action.VIEW -d "'"$DEEPLINK"'" '"$PROD_PACKAGE"'/'"$PROD_ACTIVITY"
    ;;
  tester|test)
    echo "Launching Deeplink Tester (viewlift scheme): $TESTER_PACKAGE"
    adb shell 'am start -W -a android.intent.action.VIEW -d "'"$DEEPLINK"'" '"$TESTER_PACKAGE"'/'"$TESTER_ACTIVITY"
    ;;
  *)
    echo "Usage: $0 [prod|tester]"
    echo "  prod   - open $PROD_PACKAGE (default)"
    echo "  tester - open Deeplink Tester and show full URI"
    exit 1
    ;;
esac
