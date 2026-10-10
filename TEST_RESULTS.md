# Результаты Phase 1

Локально: production renderer и DataHub прошли 183 проверки; создано 24 render captures (3 страницы × LIVE/DEMO × 4 размера). Визуально проверены три DEMO-страницы 840×480; после просмотра исправлены правый отступ плиток, положение подписей и единицы AFR. Native Java источники скомпилированы против Android API 10 jar; локальный JDK 27 используется только для smoke-проверки Java 8 bytecode, CI отдельно компилирует release 7 на JDK 17.

EPK roundtrip с одноразовой тестовой парой RSA-1024 восстановил payload byte-for-byte. Реальный OBU private key не используется.

CI и Android emulator: ожидается запуск; окончательные ссылки и результат добавляются после успешных jobs. APK должен пройти package/minSdk/targetSdk, отсутствие permissions, v1 signature, zipalign и DEX035 checks; EPK — строгую проверку раскладки. Приложение не устанавливалось на автомобиль. Настоящая телеметрия и Bluetooth в фазе 1 отсутствуют.
