package dev.itsactuallyluna9.worldtimetolive;

public enum WorldTimerState {
    UNKNOWN,
    BEFORE_START_GRACE_PERIOD,
    IN_START_GRACE_PERIOD,
    IN_MAIN,
    AFTER_END,
    AFTER_END_GRACE_PERIOD
}
