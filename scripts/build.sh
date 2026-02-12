#!/bin/bash
# Build SurvivalCore server JAR.
# Usage: ./scripts/build.sh [--skip-patches]

set -euo pipefail

SKIP_PATCHES=false
for arg in "$@"; do
    case $arg in
        --skip-patches) SKIP_PATCHES=true ;;
        *) echo "Unknown argument: $arg"; exit 1 ;;
    esac
done

echo "Building SurvivalCore..."

if [ "$SKIP_PATCHES" = false ]; then
    echo "Applying patches..."
    ./gradlew applyAllPatches
fi

echo "Building server JAR..."
./gradlew createMojmapPaperclipJar

JAR=$(ls build/libs/survivalcore-paperclip-*-mojmap.jar 2>/dev/null | head -1)
if [ -n "$JAR" ]; then
    echo ""
    echo "Build successful: $JAR"
    echo "Size: $(du -h "$JAR" | cut -f1)"
else
    echo "Build failed - no JAR produced"
    exit 1
fi
