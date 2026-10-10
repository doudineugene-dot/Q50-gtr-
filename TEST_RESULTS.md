# Отчёт — Q50 GTR+ Codex, Phase 1

## Финальная проверка Bluetooth-итерации — 2026-10-11 (Asia/Barnaul)

Проверенный observer commit: `07f095238fcdc9a34e2a9b8d9a88a62f37e649c5`.
[Build + emulator SUCCESS](https://github.com/doudineugene-dot/Q50-gtr-/actions/runs/38070795928).
[APK/EPK + static host helper + provenance/checksums](https://github.com/doudineugene-dot/Q50-gtr-/actions/runs/38070795928/artifacts/11676656880).
[Android screenshot, logcat и экспортированный evidence ZIP](https://github.com/doudineugene-dot/Q50-gtr-/actions/runs/38070795928/artifacts/11676483770).

Проверенный dashboard + DCU Recon commit: `c084c86ab4121eea4034a23fdbd652312f1a14f0`.
[Build + emulator SUCCESS](https://github.com/doudineugene-dot/Q50-gtr-/actions/runs/38070384927).
[Dashboard и Recon APK/EPK](https://github.com/doudineugene-dot/Q50-gtr-/actions/runs/38070384927/artifacts/11676885745).
[Android captures и завершённый Recon report](https://github.com/doudineugene-dot/Q50-gtr-/actions/runs/38070384927/artifacts/11677080741).

CI: 183 dashboard checks/24 renders, **17 ProbeChecks**, mocked native capability/errno fixtures,
bounded EvidenceIO/ZIP/USB/process checks, **7 HCI framing tests**, native Linux execution,
EPK crypto roundtrip/structure, API10 Java7 compile, actual API9 x86/ARM JNI,
DEX035/v1 signature/zipalign — PASS. Static ELF32 helper собран legacy bionic,
без ELF interpreter, выполнен в Linux CI; это не подтверждение запуска на DCU.
API29 emulator: dashboard lifecycle regression, Recon JNI/три snapshots/COMPLETE/export,
observer JNI/background completion/export ZIP — PASS; нет FATAL EXCEPTION.
В observer harness исправлен повторный launch после ReviewPermissionsActivity:
теперь новый процесс получает короткий test interval. APK targetSdk остаётся 10.

Архивы скачаны в `artifact/final-observer-07f/` и `artifact/final-recon/`,
каждый SHA256 повторно проверен. Подпись persistent, сертификат
`f1b298035bde56521aa6888c14281e190df1abc13dfd21a0cff8d0fecaea8e84`.

```text
5c2ec1251b0e2fd59354e9737517a2f5a0fc29f34f7cdf08ac33bd923010dab9  Q50-Codex-EcuTek-Probe.apk
cdc1c4dfa7b874a064e818fde6a50e0291f51bc26208ebb746fa0080d35ea5d8  Q50-Codex-EcuTek-Probe.epk
07c936d2a958ad414005ed5bfd3296a8a81a6282a3d02861774f5bc1b2a177b2  Q50-btprobe-linux-x86
ce132859f3b2ca83def71733e33cc18c46f43213e7de980f353ea0a502899882  Q50-Codex-DCU-Recon.apk
0895e9e15735a57a2cee34a53d8b39b0db19d89b229649223eed0eae73928774  Q50-Codex-DCU-Recon.epk
```

**Физический API10/DCU/EVI не проверен.** Нет измеренного EVI discovery/connect,
PSM discovery/auth/session framing, ECUTEK RPM или Boost Actual. Пользовательские
log22/VR30 наблюдения не подменены emulator evidence. Следующий минимальный тест:
[90с с VR30 connect/disconnect](docs/BLUETOOTH-PROBE-NEXT-TEST.md).
Ниже сохранена история предыдущих проверок; этот раздел имеет приоритет.

## Итерация EcuTek/DCU 2026-10-10

Дополнительный 90с observer: `bluetooth-probe/`, `ru.q50.ecutekprobe`.
Локально JDK27 release8: Java compile против API10 jar PASS, bounded evidence
I/O/ZIP/USB-mount/process checks PASS; семь Python HCI framing tests PASS;
существующий production scene — 183 checks / 24 captures PASS. Локальный
Python не содержит cryptography; crypto/build/native/emulator выполняются в
pinned CI, а не считаются локально пройденными. Java7 bytecode проверяет CI
JDK17 release7. Новая native часть: GET connection list и legacy RAW sockaddr,
16MiB JSONL cap, ACL fragmentation/direction/handle separation, observed CoC
request/response metadata. Тестовые пакеты — стандартные Bluetooth fixtures,
не EcuTek data. Финальные CI результаты зафиксированы в верхнем разделе.

Новый probe ru.q50.codexprobe, API10/10, Q50-Codex-DCU-Recon.apk/.epk. Только BLUETOOTH + WRITE_EXTERNAL_STORAGE; dashboard сохраняет zero permissions. Нет scan/pair/remote-connect/ECU-запросов. HCI RAW listener только читает, ограничен 30 сек/4096 пакетов; данные не выдаются за EVI RX/telemetry.

Локально JDK27: 17 ProbeChecks PASS, включая safe USB mount parsing. --release7 отсутствует в JDK27; Java7/API10 компиляция выполняется pinned JDK17 CI. Native fixtures: errno denial, returned controller ID, cached LE bit, L2CAP non-proof, закрытие FD, output boundary. Обязательны 183 host checks, EPK crypto roundtrip и emulator workflow. Новый emulator check требует JNI load, три снимка, COMPLETE и export files, отсутствие FATAL EXCEPTION. NDK переключён на r10e/GCC4.9 с проверкой actual API9 platform directories; новый NDK не должен молча поднять native minimum API.

Физический DCU/EVI не подключён к этой сессии. Discovery/connect, EVI PSM/CID/handle, RX rate, ECUTEK RPM/Boost не получены. log22.zip и VR30 DASH APK отсутствуют в tracked files; сведения пользователя не выдаются за собственные измерения.

Первая фаза реализована с нуля: три русские Canvas-страницы, оригинальная графика, раздельные DEMO/LIVE, диагностика размеров/FPS, DataHub с VALID/STALE/UNAVAILABLE/ERROR и заготовками трёх источников. LIVE по умолчанию пуст; LPFP недоступен даже в DEMO.

## Сборка и артефакты

- Ветка: `codex/phase1-from-zero`.
- Коммит проверенной сборки: `d970831db973d9ad236c7d8714f21cc5a59af6cc`. Последующий коммит меняет только отчёт и PROJECT_STATE.
- [Actions run 38063990388 — build и emulator SUCCESS](https://github.com/doudineugene-dot/Q50-gtr-/actions/runs/38063990388).
- [APK/EPK, 24 render captures и результаты проверок](https://github.com/doudineugene-dot/Q50-gtr-/actions/runs/38063990388/artifacts/11674405429).
- [8 настоящих Android screenshots и logcat](https://github.com/doudineugene-dot/Q50-gtr-/actions/runs/38063990388/artifacts/11674640506).
- Package `ru.q50.codex`, versionName `1.0-phase1`, versionCode `1`, minSdk/targetSdk **10/10**.
- APK `Q50-Codex-Phase1.apk`: **16421 байт**.
- EPK `Q50-Codex-Phase1.epk`: **16832 байт**, type 2, один блок `q50codex.apk`, RSA key size 128 байт.
- Подпись **persistent**, v1/JAR; SHA256 сертификата `f1b298035bde56521aa6888c14281e190df1abc13dfd21a0cff8d0fecaea8e84`, совпадает с release-signing-cert.sha256. Keystore/пароли не опубликованы.

```text
076803660a7a1ba00279bd35fdaecc10029aefdb6d402dbf4fc3812c7c6ebfc3  Q50-Codex-Phase1.apk
f48668a1ccf51c0bcf012c117dc526b640ac2cee4696abf470cfb4e4ca237ba8  Q50-Codex-Phase1.epk
```

## Проверки

| Проверка | Результат |
|---|---|
| DataHub, приоритеты, stale/expiry, error, DEMO/LIVE, касания | 183 проверки PASS |
| Production-renderer на Java2D | 24 captures: LIVE/DEMO × 3 страницы × 640×480, 840×480, 1024×600, 1280×720 |
| Native Java | Компиляция против API10 jar с зафиксированным checksum; CI JDK17 release7 |
| APK | package/sdk, zero permissions, v1, zipalign, DEX035 PASS |
| EPK | 16 строгих проверок структуры PASS |
| EPK crypto roundtrip | Одноразовая RSA-1024 test key pair; payload восстановлен byte-for-byte |
| Android API29 emulator | Установка/запуск; screen/window/view/canvas 840×480 |
| Native UI/lifecycle | 3 LIVE + 3 DEMO страницы, возврат LIVE без чисел, DIAG off, pause/resume PASS; нет FATAL EXCEPTION |
| Визуальная проверка | Восемь native screenshots: круги, плитки/вкладки, LPFP unavailable, LIVE после DEMO пуст |
| Таймер перерисовки | Один lifecycle-owned Runnable; после переключений на финальных DEMO-снимках 11 FPS |
| Физический API10 / DCU | Ещё не проверено |

В harness закрывается системный DeprecatedTargetSdkVersionDialog API29; targetSdk приложения остаётся 10. При просмотре снимков найден и исправлен рост числа таймеров после касаний. После исправления выполнен успешный повторный build/emulator run. Host captures не выдаются за скриншоты автомобиля; native captures получены на API29.

## Файлы и воспроизведение

Создано **28 файлов**; точный список: [codex-phase1/FILES_CREATED.txt](codex-phase1/FILES_CREATED.txt). Дополнительно изменён только корневой `.gitignore` для Python cache. Старые app source/resources/build scripts и production workflow не изменены. Переиспользованы только существующие `tools/epktool.py` и публичный `keys/obu_cert.pem`.

На Linux с JDK17, build-tools33.0.2 в PATH, Python3.12 + cryptography44.0.2: `bash codex-phase1/test.sh`, затем `bash codex-phase1/build.sh`. CI сохраняет provenance, signature, badging и checksums. EPK использует случайный сеансовый ключ: его SHA256 меняется при повторной упаковке.

Локальная выдача: `C:\Users\Hp\Downloads\Q50-Codex-Phase1\` — финальные APK/EPK, SHA256SUMS, REPORT.md, NEXT_TEST.md, FILES_CREATED.txt, архивы CI и screenshots. Checkout: `C:\Users\Hp\Q50-codex-phase1\`.

## Ограничения и NEXT_TEST

- Приём нового пакета AppManager, установка EPK и запуск на физическом Android2.3.7/DCU пока не подтверждены этой реализацией. API10-компиляция и API29-эмулятор не заменяют тест на машине.
- InTouch RPM, EcuTek, Air Lift и Bluetooth относятся к фазам 2–6. Заготовки источников сейчас публикуют ноль samples, управляющих команд нет. LPFP остаётся UNAVAILABLE до доказанного pressure channel.
- Производительность, DCU-шрифты и landscape-размеры вне проверенных четырёх требуют измерений на машине.
- Production main/релизы не заменены. Android package отдельный; возможное поведение AppManager описано в инструкции.

[NEXT_TEST.md](NEXT_TEST.md): ровно пять действий — установка отдельного EPK, LIVE, DEMO, снимки DIAG, возврат LIVE/перезапуск.
