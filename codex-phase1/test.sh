#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/host artifact/render-captures
find src -name '*.java' ! -name 'MainActivity.java' ! -name 'CanvasSurface.java' | sort > build/host-sources.txt
javac --release 7 -encoding UTF-8 -d build/host @build/host-sources.txt tests/HostChecks.java
java -Djava.awt.headless=true -cp build/host HostChecks artifact/render-captures | tee artifact/host-tests.txt
python3 tests/epk_roundtrip.py | tee artifact/epk-roundtrip.txt
