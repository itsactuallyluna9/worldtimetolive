package dev.itsactuallyluna9.worldtimetolive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;

public class WorldTimer {
    protected List<Integer> warn_times = List.of(1,2,3,4,5,10,15,30,45,60,90,120,150,180); // in minutes
    private boolean enabled;
    private Instant gracePeriodStart;
    private Instant startTime;
    private Instant endTime;
    private Instant gracePeriodEnd;
    private WorldTimerState state = WorldTimerState.UNKNOWN;
    private int lastWarned = 9999;

    // Logic:

    public WorldTimer() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                everySecond();
            }
        }, 0, 1000);
        reset();
        WorldTimeToLive.CONFIG.subscribeToEnabled(enabled -> {
            reset();
        });
    }

    private void everySecond() {
        if (WorldTimeToLive.SERVER == null || WorldTimeToLive.ADVENTURE == null) {
            return;
        }
        if (enabled) {
            var lastState = state;
            setState();
            if (lastState != state) {
                WorldTimeToLive.LOGGER.info("State changed from {} to {}", lastState, state);
                switch (state) {
                    case BEFORE_START_GRACE_PERIOD:
                        break;
                    case IN_START_GRACE_PERIOD:
                        break;
                    case IN_MAIN:
                        // no effects!
                        break;
                    case AFTER_END:
                        // no immediate effects!
                        break;
                    case AFTER_END_GRACE_PERIOD:
                        setToSpectator();
                        kickNonOps();
                        stopServer();
                        break;
                    case UNKNOWN:
                        // SKIP.
                        return;
                    default:
                        throw new IllegalStateException("Unexpected value: " + state);
                }
            }
            // and now, to warn players
            // round down to the nearest minute
            var now = Instant.now().getEpochSecond();
            var start = startTime.getEpochSecond();
            var end = endTime.getEpochSecond();
            var minutes = (int) (end - now) / 60 + 1;
            if (state == WorldTimerState.IN_MAIN && warn_times.contains(minutes) && lastWarned != minutes) {
                warnPlayers();
                lastWarned = minutes;
            }
        }
    }

    public void reset() {
        // load from config
        this.state = WorldTimerState.UNKNOWN;
        this.enabled = WorldTimeToLive.CONFIG.enabled();
        this.lastWarned = 9999;
        int gracePeriodSeconds = WorldTimeToLive.CONFIG.gracePeriodSeconds();
        switch (WorldTimeToLive.CONFIG.timings.mode()) {
            case COUNTDOWN:
                startTime = Instant.parse(WorldTimeToLive.CONFIG.timings.startTime());
                endTime = startTime.plusSeconds(WorldTimeToLive.CONFIG.timings.countdownSeconds());
                break;
            case BETWEEN:
                startTime = Instant.parse(WorldTimeToLive.CONFIG.timings.startTime());
                endTime = Instant.parse(WorldTimeToLive.CONFIG.timings.endTime());
                break;
            case AT:
                endTime = Instant.parse(WorldTimeToLive.CONFIG.timings.startTime());
                startTime = endTime.minusSeconds(WorldTimeToLive.CONFIG.timings.countdownSeconds());
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + WorldTimeToLive.CONFIG.timings.mode());
        }
        gracePeriodStart = startTime.minusSeconds(gracePeriodSeconds);
        gracePeriodEnd = endTime.plusSeconds(gracePeriodSeconds);
        setState();
    }

    private void setState() {
        try {
            Instant now = Instant.now();

            // Handle the state transitions sequentially
            switch (state) {
                case BEFORE_START_GRACE_PERIOD:
                    if (!now.isBefore(gracePeriodStart)) {
                        state = WorldTimerState.IN_START_GRACE_PERIOD;
                        return; // Stop here so we don't skip states
                    }
                    break;

                case IN_START_GRACE_PERIOD:
                    if (!now.isBefore(startTime)) {
                        state = WorldTimerState.IN_MAIN;
                        return; // Stop here to move to the next state on the next call
                    }
                    break;

                case IN_MAIN:
                    if (!now.isBefore(endTime)) {
                        state = WorldTimerState.AFTER_END;
                        return; // Stop here to handle the next state on the next call
                    }
                    break;

                case AFTER_END:
                    if (!now.isBefore(gracePeriodEnd)) {
                        state = WorldTimerState.AFTER_END_GRACE_PERIOD;
                        return; // Stop here to avoid skipping states
                    }
                    break;

                default:
                    // If state is not set or UNKNOWN, determine initial state
                    if (now.isBefore(gracePeriodStart)) {
                        state = WorldTimerState.BEFORE_START_GRACE_PERIOD;
                    } else if (now.isBefore(startTime)) {
                        state = WorldTimerState.IN_START_GRACE_PERIOD;
                    } else if (now.isBefore(endTime)) {
                        state = WorldTimerState.IN_MAIN;
                    } else if (now.isBefore(gracePeriodEnd)) {
                        state = WorldTimerState.AFTER_END;
                    } else {
                        state = WorldTimerState.AFTER_END_GRACE_PERIOD;
                    }
                    break;
            }
        } catch (Exception e) {
            state = WorldTimerState.UNKNOWN;
        }
    }

    // Effects:

    protected void checkLogin(ServerLoginPacketListenerImpl handler) {
        // always allow ops
        var username = handler.getUserName().split("\\s*[()]\\s*")[0];
        if (Arrays.asList(WorldTimeToLive.SERVER.getPlayerList().getOpNames()).contains(username)) {
            return;
        }

        if (this.enabled) {
            var mm = MiniMessage.miniMessage();
            if (this.state == WorldTimerState.BEFORE_START_GRACE_PERIOD && !WorldTimeToLive.CONFIG.effects.beforeGraceAllowsJoin()) {
                var component = WorldTimeToLive.ADVENTURE.toNative(mm.deserialize(WorldTimeToLive.CONFIG.messages.beforeKickMessage()));
                handler.disconnect(component);
            } else if ((this.state == WorldTimerState.AFTER_END || this.state == WorldTimerState.AFTER_END_GRACE_PERIOD) && !WorldTimeToLive.CONFIG.effects.afterAllowsJoin()) {
                var component = WorldTimeToLive.ADVENTURE.toNative(mm.deserialize(WorldTimeToLive.CONFIG.messages.afterKickMessage()));
                handler.disconnect(component);
            }
        }
    }

    public boolean canDoInteraction(Player player) {
        if (this.enabled && !WorldTimeToLive.SERVER.getPlayerList().isOp(player.getGameProfile())) {
            if (this.state == WorldTimerState.BEFORE_START_GRACE_PERIOD) {
                return WorldTimeToLive.CONFIG.effects.beforeAllowsBlocks();
            }
        }
        return true;
    }

    public net.minecraft.network.chat.Component getMOTD() {
        if (this.enabled) {
            var mm = MiniMessage.miniMessage();
            if (this.state == WorldTimerState.BEFORE_START_GRACE_PERIOD) {
                return WorldTimeToLive.ADVENTURE.toNative(mm.deserialize(WorldTimeToLive.CONFIG.messages.motdBeforeTimer()));
            } else if (this.state == WorldTimerState.AFTER_END || this.state == WorldTimerState.AFTER_END_GRACE_PERIOD) {
                return WorldTimeToLive.ADVENTURE.toNative(mm.deserialize(WorldTimeToLive.CONFIG.messages.motdAfterTimer()));
            }
        }
        return null;
    }

    private void stopServer() {
        // kick ALL players
        if (!WorldTimeToLive.CONFIG.effects.afterGraceShutdownServer()) {
            return;
        }
        var mm = MiniMessage.miniMessage();
        var component = WorldTimeToLive.ADVENTURE.toNative(mm.deserialize(WorldTimeToLive.CONFIG.messages.afterKickMessage()));
        WorldTimeToLive.SERVER.getPlayerList().getPlayers().forEach(player -> {
            player.connection.disconnect(component);
        });
        WorldTimeToLive.SERVER.halt(false);
    }

    private void kickNonOps() {
        // kick all non-ops
        if (!WorldTimeToLive.CONFIG.effects.afterGraceKickPlayers()) {
            return;
        }
        var mm = MiniMessage.miniMessage();
        var component = WorldTimeToLive.ADVENTURE.toNative(mm.deserialize(WorldTimeToLive.CONFIG.messages.afterKickMessage()));
         WorldTimeToLive.SERVER.getPlayerList().getPlayers().forEach(player -> {
             if (!WorldTimeToLive.SERVER.getPlayerList().isOp(player.getGameProfile())) {
                 player.connection.disconnect(component);
             }
         });
    }

    private void setToSpectator() {
        // set all non-ops to spectator
        if (!WorldTimeToLive.CONFIG.effects.afterGraceSetSpectator()) {
            return;
        }
        WorldTimeToLive.SERVER.getPlayerList().getPlayers().forEach(player -> {
            if (!WorldTimeToLive.SERVER.getPlayerList().isOp(player.getGameProfile())) {
                player.setGameMode(GameType.SPECTATOR);
            }
        });
    }

    private void warnPlayers() {
        var mm = MiniMessage.miniMessage();
        var component = mm.deserialize(WorldTimeToLive.CONFIG.messages.warnMessage(), getTagResolver());
        if (WorldTimeToLive.CONFIG.effects.warnPlayers()) {
            WorldTimeToLive.SERVER.getPlayerList().getPlayers().forEach(player -> {
                player.sendMessage(component);
            });
        } else {
            WorldTimeToLive.SERVER.getPlayerList().getPlayers().forEach(player -> {
                if (!WorldTimeToLive.SERVER.getPlayerList().isOp(player.getGameProfile())) {
                    player.sendMessage(component);
                }
            });
        }
        WorldTimeToLive.ADVENTURE.console().sendMessage(component);
    }

    public static @NotNull TagResolver getTagResolver() {
        return TagResolver.resolver("remaining", (args, context) -> {
            String method;
            if (args.peek() == null) {
                method = "stopwatch";
            } else {
                method = args.pop().lowerValue();
            }
            if (method.equals("human")) {
                return Tag.selfClosingInserting(Component.text(getTimeRemainingHuman()));
            }
            return Tag.selfClosingInserting(Component.text(getTimeRemainingStopwatch()));
        });
    }

    public String getTimeRemainingHuman() {
        // returns endtime - now in human-readable format
        // 30d 12h 30m 30s
        var now = Instant.now().getEpochSecond() - 1;
        var end = endTime.getEpochSecond();
        var diff = end - now;
        return getTimeHuman(diff);
    }

    public String getTimeRemainingStopwatch() {
        // returns endtime - now in stopwatch format
        // 30d 12:30:30
        var now = Instant.now().getEpochSecond() - 1;
        var end = endTime.getEpochSecond();
        var diff = end - now;
        return getTimeStopwatch(diff);
    }

    public static String getTimeHuman(long diff) {
        var days = diff / 86400;
        diff = diff % 86400;
        var hours = diff / 3600;
        diff = diff % 3600;
        var minutes = diff / 60;
        var seconds = diff % 60;
        // hide anything that is 0
        // eg 59s
        // keep lower units (so 1m 0s)
        if (days == 0) {
            if (hours == 0) {
                if (minutes == 0) {
                    return seconds + "s";
                }
                return minutes + "m " + seconds + "s";
            }
            return hours + "h " + minutes + "m " + seconds + "s";
        }
        return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
    }

    public static String getTimeStopwatch(long diff) {
        var days = diff / 86400;
        diff = diff % 86400;
        var hours = diff / 3600;
        diff = diff % 3600;
        var minutes = diff / 60;
        var seconds = diff % 60;
        // double pad hours, minutes, and seconds
        // hide days and hours if they are 0 (keeping lower units)
        // eg 1d 12:30:30
        // or 1d 00:00:00
        if (days == 0) {
            if (hours == 0) {
                return String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
            }
            return String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
        }
        return days + "d " + String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
    }
}
