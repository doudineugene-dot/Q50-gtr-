#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/host artifact
javac --release 7 -encoding UTF-8 -d build/host src/ru/q50/ecutekprobe/EvidenceIO.java tests/EvidenceChecks.java
java -cp build/host ru.q50.ecutekprobe.EvidenceChecks build/evidence-tests | tee artifact/evidence-tests.txt
python3 -m unittest discover -s tests -v 2>&1 | tee artifact/hci-tests.txt
gcc -DSTANDALONE -std=c99 -O2 -Wall -Wextra -Werror -Wno-misleading-indentation native/btprobe.c -o build/host/btprobe
mkdir -p artifact/host-native
# An absent/denied HCI socket is a valid diagnostic outcome on the runner.
set +e
build/host/btprobe artifact/host-native 1
RESULT=$?
set -e
test "$RESULT" -eq 0 -o "$RESULT" -eq 5 -o "$RESULT" -eq 6
grep -q 'finished result=' artifact/host-native/native.txt
python3 tools/analyze_hci.py artifact/host-native/hci-packets.jsonl > artifact/host-native/framing.json
echo 'PASS: native observer executes on Linux; this is not a DCU transport test' | tee artifact/native-tests.txt
