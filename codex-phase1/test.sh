#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/host artifact/render-captures
find src -name '*.java' ! -name 'MainActivity.java' ! -name 'CanvasSurface.java' ! -name 'ProbeService.java' ! -name 'ProbeActivity.java' | sort > build/host-sources.txt
javac --release 7 -encoding UTF-8 -d build/host @build/host-sources.txt tests/HostChecks.java
java -Djava.awt.headless=true -cp build/host HostChecks artifact/render-captures | tee artifact/host-tests.txt
javac --release 7 -encoding UTF-8 -cp build/host -d build/host tests/ProbeChecks.java
java -cp build/host ProbeChecks | tee artifact/probe-tests.txt
if [ "$(uname -s)" = Linux ]; then
  gcc -std=c99 -D_POSIX_C_SOURCE=200809L -Wall -Wextra -Werror -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" tests/native_checks.c -o build/host/native-checks
  build/host/native-checks | tee artifact/native-tests.txt
fi
python3 tests/epk_roundtrip.py | tee artifact/epk-roundtrip.txt
