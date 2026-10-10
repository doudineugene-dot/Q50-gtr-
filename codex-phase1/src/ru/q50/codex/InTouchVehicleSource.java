package ru.q50.codex;
public final class InTouchVehicleSource implements TelemetrySource {
    public void start(DataHub hub) { }
    public void stop() { }
    public String status() { return "InTouch: НЕ ПОДКЛЮЧЕН"; }
}
