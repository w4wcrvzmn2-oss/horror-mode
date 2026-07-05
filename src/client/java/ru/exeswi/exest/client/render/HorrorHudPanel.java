package ru.exeswi.exest.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.Box;
import ru.exeswi.exest.client.state.ClientHorrorState;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

import java.util.List;

/**
 * The corner status panel: horror level, a mind (sanity) bar, the current day and a
 * presence line that reacts to how close the nearest real creature is — even when it
 * is somewhere you are not looking. Toggled with the HUD key (default H).
 */
public final class HorrorHudPanel {

    private static final int PANEL_WIDTH = 118;
    private static final int PANEL_HEIGHT = 56;

    private HorrorHudPanel() {
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ClientHorrorState.hudVisible || client.player == null || client.world == null
                || client.options.hudHidden || client.getDebugHud().shouldShowDebugHud()) {
            return;
        }
        TextRenderer tr = client.textRenderer;
        int x = 6;
        int y = 6;

        // backdrop with a dark red frame
        ctx.fill(x - 4, y - 4, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0x9A000000);
        int frame = 0xFF3C0808;
        ctx.fill(x - 4, y - 4, x + PANEL_WIDTH, y - 3, frame);
        ctx.fill(x - 4, y + PANEL_HEIGHT - 1, x + PANEL_WIDTH, y + PANEL_HEIGHT, frame);
        ctx.fill(x - 4, y - 4, x - 3, y + PANEL_HEIGHT, frame);
        ctx.fill(x + PANEL_WIDTH - 1, y - 4, x + PANEL_WIDTH, y + PANEL_HEIGHT, frame);

        // horror level with ten pips
        int level = ClientHorrorState.horrorLevel;
        ctx.drawTextWithShadow(tr, "HORROR  " + level + "/10", x, y, 0xFFB03030);
        for (int i = 0; i < 10; i++) {
            int px = x + i * 11;
            int color = i < level ? 0xFFAA1111 : 0xFF2A2A2A;
            ctx.fill(px, y + 10, px + 8, y + 13, color);
        }

        // mind (sanity) bar
        float sanity = ClientHorrorState.sanity;
        int barColor = sanity > 66.0f ? 0xFF3FA34D : sanity > 33.0f ? 0xFFC9A227 : 0xFFB01818;
        boolean panicBlink = sanity < 20.0f && client.world.getTime() % 20 < 10;
        ctx.drawTextWithShadow(tr, "MIND", x, y + 18, panicBlink ? 0xFFFF4040 : 0xFF9A9A9A);
        int barX = x + 34;
        int barWidth = PANEL_WIDTH - 42;
        ctx.fill(barX, y + 19, barX + barWidth, y + 25, 0xFF1C1C1C);
        int filled = (int) (barWidth * sanity / 100.0f);
        if (filled > 0) {
            ctx.fill(barX, y + 19, barX + filled, y + 25, barColor);
        }

        long day = client.world.getTime() / 24000L + 1;
        ctx.drawTextWithShadow(tr, "DAY " + day, x, y + 30, 0xFF808080);

        renderPresence(ctx, tr, client, x, y + 42);
    }

    /** The line that makes people close the panel and then reopen it anyway. */
    private static void renderPresence(DrawContext ctx, TextRenderer tr, MinecraftClient client, int x, int y) {
        List<AbstractHorrorEntity> nearby = client.world.getEntitiesByClass(AbstractHorrorEntity.class,
                client.player.getBoundingBox().expand(48.0, 32.0, 48.0), e -> true);
        double nearestSq = Double.MAX_VALUE;
        for (AbstractHorrorEntity mob : nearby) {
            nearestSq = Math.min(nearestSq, client.player.squaredDistanceTo(mob));
        }
        String text;
        int color;
        if (nearestSq < 8.0 * 8.0) {
            boolean blink = client.world.getTime() % 10 < 5;
            text = "IT IS RIGHT HERE";
            color = blink ? 0xFFFF2020 : 0xFF700000;
        } else if (nearestSq < 20.0 * 20.0) {
            text = "SOMETHING IS CLOSE";
            color = 0xFFFF7020;
        } else if (nearestSq < 48.0 * 48.0) {
            text = "YOU ARE NOT ALONE";
            color = 0xFFC0B040;
        } else {
            text = "ALONE. FOR NOW";
            color = 0xFF606060;
        }
        ctx.drawTextWithShadow(tr, text, x, y, color);
    }
}
