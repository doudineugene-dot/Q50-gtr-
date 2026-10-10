# EcuTek direct Bluetooth research — 2026-10-10

## Confirmed facts

1. User's interface is EVI-BT R9. EcuTek's official support says revision 7 and above is **EVI-BTLE / Bluetooth Low Energy**.
2. EcuTek's official support says:
   - classic EVI-BT is Bluetooth Classic;
   - EVI-BTLE is Bluetooth Low Energy;
   - Rev 7+ is BTLE;
   - EVI-BTLE is added from inside ECU Connect rather than ordinary Android Bluetooth settings;
   - Android initiates the connection;
   - only one host device can be connected to one EVI at a time.
3. The user's Samsung log22.zip previously exposed Android log lines showing ECU Connect using **LE L2CAP CoC** with changing dynamic PSM values across connections. Treat the exact PSM as dynamic; do not hard-code a constant.
4. Independent public evidence: a Renesas engineering forum question was posted by user **marco-ecutek**, explicitly discussing implementing a data stream using **L2CAP Connection-oriented Channels (CoC)** instead of GATT notifications, citing flow-control/efficiency/client-code reasons. This is strong corroboration that EcuTek engineers actively explored this exact transport architecture.
5. Public Android API for LE L2CAP CoC is API 29. Hidden Android framework support appeared much later than Android 2.3.7. Therefore the stock API-10 Java Bluetooth framework cannot be assumed to provide EVI-BTLE CoC.
6. Nevertheless, the owner has observed **VR30 DASH 0.5.4 on the same stock InTouch DCU connecting directly to EcuTek with no phone, USB, UDP or screen mirroring**. Therefore a working implementation exists on this hardware and must be using a route other than ordinary API-10 public BLE APIs, or another compatible transport exposed by the DCU/vendor stack.

## Public sources

- EcuTek support: ECU Connect Vehicle Interface EVI-BTLE or EVI-BT
  https://ecutek.atlassian.net/wiki/spaces/SUPPORT/pages/557483/ECU+Connect+Vehicle+Interface+EVI-BTLE+or+EVI-BT
- EcuTek support: Adding an ECU Connect Vehicle Interface manually
  https://ecutek.atlassian.net/wiki/spaces/SUPPORT/pages/1681719302/Configuring+an+EVI-BT+manually
- EcuTek support: Bluetooth connection explained
  https://ecutek.zendesk.com/hc/en-gb/articles/115002067729-The-Bluetooth-connection-explained
- Renesas forum: marco-ecutek on L2CAP Connection-oriented channels
  https://community.renesas.com/wireles-connectivity/f/bluetooth-low-energy/19963/l2cap-connection-oriented-channels-api
- Android docs: createL2capChannel added API 29
  https://developer.android.com/reference/android/bluetooth/BluetoothDevice#createL2capChannel(int)

## Engineering consequence

Do not spend time trying ordinary API10 BluetoothAdapter BLE/GATT calls. The next task is to identify the transport path VR30 DASH uses on the stock DCU.

Priority order:

1. Acquire/analyse VR30 DASH APK/EPK if legally available.
2. On rooted DCU, inspect Bluetooth userspace/native stack:
   - processes and services related to bluetooth/bluez/bt
   - loaded libraries
   - /system/lib bluetooth libraries
   - /dev nodes
   - /proc/net/bluetooth/*
   - Unix-domain sockets in /dev/socket and /data
   - dbus if present
   - hciattach/hciconfig/hcitool/l2ping if present
   - kernel config/modules/driver/controller
3. When VR30 DASH is running and connected:
   - capture process PID
   - inspect /proc/<pid>/fd and /proc/<pid>/maps
   - identify opened Bluetooth/Unix sockets
   - identify native .so libraries
   - compare socket/fd state before and after connection
4. Build a read-only probe that reproduces only:
   - discovery/selection of EVI
   - connection
   - RX/TX counters
   - raw byte logging
5. Only after transport is proven, infer packet framing and obtain **EcuTek RPM**.
6. Do not treat stock InTouch RPM as EcuTek success.

## Acceptance gate before any full app install

The dedicated test package must report:

- EVI DISCOVERED
- EVI CONNECTED
- transport path
- remote address/identifier
- active HCI handle if obtainable
- PSM/CID if applicable
- RX bytes/s
- RX packets/s
- last RX age
- raw packet hex preview (rate-limited)
- ECUtek RPM tagged source=ECUTEK

Success = RPM changes with throttle and becomes stale/unavailable when EVI is disconnected.

Second success = Boost Actual changes live from EcuTek.

Until those are proven, do not move to Air Lift integration or final graphics.
