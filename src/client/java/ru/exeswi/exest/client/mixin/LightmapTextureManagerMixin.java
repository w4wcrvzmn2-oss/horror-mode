package ru.exeswi.exest.client.mixin;

import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.exeswi.exest.client.state.ClientHorrorState;

/**
 * Feeds our horror darkness into the same pipeline the vanilla Darkness status effect
 * uses. Raising this factor dims the entire lightmap — including torch light, which is
 * exactly why torches barely help during horror events.
 */
@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {

    @Inject(method = "getDarknessFactor", at = @At("RETURN"), cancellable = true)
    private void exest$deepenDarkness(float delta, CallbackInfoReturnable<Float> cir) {
        float horror = ClientHorrorState.darknessFactor();
        if (horror > cir.getReturnValueF()) {
            cir.setReturnValue(horror);
        }
    }
}
