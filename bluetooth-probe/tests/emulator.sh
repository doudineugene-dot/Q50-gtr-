#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
OUT=artifact/android-probe
mkdir -p "$OUT"
trap 'adb logcat -d > "$OUT/logcat.txt"' EXIT
adb install -r artifact/Q50-Codex-EcuTek-Probe.apk
adb logcat -c
adb shell am start -W -n ru.q50.ecutekprobe/.ProbeActivity --ei seconds 5
python3 ../tools/dismiss_legacy_android.py "$OUT"
sleep 3
adb shell input keyevent 66
for attempt in 1 2 3 4; do
  adb shell uiautomator dump /sdcard/probe-ui.xml
  adb pull /sdcard/probe-ui.xml "$OUT/ui-$attempt.xml"
  BUTTON=$(python3 - "$OUT/ui-$attempt.xml" <<'PY'
import re,sys,xml.etree.ElementTree as ET
for node in ET.parse(sys.argv[1]).iter('node'):
    if node.get('resource-id') in ('com.android.permissioncontroller:id/continue_button','android:id/button1'):
        a,b,c,d=map(int,re.findall(r'\d+',node.get('bounds')));print((a+c)//2,(b+d)//2);break
PY
  )
  [ -n "$BUTTON" ] || break
  read -r X Y <<< "$BUTTON"
  adb shell input tap "$X" "$Y"
  sleep 2
done
# Relaunch with the test interval after system dialogs so the background test
# starts in the application, not in ReviewPermissionsActivity.
adb logcat -c
adb shell am start -W -n ru.q50.ecutekprobe/.ProbeActivity --ei seconds 5
sleep 1
adb shell input keyevent 3
for n in $(seq 1 45); do
  if adb logcat -d -s Q50BtProbe:I | grep -q 'COMPLETE '; then break; fi
  sleep 1
done
adb logcat -d -s Q50BtProbe:I | grep -q 'READY ABI=x86 JNI=null'
adb logcat -d -s Q50BtProbe:I | grep -q 'COMPLETE '
adb pull /sdcard/Q50-EcuTek-Probe "$OUT/exports"
python3 - "$OUT" <<'PY'
import json,pathlib,sys,zipfile
root=pathlib.Path(sys.argv[1]);archives=list(root.glob('exports/*.zip'));assert archives
with zipfile.ZipFile(archives[-1]) as z:
    assert 'COMPLETE.txt' in z.namelist()
    summary=z.read('SUMMARY.txt').decode();assert 'ECUTEK RPM=UNAVAILABLE' in summary
    native=z.read('native.txt').decode();assert 'finished result=' in native
    inventory=z.read('inventory-0.txt').decode();assert 'JNI loadError=null' in inventory
    assert 'processes-1.txt' in z.namelist()
    (root/'native.txt').write_text(native)
print('PASS: JNI loaded, service completes while activity is backgrounded, exported ZIP contains inventory/native evidence; EcuTek telemetry unavailable')
PY
adb shell am start -W -n ru.q50.ecutekprobe/.ProbeActivity
adb exec-out screencap -p > "$OUT/probe.png"
adb logcat -d > "$OUT/logcat.txt"
if grep -q 'FATAL EXCEPTION' "$OUT/logcat.txt"; then exit 1; fi
echo 'PASS: API29 x86 emulator only; physical API10/DCU and EVI remain untested' > "$OUT/results.txt"
