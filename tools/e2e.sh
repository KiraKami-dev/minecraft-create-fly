#!/bin/bash
# End-to-end tests of the port (see e2e/). Usage: tools/e2e.sh [server|gametest [filter [verify]]|client|flight|all|play|playtest] [scenario]
#   server:   headless dedicated server — mixin audit + spawn sub-levels + stay alive 600 ticks
#   gametest: headless GameTest server — runs the @GameTest suite (e2e/src/main/java/dev/mcf/e2e/gametest),
#             prints one line per test and exits non-zero if a required test failed. The optional filter is a
#             test selector, e.g. 'mcf_e2e:sable_physics_test_*'; "verify" after it runs every selected test
#             100 times in each of the 4 rotations (flakiness check) and prints counts instead
#   client:   real game window in a flat world — spawns sub-levels in view, screenshots, quits
#   playtest: client, scripted assemble/collide/stand-on/push/rotate checks with PASS/FAIL lines (no default scene)
#   flight:   real game window: an airship (propeller bearing, burner, levitite) is built in view, assembled and
#             flies up; screenshots e2e/run/client/screenshots/flight-*.png, quits
#   play:     same scene as client, but the game stays open for manual testing
#   all:      server, gametest, then client
#   scenario: optional second argument of server/client/play, e.g. 'flywheel' = Create blocks assembled into a yawed sub-level
# Results: e2e/run/<side>/logs/latest.log, screenshots in e2e/run/client/screenshots/,
#          JUnit report in e2e/run/gametest/gametest-report.xml
# GRADLEW overrides the Gradle command (default: ./gradlew --console=plain), e.g. a wrapper that queues builds.
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd); cd "$ROOT"
[ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ] && export JAVA_HOME=$(/usr/libexec/java_home -v 21)
MODE=${1:-all}
GRADLEW=${GRADLEW:-./gradlew --console=plain}
SRV=e2e/run/server; CLI=e2e/run/client; GT=e2e/run/gametest
# vanilla's default flat world spelled out (an empty generator-settings makes the server log "No key layers")
FLAT='{"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains","structure_overrides":["minecraft:strongholds","minecraft:villages"]}'
SCENARIO=; [ "$MODE" != gametest ] && SCENARIO=${2:-}
[ "$MODE" = playtest ] && SCENARIO=playtest
SCEN=(); [ -n "$SCENARIO" ] && SCEN=("-Pe2eScenario=$SCENARIO")

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
  printf 'level-type=minecraft\\:flat\ngenerator-settings=%s\nonline-mode=false\nlevel-name=world\nspawn-protection=0\nmax-tick-time=-1\n' "$FLAT" > "$SRV/server.properties"
  $GRADLEW :e2e:runServer ${SCEN[@]+"${SCEN[@]}"} > "$SRV/gradle.log" 2>&1 \
    || { echo "server run failed, see $SRV/gradle.log"; exit 1; }
  report "$SRV/logs/latest.log" server
fi

