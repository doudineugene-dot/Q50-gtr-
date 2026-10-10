# Штатный Bluetooth DCU — 2026-10-10

По issue #11 владелец наблюдал VR30 DASH 0.5.4, соединённый напрямую с EcuTek на штатном DCU, без телефона/USB/UDP/зеркалирования. Это свидетельство владельца; сетевые логи, UUID/PSM/каналы и пакеты в этой фазе не получены.

Отрицательный результат стандартного BluetoothAdapter не доказывает невозможность Bluetooth на DCU. Фаза 3 должна исследовать vendor/hidden Android API, native/JNI, AF_BLUETOOTH, L2CAP/RFCOMM/HCI, BlueZ, Ygomi/Infiniti services/daemons, /proc/net/bluetooth и принадлежность сокетов PID.

Dashboard Phase1 не открывает Bluetooth; отдельный probe описан ниже. EcuTekSource остаётся UNAVAILABLE. Нет вымышленных UUID/PSM/telemetry или управляющих команд.

## Проверенные источники и последствия

- [EcuTek Rev7+](https://ecutek.atlassian.net/wiki/spaces/SUPPORT/pages/1681719302/Configuring+an+EVI-BT+manually): EVI-BTLE, LE добавляется из приложения. R9/VR30 0.5.4 и Samsung log22 dynamic PSM — evidence пользователя, исходный capture/APK отсутствует в tracked checkout.
- [Android LE CoC](https://developer.android.com/reference/android/bluetooth/BluetoothDevice#createL2capChannel(int)): API29; PSM приложение получает от remote device. Reflection API10 не создаёт поддержку CoC.
- [Renesas marco-ecutek](https://community.renesas.com/wireles-connectivity/f/bluetooth-low-energy/19963/l2cap-connection-oriented-channels-api): обсуждение CoC вместо GATT/notifications и credit flow control. Ответ: SDK DA14531 не содержит такого public API. Corroboration архитектуры, не идентификация чипа R9/его протокола.
- [Gen1 reverse engineer](https://www.reddit.com/r/CarHacking/comments/tjls9z/): другой DCU описан как Atom/MeeGo с Android subsystem. Immutable primary artifacts: [processes](https://gist.githubusercontent.com/nickdaria/570ab12f873371901f06fed9b88b3e7c/raw/2d06b13ca47be9ac2b0aa02335db0b9205f79814/q50-fs-afx), [USB](https://gist.githubusercontent.com/nickdaria/7bdd8a4034528b1337ee5708e7dfd09e/raw/4e151b639b436614f1098ad62466ddc739bc1047/gistfile1.txt), [modules](https://gist.githubusercontent.com/nickdaria/4538950231981246aa42b227fc77cf40/raw/bc44caa8b1d73acc4e6d25f5fb1a5d905f6ded7d/q50-kernel-modules).

Process dump показывает system DBus, `/home/naviwork/system/bin/tel_proc`, ioapf_proc/PS_VRD01, `/sbin/android.sh`; Android com.ygomi.factoryconfig/packageprovider/otaconfig, com.connexis.launcher/syncclient/wcspoller. Это реальные search targets, не подтверждённый BT API. Modules: bt_hci.ko/bt_dfu.ko/hci_uart.ko. USB CSR 0a12:0001 HCI не доказывает принадлежность встроенному радио или BLE support. Внешний BT не предлагается.

[Linux v2.6.37 L2CAP](https://raw.githubusercontent.com/torvalds/linux/v2.6.37/include/net/bluetooth/l2cap.h): legacy sockaddr_l2 без address type, BASIC/ERTM/STREAMING, без LE credit-based режима. Чужой kernel version не исключает vendor backports/пользовательский стек. [BlueZ HCI UAPI](https://github.com/bluez/bluez/blob/master/lib/bluetooth/hci.h) задаёт ioctl/filter ABI probe. Socket success не доказывает CoC.

## Реализация

`codex-phase1/ProbeManifest.xml`, отдельный package ru.q50.codexprobe; API10 Java/DEX035/v1 signature. JNI x86/armeabi-v7a API9; x86_64 API21 для emulator. NDK r16b с официальным archive SHA1.

- No UUID/PSM/ECU packet constants; no scan/pair/remote-connect/send, reset/root/USER-channel takeover.
- JNI L2CAP/RFCOMM/HCI open/close + errno, local HCIGETDEVLIST/INFO и cached features/LE bit, L2CAP_OPTIONS/readlink. LE bit не доказывает CoC.
- Passive HCI RAW: реальные перечисленные controllers, command/ACL/event filter, direction ancillary; 30 сек/4096 packets. Raw hex не маркируется EVI, может содержать другие BT sessions; не публиковать без просмотра.
- Inventory: signatures без vendor invocation, bonded devices, SDK/ABI/mounts/modules, sysfs/controller paths, framework/libs, Binder service list, packages/services/permissions. Три proc snapshots с интервалом15сек, relevant PID/cmdline/status/maps/fd/socket inode/root. Доступ/ошибки/лимиты явно указаны.
- Доступный APK с VR30 в package/label копируется для локального анализа (до4, <64MiB каждый). Private data и link keys не читаются. Экспорт report/evidence в shared storage.
- Если Android скрывает host или CAP_NET_RAW denied: `tools/dcu-host-probe.sh` только в EXISTING authorized host shell, без rooting/drivers. `tools/inspect_bt_artifact.py` индексирует actual APK/dex/.so strings/hash; strings — leads, не подтверждение transport или channel IDs.

## Как артефакты выбирают следующий код

| Evidence | Следующий шаг |
|---|---|
| VR30 dex/native imports | Actual vendor binding/JNI, permissions и read-only calls |
| FD inode + Unix socket + mapped ELF | Peer daemon/IPC ABI; не открывать произвольные endpoints |
| HCI existing VR30 session | EVI address/handle, actual PSM/CID и handshake; не replay до классификации команд |
| L2CAP доступен, CoC неизвестен | Проверить actual backport/user stack, не подменять CoC RFCOMM |
| Host/HCI denied/hidden | Existing host snapshot + namespace comparison |

Gate остаётся НЕ ПРОЙДЕН: reproducible read-only EVI session, RPM provenance=ECUTEK и stale после EVI disconnect; затем Boost Actual с проверенной absolute/gauge семантикой. UI пока UNKNOWN/нет данных, DataHub пуст. Тест: `DCU-PROBE-NEXT-TEST.md`.
