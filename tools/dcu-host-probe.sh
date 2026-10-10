#!/bin/sh
# Optional fallback only through an EXISTING authorized host shell; no su,
# rooting, controller resets, pairing database access or software installation.
set -eu
[ "$#" = 1 ] || { echo 'usage: sh dcu-host-probe.sh /absolute/writable/output-directory'; exit 2; }
case "$1" in /*) ;; *) echo 'absolute output path required'; exit 2;; esac
OUT="$1/q50-host-$(date +%Y%m%d-%H%M%S)-$$"
mkdir "$OUT"
for name in version modules mounts net/unix net/bluetooth/l2cap net/bluetooth/rfcomm net/bluetooth/hci; do
  cat "/proc/$name" > "$OUT/$(echo "$name" | tr / _).txt" 2>&1 || true
done
ps > "$OUT/ps.txt" 2>&1 || true
for dir in /sys/class/bluetooth /dev /usr/lib /usr/sbin /home/naviwork/system/bin /home/naviwork/system/lib /etc/dbus-1/system.d; do
  ls -l "$dir" >> "$OUT/paths.txt" 2>&1 || true
done
for pid in /proc/[0-9]*; do
  [ -r "$pid/cmdline" ] || continue
  cmd=$(tr '\000' ' ' < "$pid/cmdline")
  case "$cmd" in *bluetooth*|*bluez*|*ygomi*|*connexis*|*vr30*|*VR30*|*tel_proc*|*dbus*|*audio_ps*|*carwings*)
    echo "PID $pid $cmd" >> "$OUT/processes.txt"
    cat "$pid/status" "$pid/maps" >> "$OUT/processes.txt" 2>&1 || true
    ls -l "$pid/fd" "$pid/root" "$pid/exe" >> "$OUT/processes.txt" 2>&1 || true;;
  esac
done
# DBus daemon introspection only, no vendor method invocation.
if command -v dbus-send >/dev/null 2>&1; then
  dbus-send --system --print-reply --reply-timeout=2000 --dest=org.freedesktop.DBus / org.freedesktop.DBus.ListNames > "$OUT/dbus-names.txt" 2>&1 || true
fi
echo "$OUT"
