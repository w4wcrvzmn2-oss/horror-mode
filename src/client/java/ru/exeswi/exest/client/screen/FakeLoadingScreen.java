package ru.exeswi.exest.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * "Loading terrain..." out of nowhere, exactly like the real one, gone two seconds
 * later. Did the world just... reload?
 */
public class FakeLoadingScreen extends Screen {

    private int ticksLeft;

    public FakeLoadingScreen() {
        super(Text.literal("Loading"));
        this.ticksLeft = 40 + (int) (Math.random() * 40);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xFF000000);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("multiplayer.downloadingTerrain"), width / 2, height / 2 - 10, 0xFFFFFF);
    }

    @Override
    public void tick() {
        if (--ticksLeft <= 0 && client != null) {
            client.setScreen(null);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
