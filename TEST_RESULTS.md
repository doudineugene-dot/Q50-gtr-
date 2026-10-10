# Результаты Phase 1

Локально: production renderer и DataHub прошли 183 проверки; создано 24 render captures (3 страницы × LIVE/DEMO × 4 размера). Визуально проверены три DEMO-страницы 840×480; после просмотра исправлены правый отступ плиток, положение подписей и единицы AFR. Native Java источники скомпилированы против Android API 10 jar; локальный JDK 27 используется только для smoke-проверки Java 8 bytecode, CI отдельно компилирует release 7 на JDK 17.

EPK roundtrip с одноразовой тестовой парой RSA-1024 восстановил payload byte-for-byte. Реальный OBU private key не используется.

Первый CI run 38063126176: build прошёл полностью, 183 host checks, APK 16184 bytes, EPK 16592 bytes, minSdk/targetSdk 10/10, DEX035, v1 подпись, отсутствие permissions, zipalign, 16 проверок EPK. Подпись persistent; fingerprint совпал с release-signing-cert.sha256.

Первая попытка Android API 29 emulator показала настоящий LIVE-screen 840×480 без crash. Автоматический touch-test остановился: первый tap перехватил системный DeprecatedTargetSdkVersionDialog для targetSdk10, а не View приложения. В тест добавлено явное закрытие предупреждения клавишей ENTER перед касаниями; targetSdk приложения не меняется. Окончательные ссылки и результат повторной проверки добавляются после успешных jobs.

Приложение не устанавливалось на автомобиль. Настоящая телеметрия и Bluetooth в фазе 1 отсутствуют.
