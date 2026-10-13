#!/bin/bash
# End-to-end smoke test of the port (see e2e/). Usage: tools/e2e.sh [server|client|all]
#   server: headless dedicated server — mixin audit + spawn sub-levels + stay alive 600 ticks
#   client: real game window in a flat world — spawns sub-levels in view, screenshots, quits
# Results: e2e/run/<side>/logs/latest.log, screenshots in e2e/run/client/screenshots/
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd); cd "$ROOT"
[ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ] && export JAVA_HOME=$(/usr/libexec/java_home -v 21)
MODE=${1:-all}
SRV=e2e/run/server; CLI=e2e/run/client

report() { # <log> <side>
  local log=$1
  echo "== $2 =="
  grep -E "E2E:" "$log" | sed 's/^.*E2E:/  E2E:/' | sort -u | head -30
  local mixin_err; mixin_err=$(grep -cE "Mixin apply.*failed|InvalidInjectionException|InvalidMixinException|MixinTransformerError|Critical injection failure" "$log")
  local exc; exc=$(grep -cE "^\S*(Exception|Error)\b|Exception in|Caused by:" "$log")
  echo "  mixin errors: $mixin_err   exception lines: $exc"
  grep -m5 -E "Mixin apply.*failed|InvalidInjectionException|InvalidMixinException|Critical injection failure|Caused by:" "$log" | cut -c1-300
}

if [ "$MODE" = server ] || [ "$MODE" = all ]; then
  rm -rf "$SRV/world"; mkdir -p "$SRV"
  echo "eula=true" > "$SRV/eula.txt"
  printf 'level-type=minecraft\\:flat\nonline-mode=false\nlevel-name=world\nspawn-protection=0\nmax-tick-time=-1\n' > "$SRV/server.properties"
  ./gradlew :e2e:runServer --console=plain > "$SRV/gradle.log" 2>&1
  report "$SRV/logs/latest.log" server
fi

if [ "$MODE" = client ] || [ "$MODE" = all ]; then
  mkdir -p "$CLI/saves"
  if [ ! -d "$CLI/saves/e2e" ]; then
    [ -d "$SRV/world" ] || { echo "run 'tools/e2e.sh server' first (it generates the flat world)"; exit 1; }
    cp -r "$SRV/world" "$CLI/saves/e2e"
  fi
  # keep simulating while the window is unfocused; skip onboarding screens
  printf 'pauseOnLostFocus:false\nonboardAccessibility:false\ntutorialStep:none\nskipMultiplayerWarning:true\njoinedFirstServer:true\n' > "$CLI/options.txt"
  rm -rf "$CLI/screenshots"
  ./gradlew :e2e:runClient --console=plain > "$CLI/gradle.log" 2>&1
  report "$CLI/logs/latest.log" client
  ls "$CLI/screenshots" 2>/dev/null | sed 's/^/  screenshot: /'
fi
