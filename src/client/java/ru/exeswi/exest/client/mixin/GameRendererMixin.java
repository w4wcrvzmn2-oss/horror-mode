package ru.exeswi.exest.client.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.exeswi.exest.client.state.ClientHorrorState;

/** Night Vision barely works here: its lightmap contribution is cut to a quarter. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "getNightVisionStrength", at = @At("RETURN"), cancellable = true)
    private static void exest$weakenNightVision(LivingEntity entity, float tickDelta,
                                                CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValueF() * ClientHorrorState.nightVisionFactor());
    }
}
