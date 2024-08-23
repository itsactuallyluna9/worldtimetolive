package dev.itsactuallyluna9.worldtimetolive;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

public class WTTLCommands {
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
                .then(Commands.literal("config")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    WorldTimeToLive.CONFIG.load();
                                    WorldTimeToLive.TIMER.reset();
                                    return 1;
                                }))));
    }
}
