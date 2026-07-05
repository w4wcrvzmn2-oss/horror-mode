package ru.exeswi.exest.client.mixin;

import net.minecraft.client.item.CompassAnglePredicateProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.exeswi.exest.client.state.ClientHorrorState;

/**
 * During the compass-spin world event every compass needle whirls as if the world had
 * lost its north. require = 0 keeps the mod alive even if a mapping shifts — the event
 * would then degrade to a no-op instead of crashing the game.
 */
@Mixin(CompassAnglePredicateProvider.class)
public abstract class CompassAnglePredicateProviderMixin {

    private static float exest$spin;

    @Inject(method = "unclampedCall", at = @At("HEAD"), cancellable = true, require = 0)
    private void exest$spinCompass(ItemStack stack, ClientWorld world, LivingEntity user, int seed,
                                   CallbackInfoReturnable<Float> cir) {
        if (ClientHorrorState.isCompassSpinning()) {
            exest$spin = (exest$spin + 0.031f + (seed % 7) * 0.002f) % 1.0f;
            cir.setReturnValue(exest$spin);
        }
    }
}
