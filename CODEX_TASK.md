# Задача Codex — Phase 2: direct EcuTek transport

Phase 1 infrastructure is accepted. Do **not** spend the next iteration on final graphics or a car install that has no EcuTek data.

Read first:
- issue #11
- `docs/ecutek-research-2026-10-10.md`
- `docs/bluetooth-findings.md`
- `PROJECT_STATE.md`

## Blocking milestone

Build a dedicated read-only EcuTek probe for the stock Infiniti InTouch DCU:

**stock DCU -> EVI-BT R9 -> real EcuTek RPM**

The user's EVI is Rev 9, therefore official EcuTek documentation classifies it as EVI-BTLE (Rev 7+). Existing user logs also showed ECU Connect LE L2CAP CoC with dynamic PSM values.

VR30 DASH 0.5.4 is real-world evidence that this exact stock DCU can connect directly to EcuTek without phone, USB, UDP or mirroring.

Therefore:
- do not stop at API10 public Bluetooth limitations;
- do not hard-code a PSM;
- investigate vendor/native/HCI/BlueZ/Unix-socket paths actually present on the DCU;
- if VR30 DASH package can be obtained, statically analyse its manifest/dex/native libraries and Bluetooth calls.

## Deliverable

Create:
- Q50-Codex-EcuTek-Probe.apk
- Q50-Codex-EcuTek-Probe.epk

Diagnostic UI must show:
- EVI DISCOVERED
- EVI CONNECTED
- transport
- remote identifier/address
- HCI handle if available
- PSM/CID if applicable
- RX bytes/sec
- RX packets/sec
- last packet age
- rate-limited raw RX hex
- ECUTEK RPM
- ECUTEK BOOST ACTUAL

No stock InTouch fallback on this probe screen.

## Success criteria

1. ECUTEK RPM changes with engine speed.
2. Disconnecting/powering off EVI makes ECUTEK RPM stale/unavailable.
3. BOOST ACTUAL changes live.
4. Evidence/logs are written to `TEST_RESULTS.md` and `docs/bluetooth-findings.md`.

No ECU writes, flashing, map switching, DTC clearing or protocol-byte invention.
Do not proceed to Air Lift until this gate passes.


## New verified transport option — external USB Bluetooth

Read `docs/intouch-gen1-external-leads.md`.

A recent InTouch Gen1 project has now publicly demonstrated:
- custom Bluetooth drivers;
- an external Bluetooth dongle attached to the DCU;
- direct connection from InTouch Gen1 to a Bluetooth OBD adapter;
- external Wi-Fi dongle + custom drivers;
- updated TCP stack.

This gives us a second standalone route that does NOT use a phone bridge:

**Path A (preferred):** stock DCU Bluetooth -> EVI-BT R9.

**Path B (acceptable fallback):** DCU USB host -> external USB Bluetooth controller -> EVI-BT R9.

Do not confuse generic OBD classic Bluetooth success with EVI R9 support. EVI R9 is BTLE, and our target transport is LE L2CAP CoC.

Immediate Codex research tasks:
1. Continue Path A reverse engineering.
2. In parallel, inspect the DCU/kernel/project dumps for USB host, bluetooth USB/HCI drivers, BlueZ/native HCI pieces, kernel modules and firmware.
3. Search for the Reddit author's promised source release/how-to and exact dongle chipset.
4. If the stock BT path remains blocked, design an external-dongle probe that can enumerate HCI capabilities and determine whether BLE + LE CoC can be supported.
5. Do not fall back to phone/UDP unless both standalone paths are proven impossible.

The final success gate remains real EcuTek RPM + Boost Actual on the stock DCU.
