# Built-in DCU Bluetooth reconnaissance

Separate package `ru.q50.ecutekprobe`, minSdk/targetSdk 10/10. This iteration
does not connect to EVI or decode EcuTek telemetry. RPM and Boost Actual remain
UNAVAILABLE. No stock RPM fallback or synthetic samples.

The 90-second foreground service survives switching to VR30 DASH and collects
four inventories (nominally 0/30/60/90 s). Native HCI observation runs alongside
inventory, with connection/device GET ioctls every 15 s. Inventory commands
have a 3 s deadline and bounded output. A slow device can finish later than
90 seconds; wait for “Готово”. App restart after process death does not pretend
an interrupted capture is complete. Logs retain each denial/absence.

Collected evidence:

- Android version/ABI, adapter state and bonded metadata (bonded is not connected).
- Framework method signatures, installed package label/version/UID/APK path,
  services/permissions and DEX/native entry names for relevant packages.
- `/proc` Bluetooth and Unix sockets; relevant process command/status/maps/fd
  symlinks to correlate inode with PID; vendor host paths and controller sysfs.
- Native socket availability for L2CAP, RFCOMM and HCI RAW; HCIGETDEVLIST,
  HCIGETDEVINFO cached feature bits/counters and HCIGETCONNLIST handles/remotes.
- Passive RAW observer with legacy 4-byte sockaddr_hci, events and ACL only.
  No remote connect, HCI command, scan, reset, enable, driver load, USER channel,
  unknown vendor Binder transaction, D-Bus method, root request, or ECU write.
- Raw full HCI packets as JSONL with monotonic timestamp, controller ID and
  direction from ancillary data; direction -1 remains unknown. File cap 16 MiB.

`SUMMARY.txt` is intentionally an unproven EcuTek gate. `native.txt` and the
offline analyzer can report generic transport observations, never EVI discovery,
EVI connection, or RPM without independent peer/protocol evidence. Cached LE
bit does not establish usable CoC; a local L2CAP socket does not establish LE
support; generic controller RX counters include other Bluetooth traffic.

Archives may contain device addresses, app metadata and Bluetooth payloads.
Keep these local; the probe has no Internet permission and does not upload.
It does not copy proprietary APKs, pairing key stores or phone databases.
ZIP export requires WRITE_EXTERNAL_STORAGE; passive public metadata requires
BLUETOOTH. BLUETOOTH_ADMIN and vehicle permissions are absent. These Android
permissions do not grant CAP_NET_RAW or host process access.

On completion a ZIP is saved privately and attempted under
`<external-storage>/Q50-EcuTek-Probe/run-<epoch-ms>.zip`. “ЭКСПОРТ НА USB” uses
actual FAT/exFAT/FUSE mounts containing USB or `/media/`; if mounts are hidden
from the Android namespace it reports failure and preserves the private ZIP.
No speculative removable path is written. Files are not automatically deleted.

## Build / analysis

Linux JDK17, build-tools33.0.2, Python3.12+cryptography44.0.2, gcc-multilib,
official NDK r16b (archive SHA1 `42aa43aae89a50d1c66c3f9fdecd676936da6128`):

```sh
bash bluetooth-probe/test.sh
Q50_NDK=/path/to/android-ndk-r16b bash bluetooth-probe/build.sh
python3 bluetooth-probe/tools/analyze_hci.py /path/to/unpacked/hci-packets.jsonl
```

CI builds x86/ARM JNI against android-9, verifies ELF32 and DEX035, v1-only
signing and EPK type2/name `q50probe.apk`. APK label/package are separate from
both production and the Phase1 dashboard. Persistent signing uses existing CI
secrets; otherwise temporary is explicit. API29 x86 emulator checks actual JNI
load, background completion, denials, and exported ZIP. It cannot prove API10,
DCU permissions, controller type, EVI discovery, or EcuTek transport.

`Q50-btprobe-linux-x86` is a static ELF32 host-Linux fallback from the same C
source. With an already available host shell, run it as the current user:

```sh
mkdir -p /actual/writable/path/q50-bt
./Q50-btprobe-linux-x86 /actual/writable/path/q50-bt 90
```

The executable statically links legacy API9 bionic and has no ELF interpreter;
it does not depend on host glibc or libbluetooth. This avoids compiling against
modern glibc's kernel floor on the reported 2.6.37 host. CI executes it on Linux;
real DCU compatibility still needs testing.

This does not obtain root. If Android lacks host visibility or raw privileges,
the exact next requirement is a capture from an existing authorized host root
shell, or a legally supplied VR30 DASH APK/EPK. No privilege escalation or
external controller is installed.

The analysis tool reconstructs ACL fragments per controller/direction/handle,
reads standard LE connection/disconnection events, CID5 credit-channel request
and response fields, and calculates generic PDU rates. A request is explicitly
not a successful connection. It does not guess how EcuTek advertises the PSM,
pairing/authentication, framing, RPM or Boost scaling. Synthetic framing tests
are Bluetooth standards fixtures, not EcuTek packet samples.

Next test: [five actions](../docs/BLUETOOTH-PROBE-NEXT-TEST.md).
