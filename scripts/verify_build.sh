#!/usr/bin/env bash
#
# Runs the full verification sequence for the demo flavor and writes the real
# results into BUILD_VERIFIED.md.
#
# Run this on a machine that has the Android SDK and a JDK 17 installed. The
# environment this project was authored in had neither, which is why the
# committed BUILD_VERIFIED.md is marked NOT VERIFIED.

set -uo pipefail

cd "$(dirname "$0")/.." || exit 1
REPORT="BUILD_VERIFIED.md"

run() {
  local label="$1"
  shift
  echo "==> $label"
  if "$@" > "/tmp/insangram_${label// /_}.log" 2>&1; then
    echo "    PASS"
    printf -- '- `%s` — **PASS**\n' "$*" >> "$REPORT"
    return 0
  else
    echo "    FAIL (see /tmp/insangram_${label// /_}.log)"
    printf -- '- `%s` — **FAIL**\n\n```\n%s\n```\n' \
      "$*" "$(tail -n 40 "/tmp/insangram_${label// /_}.log")" >> "$REPORT"
    return 1
  fi
}

if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  echo "gradle-wrapper.jar is missing."
  echo "Open the project in Android Studio once, or run:"
  echo "  gradle wrapper --gradle-version 8.9"
  exit 1
 fi

chmod +x gradlew

{
  echo "# Build verification report"
  echo
  echo "Generated: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  echo
  echo "## Environment"
  echo
  echo '```'
  echo "os:       $(uname -srm)"
  echo "java:     $(java -version 2>&1 | head -n 1)"
  echo "gradle:   $(./gradlew --version 2>/dev/null | grep -m1 Gradle || echo unknown)"
  echo "sdk:      ${ANDROID_HOME:-${ANDROID_SDK_ROOT:-unset}}"
  echo '```'
  echo
  echo "## Commands"
  echo
} > "$REPORT"

failures=0
run "stop"     ./gradlew --stop            || failures=$((failures + 1))
run "clean"    ./gradlew clean              || failures=$((failures + 1))
run "assemble" ./gradlew :app:assembleDemoDebug   || failures=$((failures + 1))
run "unit"     ./gradlew :app:testDemoDebugUnitTest || failures=$((failures + 1))
run "lint"     ./gradlew :app:lintDemoDebug  || failures=$((failures + 1))

APK=$(find app/build/outputs/apk -name '*.apk' 2>/dev/null | head -n 1)

{
  echo
  echo "## Result"
  echo
  if [ "$failures" -eq 0 ]; then
    echo "All commands completed successfully."
  else
    echo "$failures command(s) failed. See the log excerpts above."
  fi
  echo
  echo "APK output: ${APK:-not produced}"
} >> "$REPORT"

echo
echo "Wrote $REPORT (failures: $failures)"
exit "$failures"
