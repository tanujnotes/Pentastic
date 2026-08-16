#!/bin/bash
# Builds the macOS installer DMG: Pentastic.app + Applications drop-link on an
# instruction background. Replaces the plain DMG jpackage produces.
#
# Requires dmgbuild (`pip install dmgbuild`, or point DMGBUILD at the binary).
set -euo pipefail

cd "$(dirname "$0")/../.."
DMGBUILD="${DMGBUILD:-dmgbuild}"
VERSION="1.0.0"
OUT="composeApp/build/compose/binaries/main/dmg/Pentastic-$VERSION.dmg"

./gradlew :composeApp:createDistributable
mkdir -p "$(dirname "$OUT")"
rm -f "$OUT"
"$DMGBUILD" -s composeApp/dmg/settings.py Pentastic "$OUT"
echo "DMG written to $OUT"
