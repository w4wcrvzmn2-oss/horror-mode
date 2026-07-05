package ru.exeswi.exest.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.exeswi.exest.client.state.ClientHorrorState;

/**
 * Squeezes the fog distance whenever the horror engine raises fog density. At full
 * intensity the world ends four blocks from your face — the "black fog wall".
 */
@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {

    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void exest$denseFog(Camera camera, BackgroundRenderer.FogType fogType,
                                       float viewDistance, boolean thickFog, float tickDelta,
                                       CallbackInfo ci) {
        float density = ClientHorrorState.fogFactor();
        if (density <= 0.01f) {
            return;
        }
        float end = Math.max(4.0f, viewDistance * (1.0f - density));
        float start = Math.max(0.0f, end * (1.0f - density));
        if (end < RenderSystem.getShaderFogEnd()) {
            RenderSystem.setShaderFogStart(start);
            RenderSystem.setShaderFogEnd(end);
        }
    }
}