if [ "$MODE" = gametest ] || [ "$MODE" = all ]; then
  # runGametest itself deletes the old world; the server exits with the number of failed required tests
  mkdir -p "$GT"; rm -f "$GT/logs/latest.log" "$GT/gametest-report.xml"
  FILTER=(); [ "$MODE" = gametest ] && [ -n "${2:-}" ] && FILTER=("-Pe2eFilter=$2")
  VERIFY=; [ "$MODE" = gametest ] && [ "${3:-}" = verify ] && VERIFY=-Pe2eVerify
  $GRADLEW :e2e:runGametest ${FILTER[@]+"${FILTER[@]}"} $VERIFY > "$GT/gradle.log" 2>&1; rc=$?
  log=$GT/logs/latest.log
  echo "== gametest =="
  if [ ! -f "$log" ]; then
    echo "  gametest server did not start (exit $rc), see $GT/gradle.log"; exit 1
  fi
  if [ -n "$VERIFY" ]; then
    grep -E "E2E: GAMETEST (PASS|FAIL)" "$log" | sed 's/^.*E2E: GAMETEST \([A-Z(a-z)]*\) \([^ ]*\).*$/\1 \2/; s/:$//' | sort | uniq -c
    grep -m10 -E "E2E: GAMETEST FAIL" "$log" | sed 's/^.*E2E: GAMETEST /  /' | cut -c1-400
  else
    grep -E "E2E: (mixin audit|GAMETEST)" "$log" | sed 's/^.*E2E: GAMETEST /  /; s/^.*E2E: /  /' | cut -c1-400
  fi
  grep -m1 -E "GAME TESTS COMPLETE" "$log" | sed 's/^.*=\{9\} /  /; s/ =*$//'
  failed=$(grep -cE "E2E: GAMETEST FAIL " "$log")
  if ! grep -qE "E2E: GAMETEST SUMMARY" "$log"; then
    echo "  gametest run did not finish (exit $rc): last errors:"
    grep -m5 -E "Exception|Error|Caused by:|crashed" "$log" | cut -c1-300
    exit 1
  fi
  echo "  report: $GT/gametest-report.xml"
  # every test reports exactly once (multi-attempt tests after their last attempt)
  total=$(grep -m1 -E "GAME TESTS COMPLETE" "$log" | sed 's/^.*=\{9\} \([0-9]*\) GAME.*$/\1/')
  reported=$(grep -cE "E2E: GAMETEST (PASS|FAIL)" "$log")
  if [ "$reported" != "$total" ]; then
    echo "  FAILED: only $reported of $total tests reported a result"; exit 1
  fi
  if [ "$rc" -ne 0 ] || [ "$failed" -ne 0 ]; then
    echo "  FAILED: $failed required test(s) failed (exit $rc)"; exit 1
  fi
fi

if [ "$MODE" = client ] || [ "$MODE" = all ] || [ "$MODE" = play ] || [ "$MODE" = flight ] || [ "$MODE" = playtest ]; then
  # Every run starts from a copy of a clean template world. The server run saves the sub-levels it
  # spawned (in sublevels/, data/ and the plot chunks), so its world can't be reused.
  TEMPLATE=e2e/run/template-world
  if [ ! -d "$TEMPLATE" ]; then
    echo "creating clean template world (one-off server boot without spawning)..."
    rm -rf "$SRV/world"; mkdir -p "$SRV"
    echo "eula=true" > "$SRV/eula.txt"
    printf 'level-type=minecraft\\:flat\ngenerator-settings=%s\nonline-mode=false\nlevel-name=world\nspawn-protection=0\nmax-tick-time=-1\n' "$FLAT" > "$SRV/server.properties"
    $GRADLEW :e2e:runServer -Pe2ePrepare > "$SRV/gradle-template.log" 2>&1 \
      || { echo "template world creation failed, see $SRV/gradle-template.log"; exit 1; }
    cp -r "$SRV/world" "$TEMPLATE"
  fi
  mkdir -p "$CLI/saves" && rm -rf "$CLI/saves/e2e" && cp -r "$TEMPLATE" "$CLI/saves/e2e"
  # keep simulating while the window is unfocused; skip onboarding screens
  printf 'pauseOnLostFocus:false\nonboardAccessibility:false\ntutorialStep:none\nskipMultiplayerWarning:true\njoinedFirstServer:true\n' > "$CLI/options.txt"
  if [ "$MODE" = play ]; then
    $GRADLEW :e2e:runClient -Pe2ePlay ${SCEN[@]+"${SCEN[@]}"} > "$CLI/gradle.log" 2>&1
    exit 0
  fi
  rm -rf "$CLI/screenshots"
  SCENE=; [ "$MODE" = flight ] && SCENE=-Pe2eScene=flight
  $GRADLEW :e2e:runClient $SCENE ${SCEN[@]+"${SCEN[@]}"} > "$CLI/gradle.log" 2>&1 \
    || { echo "client run failed, see $CLI/gradle.log"; exit 1; }
  report "$CLI/logs/latest.log" "$MODE"
  [ "$MODE" = flight ] && grep -E "E2E: flight" "$CLI/logs/latest.log" | sed 's/^.*E2E:/  E2E:/' | head -40
  ls "$CLI/screenshots" 2>/dev/null | sed 's/^/  screenshot: /'
fi
