#!/usr/bin/env bash
set -euo pipefail
set -x
cd "$(dirname "$0")/.."
OUT=artifact/android-captures
mkdir -p "$OUT"
trap 'adb logcat -d > "$OUT/logcat.txt"' EXIT
adb shell wm size 840x480
adb shell wm density 160
adb shell settings put global policy_control immersive.full=*
adb install -r artifact/Q50-Codex-Phase1.apk
adb logcat -c
adb shell am start -W -n ru.q50.codex/.MainActivity
sleep 3
# API 29 shows its system warning for targetSdk 10 on first launch. Dismiss
# that dialog before testing application touches; never raise the app target.
adb shell input keyevent 66
sleep 1
adb shell dumpsys window windows > "$OUT/window-start.txt"
if grep -E 'mCurrentFocus=.*DeprecatedTargetSdkVersionDialog' "$OUT/window-start.txt"; then
  adb shell input keyevent 66
  sleep 1
fi
# Use actual measured content dimensions, rather than assuming navigation bar size.
SIZE=$(adb logcat -d -s Q50Codex:I | sed -n 's/.*view=\([0-9]*x[0-9]*\).*/\1/p' | tail -1)
[ -n "$SIZE" ]
W=${SIZE%x*}; H=${SIZE#*x}
S=$(awk -v w="$W" -v h="$H" 'BEGIN {a=w/840;b=h/480;print a<b?a:b}')
TAB_Y=$(awk -v h="$H" -v s="$S" 'BEGIN {printf "%.0f",h-28*s}')
MODE_X=$(awk -v w="$W" -v s="$S" 'BEGIN {printf "%.0f",w-120*s}')
TOP_Y=$(awk -v s="$S" 'BEGIN {printf "%.0f",26*s}')
# Some API 29 images leave the non-focusable compatibility warning after
# ENTER. A tap outside dismisses it. Use the already selected engine tab,
# so the same tap is harmless if the dialog was already dismissed.
FIRST_X=$(awk -v w="$W" -v s="$S" 'BEGIN {printf "%.0f",12*s+(w-24*s)/6}')
adb shell input tap "$FIRST_X" "$TAB_Y"
sleep 2
adb shell uiautomator dump /sdcard/q50-start.xml
adb pull /sdcard/q50-start.xml "$OUT/window-start.xml"
if grep -q 'android:id/button1' "$OUT/window-start.xml"; then
  # Close the actual positive system-dialog button using its measured bounds.
  read -r BX BY < <(python3 - "$OUT/window-start.xml" <<'PY'
import re, sys, xml.etree.ElementTree as ET
for node in ET.parse(sys.argv[1]).iter('node'):
    if node.get('resource-id') == 'android:id/button1':
        x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
        print((x1+x2)//2,(y1+y2)//2)
        break
PY
  )
  adb shell input tap "$BX" "$BY"
  sleep 1
fi
for mode in live demo; do
  for page in 0 1 2; do
    X=$(awk -v w="$W" -v s="$S" -v p="$page" 'BEGIN {printf "%.0f",12*s+(w-24*s)/3*(p+0.5)}')
    adb shell input tap "$X" "$TAB_Y"
    sleep 1
    adb exec-out screencap -p > "$OUT/$mode-$page.png"
    adb logcat -d -s Q50Codex:I | grep -q "page=$page mode=$(echo "$mode" | tr a-z A-Z)"
  done
  adb shell input tap "$MODE_X" "$TOP_Y"
  sleep 1
done
# Return to LIVE after DEMO, then toggle diagnostics and verify lifecycle resume.
adb logcat -d -s Q50Codex:I | tail -1 | grep -q 'mode=LIVE'
adb exec-out screencap -p > "$OUT/live-after-demo.png"
DIAG_X=$(awk -v w="$W" -v s="$S" 'BEGIN {printf "%.0f",w-35*s}')
adb shell input tap "$DIAG_X" "$TOP_Y"
sleep 1
adb logcat -d -s Q50Codex:I | tail -1 | grep -q 'diag=false'
adb exec-out screencap -p > "$OUT/live-diag-off.png"
adb shell input keyevent 3
adb shell am start -W -n ru.q50.codex/.MainActivity
sleep 1
adb shell pidof ru.q50.codex
adb logcat -d > "$OUT/logcat.txt"
if grep -q 'FATAL EXCEPTION' "$OUT/logcat.txt"; then exit 1; fi
echo "PASS: Android API 29 emulator; measured View $SIZE; all 3 LIVE + DEMO pages, LIVE after DEMO, DIAG toggle, pause/resume; not an API 10/DCU test" | tee "$OUT/results.txt"
adb install -r artifact/Q50-Codex-DCU-Recon.apk
adb shell pm grant ru.q50.codexprobe android.permission.WRITE_EXTERNAL_STORAGE
adb shell am start -W -n ru.q50.codexprobe/ru.q50.codex.ProbeActivity
python3 ../tools/dismiss_legacy_android.py "$OUT"
adb shell input keyevent 66
sleep 2
adb shell input keyevent 66
for i in $(seq 1 30); do
  adb logcat -d -s Q50Probe:I | grep -q 'COMPLETE ' && break
  sleep 2
done
adb logcat -d -s Q50Probe:I | grep -q 'COMPLETE '
adb pull /sdcard/Q50-DCU-bluetooth-report.txt "$OUT/dcu-bluetooth-report.txt"
adb pull /sdcard/Q50-DCU-evidence/hci-passive.txt "$OUT/hci-passive.txt"
grep -q 'native v1' "$OUT/dcu-bluetooth-report.txt"
grep -q 'snapshot 2' "$OUT/dcu-bluetooth-report.txt"
grep -q 'RPM/BOOST_ACTUAL remain UNPROVEN' "$OUT/dcu-bluetooth-report.txt"
adb exec-out screencap -p > "$OUT/probe.png"
adb logcat -d > "$OUT/logcat.txt"
if grep -q 'FATAL EXCEPTION' "$OUT/logcat.txt"; then exit 1; fi
echo 'PASS: dedicated probe lifecycle, JNI, 3 snapshots, passive HCI outcome, shared-storage export; no DCU/EVI proof' | tee "$OUT/probe-results.txt"
