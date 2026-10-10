# Bluetooth — факты и границы Phase 1

По issue #11 владелец наблюдал VR30 DASH 0.5.4, соединённый напрямую с EcuTek на штатном DCU, без телефона/USB/UDP/зеркалирования. Это свидетельство владельца; сетевые логи, UUID/PSM/каналы и пакеты в этой фазе не получены.

Отрицательный результат стандартного BluetoothAdapter не доказывает невозможность Bluetooth на DCU. Фаза 3 должна исследовать vendor/hidden Android API, native/JNI, AF_BLUETOOTH, L2CAP/RFCOMM/HCI, BlueZ, Ygomi/Infiniti services/daemons, /proc/net/bluetooth и принадлежность сокетов PID.

Phase 1 не открывает Bluetooth и не запрашивает разрешений; EcuTekSource/AirLiftSource — честные UNAVAILABLE-заготовки. Нет вымышленных UUID, protocol bytes, соединений, команд или одновременных сессий. Управление ECU/клапанами/калибровками отсутствует.
