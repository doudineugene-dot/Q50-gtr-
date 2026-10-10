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


## Transport constraint — built-in DCU Bluetooth only

User has clarified that the target implementation is already known to work **without any external Bluetooth dongle**.

Therefore the EcuTek path for this project is strictly:

**stock DCU built-in Bluetooth -> EVI-BT R9 -> live EcuTek telemetry**

Do not spend development time on:
- external USB Bluetooth dongles;
- USB Bluetooth drivers;
- phone bridges;
- UDP bridges;
- screen mirroring.

The existence of VR30 DASH 0.5.4 on this same stock DCU is the key proof-of-feasibility and should remain the primary reverse-engineering reference.

Immediate research priority:
1. Identify the exact built-in Bluetooth stack/vendor path available on the DCU.
2. Determine how VR30 DASH accesses it despite API10 public BLE limitations.
3. Reproduce EVI discovery/connection read-only.
4. Obtain real EcuTek RPM.
5. Obtain real Boost Actual.
6. Only after those are proven, expand channels and then move to Air Lift.

The final success gate remains real EcuTek RPM + Boost Actual on the stock DCU using the built-in Bluetooth subsystem only.

## Итерация 2026-10-10

Отдельный diagnostic APK/EPK: `codex-phase1/ProbeManifest.xml`, package `ru.q50.codexprobe`; сборка `Q50_PROBE=1 bash codex-phase1/build.sh`. Выход Q50-Codex-EcuTek-Probe.apk/.epk. Инструкция: `docs/DCU-PROBE-NEXT-TEST.md`.

Транспорт/ECUTEK RPM/Boost остаются UNPROVEN. Не подтверждены UUID, получение динамического PSM, handshake и команды чтения. Probe собирает actual native HCI/socket/errno, vendor API signatures, PID/FD/maps, доступный VR30 APK и passive HCI. Это диагностическая поставка, не прохождение blocking milestone. Следующий код определяется этими артефактами, без внешнего BT и без ECU writes.
