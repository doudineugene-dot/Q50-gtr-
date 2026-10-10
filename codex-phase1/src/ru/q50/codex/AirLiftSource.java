package ru.q50.codex;
public final class AirLiftSource implements TelemetrySource {
    public void start(DataHub hub) { }
    public void stop() { }
    public String status() { return "Air Lift: НЕ ПОДКЛЮЧЕН"; }
}
