package dev.itsactuallyluna9.worldtimetolive;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

public class WTTLCommands {
    public static String WTTL_PREFIX = "<gradient:red:dark_purple>[WTTL]</gradient> ";

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("tellmm")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(context -> {
                                    Audience audience = WorldTimeToLive.ADVENTURE.audience(EntityArgument.getPlayers(context, "targets"));
                                    var mm = MiniMessage.miniMessage();
                                    Component parsed = mm.deserialize(StringArgumentType.getString(context, "message"));
                                    audience.sendMessage(parsed);
                                    return 1;
                                }))));
        dispatcher.register(Commands.literal("wttl")
                .then(Commands.literal("status")
                        .executes(context -> {
//                            var mm = MiniMessage.miniMessage();
//                            var component = mm.deserialize(WorldTimeToLive.CONFIG.messages.warnMessage(), getTagResolver());
//                            context.getSource().sendMessage(component);
                            return 1;
                        }))
                .then(Commands.literal("config")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    WorldTimeToLive.CONFIG.load();
                                    WorldTimeToLive.TIMER.reset();
                                    var mm = MiniMessage.miniMessage();
                                    Component parsed = mm.deserialize(WTTL_PREFIX + "Config reloaded!");
                                    context.getSource().sendMessage(parsed);
                                    return 1;
                                }))
                        .then(Commands.literal("enabled")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> {
                                            WorldTimeToLive.CONFIG.enabled(BoolArgumentType.getBool(context, "enabled"));
                                            var mm = MiniMessage.miniMessage();
                                            Component parsed = mm.deserialize(WTTL_PREFIX + "Timer " + (WorldTimeToLive.CONFIG.enabled() ? "<green>enabled</green>" : "<red>disabled</red>") + ".");
                                            context.getSource().sendMessage(parsed);
                                            return 1;
                                        }))
                                .executes(context -> {
                                    var mm = MiniMessage.miniMessage();
                                    Component parsed = mm.deserialize(WTTL_PREFIX + "Timer is " + (WorldTimeToLive.CONFIG.enabled() ? "<green>enabled</green>" : "<red>disabled</red>") + ".");
                                    context.getSource().sendMessage(parsed);
                                    return 1;
                                }))
                        .then(Commands.literal("gracePeriod")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                        .executes(context -> {
                                            WorldTimeToLive.CONFIG.gracePeriodSeconds(IntegerArgumentType.getInteger(context, "seconds"));
                                            var mm = MiniMessage.miniMessage();
                                            Component parsed = mm.deserialize(WTTL_PREFIX + "Grace period set to ").append(Component.text(WorldTimer.getTimeHuman(WorldTimeToLive.CONFIG.gracePeriodSeconds()) + ".").hoverEvent(Component.text(WorldTimeToLive.CONFIG.gracePeriodSeconds() + " seconds")));
                                            context.getSource().sendMessage(parsed);
                                            return 1;
                                        }))
                                .executes(context -> {
                                    var mm = MiniMessage.miniMessage();
                                    Component parsed = mm.deserialize(WTTL_PREFIX + "Grace period is set to ").append(Component.text(WorldTimer.getTimeHuman(WorldTimeToLive.CONFIG.gracePeriodSeconds()) + ".").hoverEvent(Component.text(WorldTimeToLive.CONFIG.gracePeriodSeconds() + " seconds")));
                                    context.getSource().sendMessage(parsed);
                                    return 1;
                                }))
//                        .then(Commands.literal("timings")
                        ));
    }
}
