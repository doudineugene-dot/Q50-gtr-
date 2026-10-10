# Q50 GTR+ Codex — Phase 1 с нуля

Нативный прототип трёх страниц для stock InTouch DCU Gen1. minSdk=10, targetSdk=10; пакет `ru.q50.codex`. При запуске LIVE пуст, переключатель справа включает явно обозначенный DEMO. DIAG показывает реальные размеры/FPS. LPFP всегда недоступен.

На Linux с JDK 17, Android SDK build-tools 33.0.2 в PATH, Python 3.12 и cryptography 44.0.2:

```sh
bash codex-phase1/test.sh
bash codex-phase1/build.sh
```

Workflow `.github/workflows/codex-phase1.yml` запускается на push в `codex/**`. Артефакт `Q50-Codex-Phase1-APK-EPK` содержит APK, EPK, SHA256SUMS, подпись, badging, provenance, тестовые отчёты и host captures. Второй job проверяет touch/lifecycle на Android API 29 emulator и выдаёт отдельный артефакт реальных Android captures. Это не проверка API 10 или автомобиля.

Production-источники из старого `src/` не импортируются. Упаковщик `../tools/epktool.py` и публичный `../keys/obu_cert.pem` переиспользуются. Постоянный keystore может передаваться через Q50_KEYSTORE/Q50_KEY_ALIAS/Q50_KEY_PASS; CI использует имеющиеся signing secrets, если они доступны. Без них signing mode temporary, обновление следующей CI-сборки потребует удаления только прототипа.

Инструкции установки и пять действий теста: `../NEXT_TEST.md`. Состояние: `../PROJECT_STATE.md`, архитектура: `../docs/architecture.md`.
