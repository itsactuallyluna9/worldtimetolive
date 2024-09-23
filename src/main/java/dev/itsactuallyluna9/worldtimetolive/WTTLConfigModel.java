package dev.itsactuallyluna9.worldtimetolive;

import io.wispforest.owo.config.annotation.*;

import java.time.Instant;

@Config(name = "worldtimetolive", wrapperName = "WTTLConfig")
public class WTTLConfigModel {
    @Hook
    public boolean enabled = false;

//    public boolean inverted = false;

    @RangeConstraint(min = 0, max = Integer.MAX_VALUE)
    public int gracePeriodSeconds = 0;

    @Nest
    public Timings timings = new Timings();

    public static class Timings {
        @PredicateConstraint("isValidDate")
        public String startTime = "2024-08-13T01:00:00.000Z";
        @PredicateConstraint("isValidDate")
        public String endTime = "2024-08-13T03:00:00.000Z";
        @RangeConstraint(min = 0, max = Integer.MAX_VALUE)
        public int countdownSeconds = 0;
        public TimingMode mode = TimingMode.BETWEEN;

        public static boolean isValidDate(String date) {
            try {
                Instant.parse(date);
            } catch (Exception e) {
                return false;
            }
            return true;
        }
    }

    @Nest
    public Effects effects = new Effects();

    public static class Effects {
        public boolean beforeGraceAllowsJoin = false;
        public boolean beforeAllowsBlocks = true;
        public boolean afterAllowsJoin = true;
        public boolean afterGraceSetSpectator = false;
        public boolean afterGraceKickPlayers = false;
        public boolean afterGraceShutdownServer = false;
        public boolean warnPlayers = true;
    }

    @Nest
    public Messages messages = new Messages();

    public static class Messages {
        public String motdBeforeTimer = "<gradient:dark_red:dark_purple>[WTTL]</gradient> Starting soon...";
        public String motdAfterTimer = "<gradient:dark_red:dark_purple>[WTTL]</gradient> Thanks for playing!";
        public String beforeKickMessage = "<gradient:dark_red:dark_purple>[WTTL]</gradient> The server is currently closed. Check back later!";
        public String afterKickMessage = "<gradient:dark_red:dark_purple>[WTTL]</gradient> The server is now closed. Thanks for playing!";
        public String warnMessage = "<gradient:dark_red:dark_purple>[WTTL]</gradient> <remaining> remaining";
    }
}
