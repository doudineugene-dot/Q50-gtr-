# Q50 GTR+ Codex — состояние проекта

## Проверенная поставка 2026-10-11

90с observer `ru.q50.ecutekprobe`: tested commit `07f095238fcdc9a34e2a9b8d9a88a62f37e649c5`,
[CI build/emulator SUCCESS](https://github.com/doudineugene-dot/Q50-gtr-/actions/runs/38070795928).
30с Recon + dashboard: tested commit `c084c86ab4121eea4034a23fdbd652312f1a14f0`,
[CI build/emulator SUCCESS](https://github.com/doudineugene-dot/Q50-gtr-/actions/runs/38070384927).
Артефакты/хэши/границы проверок: [TEST_RESULTS.md](TEST_RESULTS.md).

Вывод исследования: Gen1 может разделять Android и MeeGo host; первичные дампы
дают конкретные naviwork daemons/DBus/vendor modules для поиска. Они не дают
доказанный vendor API, LE CoC backport или протокол EcuTek данного автомобиля.
Public API10/старый L2CAP socket сам по себе milestone не закрывает. Реализованы
passive RAW capture, ioctl/controller/handle probes, namespace/process/socket/library
inventory, offline ACL/CoC analyzer и static API9 host helper. PSM не фиксирован,
UUID/ECU packet bytes/каналы RPM/Boost не выдуманы. EcuTek transport пока UNKNOWN,
RPM/Boost UNAVAILABLE. Следующий gate — [одна 90с запись VR30 session](docs/BLUETOOTH-PROBE-NEXT-TEST.md).

## Итерация штатного Bluetooth / EcuTek 2026-10-10

Расширение: изолированный `bluetooth-probe/`, package `ru.q50.ecutekprobe`,
API10/10, 90с фонового сбора, четыре inventories, HCIGETCONNLIST, JSONL RAW
observer и offline HCI/ACL/LE CoC analyzer. Сборка отдельным workflow
`bluetooth-probe.yml`; следующий тест `docs/BLUETOOTH-PROBE-NEXT-TEST.md`.
Оба probe сохранены: 90с Q50-Codex-EcuTek-Probe, 30с Q50-Codex-DCU-Recon.
Для 90с observer выбирать **Q50-BuiltIn-Bluetooth-Probe-APK-EPK**.
RPM/Boost=UNAVAILABLE, discovery/connect=UNKNOWN. On-car gate не пройден.

Добавлен отдельный diagnostic APK/EPK `ru.q50.codexprobe` (1.0-dcu-recon, API10/10). Dashboard сохраняет пакет и zero-permission manifest; probe не публикует значения в DataHub. CI собирает оба пакета. Публичные Gen1 дампы показывают MeeGo/i686 + Android, host DBus/naviwork daemons, Ygomi/Connexis packages и vendor bt_hci/bt_dfu. Это evidence другой установки, не идентификация радио данного автомобиля.

Probe: JNI L2CAP/RFCOMM/HCI capability/errno, HCIGETDEVLIST/INFO, FD->socket/PID/maps, три снимка, passive RAW HCI без TX, inventory служб/framework/.so, доступный VR30 APK и shared-storage export. Нет automatic root или USER-channel takeover. HCI может быть скрыт vendor стеком либо недоступен UID; отсутствие пакетов не доказывает отсутствие Bluetooth. EVI transport, real ECUTEK RPM и Boost Actual **не доказаны**. Тест: `docs/DCU-PROBE-NEXT-TEST.md`. Исключены external dongles, phone/UDP bridge/mirroring.

Ниже исторический отчёт Phase1: «фазы не начаты» и «zero permissions» относятся к dashboard до этой итерации, не к отдельному probe.

Новая реализация по issue #11. Активный проект: `codex-phase1/`; пакет `ru.q50.codex`, версия `1.0-phase1`, minSdk/targetSdk 10/10.

Фаза 1 реализована: три страницы Canvas, оригинальная графика, DEMO/LIVE, диагностика размеров/FPS, source-agnostic DataHub, APK/EPK workflow. Финальная сборка d970831, Actions run 38063990388: build и emulator SUCCESS, 183 host checks и восемь Android screenshots. Физический DCU ещё не проверен. Подробности и ссылки: `TEST_RESULTS.md`.

LIVE запускается по умолчанию. InTouch/EcuTek/AirLift пока только адаптеры-заготовки, публикующие ноль значений. DEMO синтетический, выделен цветом и подписью на каждом доступном значении. LPFP недоступен в обоих режимах.

Старый `src/`, ресурсы и workflow не используются новой сборкой. Переиспользованы только `tools/epktool.py` и публичный `keys/obu_cert.pem`. Существующая production-ветка/релизы не заменяются. Отдельный Android package позволяет устанавливать прототип рядом со старым приложением; принятие нового пакета AppManager требует проверки в автомобиле.

Фазы 2–6 не начаты: новые доказательства автомобиля/протоколов отсутствуют. RPM и прямое Bluetooth-соединение VR30 DASH описаны владельцем в issue, а не проверены этой сборкой.

Воспроизводимая цепочка сборки зафиксирована по JDK, build-tools, Android jar checksum и Python crypto dependency. APK подписывается существующим постоянным ключом CI, когда секреты доступны; иначе явно temporary. EPK имеет случайный сеансовый ключ, поэтому побайтовая идентичность контейнера при повторной сборке не ожидается.
