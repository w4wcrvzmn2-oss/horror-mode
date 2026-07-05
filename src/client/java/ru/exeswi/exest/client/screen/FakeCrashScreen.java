package ru.exeswi.exest.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * A convincing "the game just died" screen. Any input dismisses it; it also dismisses
 * itself after a few seconds. The game keeps running behind it the whole time.
 */
public class FakeCrashScreen extends Screen {

    private static final String[] LINES = {
            "The game crashed whilst unexpected error",
            "Error: java.lang.NullPointerException: Cannot invoke \"net.minecraft.entity.Entity.isAlive()\"",
            "because \"this.watcher\" is null",
            "",
            "---- Minecraft Crash Report ----",
            "// I let you down. Sorry :(",
            "",
            "Time: just now",
            "Description: Watching player",
            "",
            "A detailed walkthrough of the error, its code path and all known details is as follows:",
            "---------------------------------------------------------------------------------------",
            "",
            "-- Head --",
            "Thread: Render thread",
            "Stacktrace:",
            "    at ru.unknown.watcher.WatcherEntity.observe(WatcherEntity.java:13)",
            "    at net.minecraft.client.MinecraftClient.render(MinecraftClient.java:1219)"
    };

    private int ticksLeft = 120;

    public FakeCrashScreen() {
        super(Text.literal("Crash"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xFF404040);
        int y = 20;
        for (String line : LINES) {
            context.drawTextWithShadow(textRenderer, line, 12, y, 0xFFFFFF);
            y += 10;
        }
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
