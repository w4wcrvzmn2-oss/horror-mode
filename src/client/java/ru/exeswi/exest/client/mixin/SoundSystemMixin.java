package ru.exeswi.exest.client.mixin;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.exeswi.exest.client.state.ClientHorrorState;

/**
 * The "silence zone" event: while it lasts, every world sound is swallowed — music,
 * mobs, blocks, rain, footsteps. Only UI clicks survive. Total silence in a game that
 * is never silent is one of the loudest things a player can hear.
 */
@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V",
            at = @At("HEAD"), cancellable = true)
    private void exest$silence(SoundInstance sound, CallbackInfo ci) {
        if (ClientHorrorState.isSilenced()
                && sound.getCategory() != SoundCategory.MASTER
                && sound.getCategory() != SoundCategory.VOICE) {
            ci.cancel();
        }
    }
}
