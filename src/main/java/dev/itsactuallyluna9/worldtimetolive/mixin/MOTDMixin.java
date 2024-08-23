package dev.itsactuallyluna9.worldtimetolive.mixin;

import dev.itsactuallyluna9.worldtimetolive.WorldTimeToLive;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerStatus.class)
public class MOTDMixin {
    @Inject(at = @At("HEAD"), method = "description", cancellable = true)
    private void getDescriptionAlt(CallbackInfoReturnable<Component> cir) {
        var message = WorldTimeToLive.TIMER.getMOTD();
        if (message != null) {
            cir.setReturnValue(message);
            cir.cancel();
        }
    }
}