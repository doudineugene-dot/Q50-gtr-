#!/usr/bin/env bash
# Рендерит UI тем же кодом, что идёт в APK, поверх Java2D-подмены android.graphics.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${1:-$ROOT/docs/preview}"
BUILD="$(mktemp -d)"
trap 'rm -rf "$BUILD"' EXIT

find "$ROOT/tools/preview" "$ROOT/src" -name "*.java" ! -name MainActivity.java > "$BUILD/sources.txt"
javac -nowarn -encoding UTF-8 -d "$BUILD/classes" @"$BUILD/sources.txt" 2>&1 | grep -v '^Note:' || true
mkdir -p "$OUT"
java -Djava.awt.headless=true -cp "$BUILD/classes" Render "$OUT"
