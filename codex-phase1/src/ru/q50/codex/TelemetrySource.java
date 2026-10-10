package ru.q50.codex;

/** Future adapters publish proven channels only; phase 1 opens no vehicle transport. */
public interface TelemetrySource {
    void start(DataHub hub);
    void stop();
    String status();
}
