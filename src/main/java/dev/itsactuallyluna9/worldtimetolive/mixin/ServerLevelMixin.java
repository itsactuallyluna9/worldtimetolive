package dev.itsactuallyluna9.worldtimetolive.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.itsactuallyluna9.worldtimetolive.WorldTimeToLive;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public class ServerLevelMixin {
    @Inject(at = @At("HEAD"), method = "isUnderSpawnProtection", cancellable = true)
    public void isUnderSpawnProtection(CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) Player player) {
        boolean canDoInteraction = WorldTimeToLive.TIMER.canDoInteraction(player);
        if (!canDoInteraction) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
