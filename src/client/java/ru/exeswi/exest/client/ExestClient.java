package ru.exeswi.exest.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ru.exeswi.exest.client.handler.ClientTickHandler;
import ru.exeswi.exest.client.network.ClientNetworking;
import ru.exeswi.exest.client.render.HorrorHudPanel;
import ru.exeswi.exest.client.render.HorrorOverlayRenderer;
import ru.exeswi.exest.client.render.entity.HorrorRenderers;
import ru.exeswi.exest.client.state.ClientHorrorState;

/** Client entry point: renderers, network receivers, overlay, HUD panel and tick loop. */
public class ExestClient implements ClientModInitializer {

    private static KeyBinding toggleHudKey;

    @Override
    public void onInitializeClient() {
        ClientNetworking.register();
        HorrorRenderers.register();
        HudRenderCallback.EVENT.register(HorrorOverlayRenderer::render);
        HudRenderCallback.EVENT.register(HorrorHudPanel::render);

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.exest.toggle_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.exest.horror"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHudKey.wasPressed()) {
                ClientHorrorState.hudVisible = !ClientHorrorState.hudVisible;
            }
            ClientTickHandler.tick(client);
        });
    }
}
