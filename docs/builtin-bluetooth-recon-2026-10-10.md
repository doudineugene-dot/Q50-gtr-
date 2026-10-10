# Built-in InTouch Bluetooth: primary evidence and implemented reconnaissance

## Evidence checked in this iteration

Remote branch was ahead of local checkout. Fast-forward `23e2883 -> 50cad34`
restored the owner's built-in-only constraint and earlier research. All
subsequent work follows that constraint. `codex_next_prompt.txt` described this
iteration; issue11 was read live including its Phase2 update.

1. [Official EcuTek support](https://ecutek.atlassian.net/wiki/spaces/SUPPORT/pages/1681719302/Configuring+an+EVI-BT+manually)
   classifies revision7+ as BTLE. Therefore R9 is in that category. This does
   not identify the radio/controller in the DCU or provide its protocol.
2. [Renesas engineering discussion](https://community.renesas.com/wireles-connectivity/f/bluetooth-low-energy/19963/l2cap-connection-oriented-channels-api)
   contains a `marco-ecutek` question about CoC streaming, and Renesas replies
   that SDK APIs were unavailable. Corroboration of engineering interest,
   not proof of R9 silicon or a released EcuTek wire protocol.
3. [Android BluetoothDevice API](https://developer.android.com/reference/android/bluetooth/BluetoothDevice#createL2capChannel(int))
   dates the public LE CoC method to API29 and requires the app to obtain the
   PSM from the remote. Reflection alone cannot add a missing LE host stack.
4. [AOSP Android2.3.7 native BluetoothSocket](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-2.3.7_r1/core/jni/android_bluetooth_BluetoothSocket.cpp)
   has a hidden L2CAP path using PF_BLUETOOTH and BlueZ-style headers, guarded
   by HAVE_BLUETOOTH. If compiled out, ENOSYS is returned. Its old L2CAP path
   is not an implementation of modern LE credit channels. This is why adapter
   null or hidden constructor availability is an incomplete diagnostic.
5. [2016 Q50 reverse-engineering author's process dump](https://gist.githubusercontent.com/nickdaria/570ab12f873371901f06fed9b88b3e7c/raw/2d06b13ca47be9ac2b0aa02335db0b9205f79814/q50-fs-afx)
   shows host D-Bus/connmand, `/home/naviwork/system/bin/audio_ps`, `tel_proc`,
   `cMmp`/`cMmpu`/`BTASoundRouter`, kernel L2CAP/RFCOMM threads and Android
   Ygomi/Connexis processes. No bluetoothd is listed in this snapshot.
   It is a different car/firmware; none of these processes is yet proven to
   expose EVI CoC on the owner's 2017 DCU. They are concrete inventory targets.
6. [Same author's module tree](https://gist.githubusercontent.com/nickdaria/4538950231981246aa42b227fc77cf40/raw/bc44caa8b1d73acc4e6d25f5fb1a5d905f6ded7d/q50-kernel-modules)
   names a 2.6.37.6 Intel-crossville kernel and `drivers/bt_hci`, `bt_dfu`,
   `net_adp`, plus hci_uart/hci_vhci modules. Names alone establish neither
   loaded modules nor LE/CoC support. Inspect `/proc/modules` and sysfs on-car.
7. [Same author's USB dump](https://gist.githubusercontent.com/nickdaria/7bdd8a4034528b1337ee5708e7dfd09e/raw/4e151b639b436614f1098ad62466ddc739bc1047/gistfile1.txt)
   lists `0a12:0001` HCI. It does not establish whether that device is built-in
   or externally attached, nor its BLE capability. Do not transfer that
   topology to this car or choose an external dongle from it.
8. [Author's external-driver project](https://www.reddit.com/r/q50/comments/1wyfemv/full_intouch_gen1_replacement_kinda_external_wifi/)
   explicitly uses additional USB Bluetooth/Wi-Fi hardware. Visible post and
   comments offer later code publication; no source/API/binary link was found
   there. This is excluded from the current transport scope.
9. [Official BlueZ5.43 HCI ABI](https://github.com/bluez/bluez/blob/5.43/lib/hci.h)
   provides GET ioctls/connection fields/direction options; old
   [Linux2.6.37 HCI socket ABI](https://github.com/torvalds/linux/blob/v2.6.37/include/net/bluetooth/hci.h)
   uses the family/device sockaddr prefix. The probe binds only that prefix
   as a RAW observer, avoiding later USER/monitor-channel requirements.

Owner evidence from [issue11](https://github.com/doudineugene-dot/Q50-gtr-/issues/11):
VR30 DASH0.5.4 directly connected on this DCU; Samsung log22 used changing CoC
PSMs 0x8A/0x8B/0x81/0x82. Those raw logs/APK are not present in this checkout
and were not independently decoded here. They constrain research, never
become hard-coded connection parameters. Public searches for VR30 DASH,
Ygomi Bluetooth and InTouch GitHub code did not locate a usable VR30 artifact
or a documented vendor CoC API. That is a search result, not proof of absence.

## Implementation and exact remaining requirements

`bluetooth-probe/` adds a separate API10 diagnostic package, native x86/ARM
observer, 90s background snapshots, ZIP export, a static host-Linux x86 helper
and an offline standard HCI/L2CAP framing analyzer. No EVI pairing, discovery
scan, remote socket connect or application TX is attempted. No controller
power/scan/reset changes, driver installation or hidden vendor API invocation.

The capture can recover an observed LE peer/handle and actual PSM/SCID/DCID
from VR30's live connection if RAW observation is permitted. Process maps/fd
and socket tables reveal kernel versus vendor IPC route. PSM requests and
successful responses remain separate observations until correlated by peer,
direction and connection lifetime. A counter is never ECUTEK RPM.

Smallest missing evidence:

- Android and host UID/capabilities; actual kernel/controller/sysfs/driver;
- VR30 package path/UID/native library names plus maps/fd before and during EVI;
- socket inode/owner, D-Bus/vendor service name, handle/PSM/CID and raw capture;
- verified EcuTek framing/authentication and RPM/Boost channel definitions.

One passive on-car run captures all accessible items, including explicit denied
paths. Android app privileges cannot guarantee access to host sockets/maps.
If denied, run the supplied helper from an already available host shell with
appropriate privileges; the package does not acquire root. If no observer
route exists, a legally supplied VR30 APK/EPK is the smallest remaining artifact
for static analysis. Neither app invents proprietary connect/auth packets.

Acceptance remains stock built-in BT -> EVI R9 -> source=ECUTEK changing RPM,
loss on disconnect, then changing Boost Actual with observed transport evidence.
This iteration supplies reconnaissance, not acceptance of that live gate.
