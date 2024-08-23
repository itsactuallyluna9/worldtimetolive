package dev.itsactuallyluna9.worldtimetolive;

import net.fabricmc.api.DedicatedServerModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.itsactuallyluna9.worldtimetolive.WTTLConfig;

public class WorldTimeToLive implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("worldtimetolive");
	public static final WTTLConfig CONFIG = WTTLConfig.createAndLoad();
	public static final WorldTimer TIMER = new WorldTimer();
	static FabricServerAudiences ADVENTURE;
	static MinecraftServer SERVER;

	public static final String MSG_PREFIX = "<gradient:dark_red:dark_purple>[WTTL]</gradient>";
	public static final String ADMIN_MSG_PREFIX = "<gradient:dark_red:dark_purple>[WTTL Admin]</gradient>";

	@Override
	public void onInitializeServer() {
		LOGGER.info("hello, world!");
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			SERVER = server;
			ADVENTURE = FabricServerAudiences.of(server);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SERVER = null;
			ADVENTURE = null;
		});
		CommandRegistrationCallback.EVENT.register(WTTLCommands::registerCommands);
		ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> TIMER.checkLogin(handler));
	}
}