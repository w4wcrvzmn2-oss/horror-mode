package ru.exeswi.exest.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * For a few seconds the game pretends it went back to the main menu. The buttons are
 * painted on and dead. Then the world is simply back.
 */
public class FakeTitleScreen extends Screen {

    private int ticksLeft = 70;

    public FakeTitleScreen() {
        super(Text.literal("Minecraft"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xFF000000);

        context.getMatrices().push();
        context.getMatrices().translate(width / 2.0f, height / 4.0f, 0.0f);
        context.getMatrices().scale(4.0f, 4.0f, 1.0f);
        context.drawCenteredTextWithShadow(textRenderer, "MINECRAFT", 0, 0, 0xFFFFFF);
        context.getMatrices().pop();

        drawDeadButton(context, height / 2, "menu.singleplayer");
        drawDeadButton(context, height / 2 + 24, "menu.multiplayer");
        drawDeadButton(context, height / 2 + 48, "menu.options");

        context.drawTextWithShadow(textRenderer, "Minecraft 1.21.1", 2, height - 10, 0xFFFFFF);
    }

    private void drawDeadButton(DrawContext context, int y, String translationKey) {
        int bw = 200;
        int x = width / 2 - bw / 2;
        context.fill(x, y, x + bw, y + 20, 0xFF2A2A2A);
        context.fill(x, y, x + bw, y + 1, 0xFF555555);
        context.fill(x, y + 19, x + bw, y + 20, 0xFF111111);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable(translationKey),
                width / 2, y + 6, 0xFF9A9A9A);
    }

    @Override
    public void tick() {
        if (--ticksLeft <= 0 && client != null) {
            client.setScreen(null);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        close();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        close();
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
