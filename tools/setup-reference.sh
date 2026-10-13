#!/bin/bash
# Rebuilds the gitignored reference/ directory: decompiled Minecraft 1.21.11 + 1.21.1 sources
# and the upstream projects this port is based on. Takes ~5-10 minutes on first run.
# Usage: tools/setup-reference.sh   (from the repo root; needs git, curl, unzip, JDK 21)
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
REF=$ROOT/reference
mkdir -p "$REF"
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
  export JAVA_HOME=$(/usr/libexec/java_home -v 21)
fi

clone() { # <dir> <url> <ref>
  [ -d "$REF/$1" ] || git clone -q --depth 1 --branch "$3" "$2" "$REF/$1"
}
clone sable-1.21.1       https://github.com/ryanhcode/sable main
clone simulated-1.21.1   https://github.com/Creators-of-Aeronautics/Simulated-Project main
clone create-fly-1.21.11 https://github.com/ZurrTum/Create-Fly v6.0.9-1.21.11-5
clone veil-1.21.1        https://github.com/FoundryMC/Veil 1.21
clone fabric-api-1.21.11 https://github.com/FabricMC/fabric-api 0.141.6+1.21.11

extract_mc() { # <project dir> <mc version> <out dir>
  local jar
  jar=$(find "$1/.gradle" -name "minecraft-merged*-$2-*-sources.jar" 2>/dev/null | head -1)
  mkdir -p "$3" && (cd "$3" && unzip -qo "$jar")
}

if [ ! -d "$REF/mc-1.21.11" ]; then
  (cd "$ROOT" && ./gradlew :sable:genSources -x :sable:validateAccessWidener --console=plain -q)
  extract_mc "$ROOT" 1.21.11 "$REF/mc-1.21.11"
fi

if [ ! -d "$REF/mc-1.21.1" ]; then
  P=$REF/mc-1.21.1-project
  mkdir -p "$P"
  cp "$ROOT/tools/mc-1.21.1-sources/build.gradle" "$ROOT/tools/mc-1.21.1-sources/settings.gradle" "$P/"
  cp -r "$ROOT/gradle" "$ROOT/gradlew" "$P/"
  (cd "$P" && ./gradlew genSources --console=plain -q)
  extract_mc "$P" 1.21.1 "$REF/mc-1.21.1"
fi

echo "reference/ ready:"; ls "$REF"
