# Задача Codex — Phase 2: direct EcuTek transport

## Итог автономной итерации 2026-10-11

Исследование и diagnostic stage выполнены; live milestone ниже остаётся открытым.
Поставка: 90с `Q50-Codex-EcuTek-Probe.apk/.epk`, static `Q50-btprobe-linux-x86`,
offline HCI/ACL/LE CoC analyzer; дополнительный 30с `Q50-Codex-DCU-Recon.apk/.epk`
с доступным VR30 APK export. Оба build/emulator CI SUCCESS, ссылки и SHA256 в
[TEST_RESULTS.md](TEST_RESULTS.md). Основной tested commit `07f095238fcdc9a34e2a9b8d9a88a62f37e649c5`.

Следующая итерация начинается с [одного install/run/export](docs/BLUETOOTH-PROBE-NEXT-TEST.md),
не с догадок о BluetoothAdapter. Нужны фактические PID/maps/socket/namespace,
controller/handle и CoC signalling во время подтверждённого VR30 RPM, либо его
легально полученный APK/EPK с actual vendor binding. При EPERM/hidden host:
static helper + host inventory только через уже доступный authorized shell;
нет root acquisition, драйверов или внешнего Bluetooth. После доказанного transport
изучить discovery динамического PSM, authentication/framing, read-only RPM channel
и disconnect/stale; только затем Boost Actual и absolute/gauge units.

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
- Q50-Codex-DCU-Recon.apk
- Q50-Codex-DCU-Recon.epk

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

Дополнение к диагностике: `bluetooth-probe/` — изолированный 90-секундный
observer `ru.q50.ecutekprobe` с отдельным workflow `bluetooth-probe.yml`.
Включает HCIGETCONNLIST, полный ограниченный JSONL HCI capture, offline ACL
reassembly/LE CoC PSM/CID analysis и статический legacy x86 host helper.
Тест этой поставки: `docs/BLUETOOTH-PROBE-NEXT-TEST.md` (пять действий).
Существующий 30-секундный `ru.q50.codexprobe` сохранён как отдельный инструмент;
их APK различаются именами, package и workflow provenance: 90с —
Q50-Codex-EcuTek-Probe, 30с — Q50-Codex-DCU-Recon.
Для следующего автомобильного теста использовать 90-секундный observer из
артефакта **Q50-BuiltIn-Bluetooth-Probe-APK-EPK**. Live gate не изменён.

Отдельный diagnostic APK/EPK: `codex-phase1/probe/AndroidManifest.xml`, package `ru.q50.codexprobe`; сборка `Q50_PROBE=1 bash codex-phase1/build.sh`. Выход Q50-Codex-DCU-Recon.apk/.epk. Инструкция: `docs/DCU-PROBE-NEXT-TEST.md`.

Транспорт/ECUTEK RPM/Boost остаются UNPROVEN. Не подтверждены UUID, получение динамического PSM, handshake и команды чтения. Probe собирает actual native HCI/socket/errno, vendor API signatures, PID/FD/maps, доступный VR30 APK и passive HCI. Это диагностическая поставка, не прохождение blocking milestone. Следующий код определяется этими артефактами, без внешнего BT и без ECU writes.
