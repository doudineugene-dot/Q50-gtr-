# Один install/run: минимальный тест штатного DCU

Это recon package, не готовый decoder. Bluetooth только штатный; USB-флешка для установки/экспорта. EVI допускает один host, не запускать ECU Connect одновременно.

1. На стоящем автомобиле установить Q50-Codex-EcuTek-Probe.epk через App Garage и открыть **Q50 EcuTek DCU Probe**. Сбор начинается автоматически; root не запрашивается.
2. В первые 10 секунд открыть установленный VR30 DASH 0.5.4 и подключить его обычным способом к EVI-BT R9. Оставить RPM открытым 30 секунд; зафиксировать время и меняется ли RPM. Probe не создаёт конкурирующую сессию.
3. Через 60–90 секунд вернуться в probe, нажать «Обновить отчёт», проверить COMPLETE, passive HCI и ошибки доступа. ECUTEK значения probe должны оставаться «НЕ ДОКАЗАНЫ».
4. Скопировать из shared storage Q50-DCU-bluetooth-report.txt и Q50-DCU-evidence на флешку/ПК. Если storage не виден: `/data/data/ru.q50.codexprobe/files/` через существующий authorized host/ADB. Если Android скрывает host: один `sh tools/dcu-host-probe.sh /absolute/writable/usb/path` в уже доступной host-сессии; без установки root/drivers.
5. Передать report, passive capture (после просмотра private BT данных) и доступный VR30 APK для локального анализа. На ПК: `python tools/inspect_bt_artifact.py path/to/VR30.apk --out artifact/vr30-leads.json`. Если APK не скопирован, `packages` содержит точный sourceDir для одного read-only извлечения.

После анализа минимальный следующий тест — RPM с provenance ECUTEK + disconnect/stale; Boost Actual следует после него. Эта версия проверяет транспортную среду, а не прохождение telemetry gate.
