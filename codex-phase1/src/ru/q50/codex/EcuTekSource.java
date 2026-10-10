package ru.q50.codex;
public final class EcuTekSource implements TelemetrySource {
    public void start(DataHub hub) { }
    public void stop() { }
    public String status() { return "EcuTek: НЕ ПОДКЛЮЧЕН"; }
}
