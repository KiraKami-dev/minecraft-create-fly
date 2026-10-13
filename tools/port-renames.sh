#!/bin/bash
# Mechanical 1.21.1 -> 1.21.11 (Mojang mappings) class renames/moves.
# Usage: tools/port-renames.sh <dir>...   (rewrites .java, .json and .accesswidener files in place)
set -euo pipefail
[ $# -ge 1 ] || { echo "usage: $0 <dir>..." >&2; exit 1; }

# dotted and slashed (descriptor) forms of each move
MOVES=(
  "net.minecraft.resources.ResourceLocation|net.minecraft.resources.Identifier"
  "net.minecraft.Util|net.minecraft.util.Util"
  "net.minecraft.world.entity.projectile.AbstractArrow|net.minecraft.world.entity.projectile.arrow.AbstractArrow"
  "net.minecraft.world.entity.projectile.Arrow|net.minecraft.world.entity.projectile.arrow.Arrow"
  "net.minecraft.client.renderer.RenderType|net.minecraft.client.renderer.rendertype.RenderType"
  "net.minecraft.advancements.critereon|net.minecraft.advancements.criterion"
)

files=$(find "$@" -type f \( -name '*.java' -o -name '*.json' -o -name '*.accesswidener' \))
for m in "${MOVES[@]}"; do
  from=${m%%|*}; to=${m##*|}
  fromS=${from//./\/}; toS=${to//./\/}
  # \b after the class name so e.g. "Util" doesn't match "UtilFoo"
  perl -pi -e "s/\Q$from\E\b/$to/g; s/\Q$fromS\E\b/$toS/g" $files
done
# simple-name renames
perl -pi -e 's/\bResourceLocation\b/Identifier/g; s/\bresourceLocation\b/identifier/g' $files
