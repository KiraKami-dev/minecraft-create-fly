#!/bin/bash
# End-to-end smoke test of the port (see e2e/). Usage: tools/e2e.sh [server|client|all|play|playtest]
#   server: headless dedicated server — mixin audit + spawn sub-levels + stay alive 600 ticks
#   client: real game window in a flat world — spawns sub-levels in view, screenshots, quits
#   playtest: client, scripted assemble/collide/stand-on/push/rotate checks with PASS/FAIL lines (no default scene)
#   play:   same scene as client, but the game stays open for manual testing
# Results: e2e/run/<side>/logs/latest.log, screenshots in e2e/run/client/screenshots/
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd); cd "$ROOT"
[ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ] && export JAVA_HOME=$(/usr/libexec/java_home -v 21)
MODE=${1:-all}
SRV=e2e/run/server; CLI=e2e/run/client

report() { # <log> <side>
  local log=$1
  echo "== $2 =="
  if [ "$MODE" = playtest ]; then # scripted, order matters
    grep -E "E2E:" "$log" | sed 's/^.*E2E:/  E2E:/'
  else
    grep -E "E2E:" "$log" | sed 's/^.*E2E:/  E2E:/' | sort -u | head -30
  fi
  local mixin_err; mixin_err=$(grep -cE "Mixin apply.*failed|InvalidInjectionException|InvalidMixinException|MixinTransformerError|Critical injection failure" "$log")
  local exc; exc=$(grep -cE "^\S*(Exception|Error)\b|Exception in|Caused by:" "$log")
  echo "  mixin errors: $mixin_err   exception lines: $exc"
  grep -m5 -E "Mixin apply.*failed|InvalidInjectionException|InvalidMixinException|Critical injection failure|Caused by:" "$log" | cut -c1-300
}

if [ "$MODE" = server ] || [ "$MODE" = all ]; then
  rm -rf "$SRV/world"; mkdir -p "$SRV"
  echo "eula=true" > "$SRV/eula.txt"
  printf 'level-type=minecraft\\:flat\nonline-mode=false\nlevel-name=world\nspawn-protection=0\nmax-tick-time=-1\n' > "$SRV/server.properties"
  ./gradlew :e2e:runServer --console=plain > "$SRV/gradle.log" 2>&1 \
    || { echo "server run failed, see $SRV/gradle.log"; exit 1; }
  report "$SRV/logs/latest.log" server
fi

if [ "$MODE" = client ] || [ "$MODE" = all ] || [ "$MODE" = play ] || [ "$MODE" = playtest ]; then
  # Every run starts from a copy of a clean template world. The server run saves the sub-levels it
  # spawned (in sublevels/, data/ and the plot chunks), so its world can't be reused.
  TEMPLATE=e2e/run/template-world
  if [ ! -d "$TEMPLATE" ]; then
    echo "creating clean template world (one-off server boot without spawning)..."
    rm -rf "$SRV/world"; mkdir -p "$SRV"
    echo "eula=true" > "$SRV/eula.txt"
    printf 'level-type=minecraft\\:flat\nonline-mode=false\nlevel-name=world\nspawn-protection=0\nmax-tick-time=-1\n' > "$SRV/server.properties"
    ./gradlew :e2e:runServer -Pe2ePrepare --console=plain > "$SRV/gradle-template.log" 2>&1 \
      || { echo "template world creation failed, see $SRV/gradle-template.log"; exit 1; }
    cp -r "$SRV/world" "$TEMPLATE"
  fi
  mkdir -p "$CLI/saves" && rm -rf "$CLI/saves/e2e" && cp -r "$TEMPLATE" "$CLI/saves/e2e"
  # keep simulating while the window is unfocused; skip onboarding screens
  printf 'pauseOnLostFocus:false\nonboardAccessibility:false\ntutorialStep:none\nskipMultiplayerWarning:true\njoinedFirstServer:true\n' > "$CLI/options.txt"
  if [ "$MODE" = play ]; then
    ./gradlew :e2e:runClient -Pe2ePlay --console=plain > "$CLI/gradle.log" 2>&1
    exit 0
  fi
  rm -rf "$CLI/screenshots"
  SCENARIO=(); [ "$MODE" = playtest ] && SCENARIO=(-Pe2eScenario=playtest)
  ./gradlew :e2e:runClient ${SCENARIO[@]+"${SCENARIO[@]}"} --console=plain > "$CLI/gradle.log" 2>&1 \
    || { echo "client run failed, see $CLI/gradle.log"; exit 1; }
  report "$CLI/logs/latest.log" client
  ls "$CLI/screenshots" 2>/dev/null | sed 's/^/  screenshot: /'
fi
