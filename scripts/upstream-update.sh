#!/bin/bash
# Updates the Paper upstream reference to the latest commit on the target branch.
# Usage: ./scripts/upstream-update.sh [branch]
# Default branch: ver/1.21.8

set -euo pipefail

BRANCH="${1:-ver/1.21.8}"
PROPS_FILE="gradle.properties"

echo "Fetching latest Paper commit on branch: $BRANCH"

LATEST_HASH=$(git ls-remote https://github.com/PaperMC/Paper.git "refs/heads/$BRANCH" | cut -f1)

if [ -z "$LATEST_HASH" ]; then
    echo "Failed to fetch commit hash for branch '$BRANCH'"
    exit 1
fi

CURRENT_HASH=$(grep "^paperRef=" "$PROPS_FILE" | cut -d'=' -f2)

if [ "$CURRENT_HASH" = "$LATEST_HASH" ]; then
    echo "Already up to date: $CURRENT_HASH"
    exit 0
fi

echo "Current: $CURRENT_HASH"
echo "Latest:  $LATEST_HASH"
echo ""

sed -i "s/^paperRef=.*/paperRef=$LATEST_HASH/" "$PROPS_FILE"
echo "Updated paperRef in $PROPS_FILE"
echo ""
echo "Next steps:"
echo "  1. ./gradlew applyAllPatches"
echo "  2. Resolve any conflicts in paper-server/"
echo "  3. ./gradlew rebuildPatches"
echo "  4. Test the build: ./gradlew createMojmapPaperclipJar"
echo "  5. Commit the changes"
