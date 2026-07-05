package ru.exeswi.exest.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;
import ru.exeswi.exest.client.state.ClientHorrorState;
import ru.exeswi.exest.config.ConfigManager;

import java.util.Random;

/**
 * All fullscreen effects, drawn cheaply with plain fills over the HUD: vignette, film
 * grain, TV static, glitch rows with RGB fringing, eyelid blinks, edge shadows, the
 * watching-eyes hallucination, the screen runner silhouette and the white flash.
 * No shaders, no framebuffers — a few hundred small rects at worst.
 */
public final class HorrorOverlayRenderer {

    private static final Random RANDOM = new Random();

    private HorrorOverlayRenderer() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || !ConfigManager.get().enableVisualEffects) {
            return;
        }
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        float sanityFear = 1.0f - MathHelper.clamp(ClientHorrorState.sanity / 100.0f, 0.0f, 1.0f);

        renderVignette(context, w, h, sanityFear, client);
        renderGrain(context, w, h, sanityFear);
        renderEdgeShadows(context, w, h, sanityFear);
        renderStatic(context, w, h);
        renderGlitch(context, w, h);
        renderHallucinationOverlay(context, w, h);
        renderRunner(context, w, h);
        renderBlink(context, w, h);
        renderScreamerFace(context, w, h);
        renderFlash(context, w, h);
    }

    /**
     * Procedural screaming face: pale, hollow-eyed, jaw torn open, shaking a few pixels
     * every frame with occasional row tearing. No texture — pure geometry, so it looks
     * slightly different every single time it appears.
     */
    private static void renderScreamerFace(DrawContext context, int w, int h) {
        if (ClientHorrorState.faceTicks <= 0) {
            return;
        }
        float progress = ClientHorrorState.faceTicks / (float) ClientHorrorState.faceDuration;
        int backdrop = (int) (MathHelper.sin(progress * MathHelper.PI) * 215.0f);
        context.fill(0, 0, w, h, (backdrop << 24));

        int cx = w / 2 + RANDOM.nextInt(17) - 8;
        int cy = h / 2 + RANDOM.nextInt(13) - 6;
        int fh = (int) (h * 0.72f);
        int fw = (int) (fh * 0.62f);
        int skin = 0xFFB8B2A4;
        int shade = 0xFF8A8378;

        // head: a stack of rects that reads as a gaunt oval
        context.fill(cx - fw / 2, cy - fh / 2 + fh / 8, cx + fw / 2, cy + fh / 2 - fh / 10, skin);
        context.fill(cx - fw / 3, cy - fh / 2, cx + fw / 3, cy - fh / 2 + fh / 8, skin);
        context.fill(cx - fw / 3, cy + fh / 2 - fh / 10, cx + fw / 3, cy + fh / 2, shade);

        // hollow eye pits with tiny bright points, never quite centered
        int eyeW = fw / 5;
        int eyeH = fh / 5;
        int eyeY = cy - fh / 6;
        for (int side : new int[]{-1, 1}) {
            int ex = cx + side * fw / 4 - eyeW / 2;
            context.fill(ex - 2, eyeY - 2, ex + eyeW + 2, eyeY + eyeH + 2, 0xFF1A1512);
            context.fill(ex, eyeY, ex + eyeW, eyeY + eyeH, 0xFF000000);
            int px = ex + 2 + RANDOM.nextInt(Math.max(1, eyeW - 4));
            int py = eyeY + 2 + RANDOM.nextInt(Math.max(1, eyeH - 4));
            context.fill(px, py, px + 2, py + 2, 0xFFE8E4D8);
        }

        // the jaw hangs far too low, edges jagged
        int mouthW = fw / 3;
        int mouthTop = cy + fh / 12;
        int mouthBottom = cy + fh / 2 + fh / 12;
        context.fill(cx - mouthW / 2, mouthTop, cx + mouthW / 2, mouthBottom, 0xFF000000);
        for (int i = 0; i < 8; i++) {
            int jx = cx - mouthW / 2 + RANDOM.nextInt(mouthW);
            context.fill(jx, mouthTop - 3 - RANDOM.nextInt(5), jx + 3, mouthTop, 0xFF000000);
        }

        // cracks crawling over the skin
        for (int i = 0; i < 10; i++) {
            int sx = cx - fw / 2 + RANDOM.nextInt(fw);
            int sy = cy - fh / 2 + RANDOM.nextInt(fh);
            context.fill(sx, sy, sx + 1, sy + 4 + RANDOM.nextInt(10), 0x903A342C);
        }

        // torn scanline rows across the whole frame
        for (int i = 0; i < 4; i++) {
            int y = RANDOM.nextInt(h);
            context.fill(RANDOM.nextInt(12) - 6, y, w, y + 2, 0x50FF0000);
        }
    }

    /**
     * Edge-only vignette built from gradients. Deliberately NOT the vanilla vignette
     * texture: that one is authored for a multiplicative blend mode and darkens the
     * whole frame uniformly when drawn normally. The screen center stays untouched.
     */
    private static void renderVignette(DrawContext context, int w, int h, float fear, MinecraftClient client) {
        float darkness = ClientHorrorState.darknessFactor();
        // capped low: this draws over the hotbar and chat, so it must never blot them out
        float strength = MathHelper.clamp(0.12f + darkness * 0.3f + fear * 0.2f, 0.0f, 0.45f);
        int size = (int) (Math.min(w, h) * 0.28f);
        int edge = (int) (strength * 255.0f) << 24;

        context.fillGradient(0, 0, w, size, edge, 0x00000000);
        context.fillGradient(0, h - size, w, h, 0x00000000, edge);
        // fillGradient is vertical-only, so the side falloff is a few narrowing strips
        int strips = 10;
        int stripW = Math.max(1, size / strips);
        for (int i = 0; i < strips; i++) {
            float t = 1.0f - i / (float) strips;
            int alpha = (int) (strength * t * t * 255.0f);
            if (alpha <= 2) {
                continue;
            }
            int color = alpha << 24;
            context.fill(i * stripW, size, i * stripW + stripW, h - size, color);
            context.fill(w - (i + 1) * stripW, size, w - i * stripW, h - size, color);
        }

        if (ClientHorrorState.isRedMoon() && client.world != null && client.world.isNight()) {
            // a thin red wash over everything under the red moon
            context.fill(0, 0, w, h, 0x14FF0000);
        }
    }

    private static void renderGrain(DrawContext context, int w, int h, float fear) {
        int count = 30 + (int) (fear * 90.0f);
        for (int i = 0; i < count; i++) {
            int x = RANDOM.nextInt(w);
            int y = RANDOM.nextInt(h);
            int gray = RANDOM.nextInt(255);
            int alpha = 12 + RANDOM.nextInt(14);
            context.fill(x, y, x + 1, y + 1, (alpha << 24) | (gray << 16) | (gray << 8) | gray);
        }
    }

    private static void renderEdgeShadows(DrawContext context, int w, int h, float fear) {
        if (fear < 0.4f || RANDOM.nextInt(30) != 0) {
            return;
        }
        // a shadow slides along a screen edge for a single frame
        int size = 20 + RANDOM.nextInt(50);
        boolean left = RANDOM.nextBoolean();
        int y = RANDOM.nextInt(Math.max(1, h - size));
        int x0 = left ? 0 : w - 8;
        context.fillGradient(x0, y, x0 + 8, y + size, 0x66000000, 0x00000000);
    }

    private static void renderStatic(DrawContext context, int w, int h) {
        if (ClientHorrorState.staticTicks <= 0) {
            return;
        }
        int count = (int) (250 * ClientHorrorState.staticIntensity);
        for (int i = 0; i < count; i++) {
            int x = RANDOM.nextInt(w);
            int y = RANDOM.nextInt(h);
            int gray = RANDOM.nextBoolean() ? 255 : 0;
            int alpha = 90 + RANDOM.nextInt(90);
            context.fill(x, y, x + 2, y + 2, (alpha << 24) | (gray << 16) | (gray << 8) | gray);
        }
    }

    private static void renderGlitch(DrawContext context, int w, int h) {
        if (ClientHorrorState.glitchTicks <= 0) {
            return;
        }
        float intensity = ClientHorrorState.glitchIntensity;
        int rows = 4 + (int) (intensity * 8);
        for (int i = 0; i < rows; i++) {
            int y = RANDOM.nextInt(h);
            int rowHeight = 1 + RANDOM.nextInt(4);
            int shift = RANDOM.nextInt(16) - 8;
            // torn row plus chromatic fringing on both sides
            context.fill(shift, y, w + shift, y + rowHeight, 0x30FFFFFF);
            context.fill(shift - 3, y, w + shift - 3, y + rowHeight, 0x2800FFFF);
            context.fill(shift + 3, y, w + shift + 3, y + rowHeight, 0x28FF0000);
        }
        for (int i = 0; i < 3; i++) {
            int x = RANDOM.nextInt(w);
            int y = RANDOM.nextInt(h);
            context.fill(x, y, x + 8 + RANDOM.nextInt(24), y + 4 + RANDOM.nextInt(8), 0x40000000);
        }
    }

    private static void renderHallucinationOverlay(DrawContext context, int w, int h) {
        if (ClientHorrorState.overlayTicks <= 0) {
            return;
        }
        float progress = ClientHorrorState.overlayTicks / (float) ClientHorrorState.overlayDuration;
        int alpha = (int) (MathHelper.sin(progress * MathHelper.PI) * 70.0f);
        // the world reddens and something with pale eyes is very close to the glass
        context.fillGradient(0, 0, w, h, (alpha << 24) | 0x330000, ((alpha / 2) << 24) | 0x110000);
        int eyeAlpha = (int) (alpha * 2.2f);
        int cx = w / 2;
        int cy = h / 3;
        int spread = w / 10;
        context.fill(cx - spread - 3, cy, cx - spread + 3, cy + 2, (eyeAlpha << 24) | 0xDDDDCC);
        context.fill(cx + spread - 3, cy, cx + spread + 3, cy + 2, (eyeAlpha << 24) | 0xDDDDCC);
    }

    private static void renderRunner(DrawContext context, int w, int h) {
        if (ClientHorrorState.runnerTicks <= 0) {
            return;
        }
        float progress = 1.0f - ClientHorrorState.runnerTicks / (float) ClientHorrorState.runnerDuration;
        if (!ClientHorrorState.runnerFromLeft) {
            progress = 1.0f - progress;
        }
        int figureH = h / 2;
        int figureW = figureH / 3;
        int x = (int) (-figureW + progress * (w + figureW * 2.0f));
        int baseY = h - figureH - h / 10;
        int bob = (int) (MathHelper.sin(progress * 40.0f) * figureH * 0.04f);
        int head = figureH / 5;
        // a black humanoid smear: head, torso, legs mid-stride
        context.fill(x + figureW / 4, baseY + bob, x + figureW / 4 + head, baseY + head + bob, 0xE8000000);
        context.fill(x, baseY + head + bob, x + figureW, baseY + figureH * 3 / 5 + bob, 0xE8000000);
        int legTop = baseY + figureH * 3 / 5 + bob;
        int stride = (int) (MathHelper.sin(progress * 50.0f) * figureW * 0.6f);
        context.fill(x + stride / 2, legTop, x + stride / 2 + figureW / 3, baseY + figureH + bob, 0xE8000000);
        context.fill(x + figureW * 2 / 3 - stride / 2, legTop,
                x + figureW - stride / 2, baseY + figureH + bob, 0xE8000000);
    }

    private static void renderBlink(DrawContext context, int w, int h) {
        if (ClientHorrorState.blinkTicks <= 0) {
            return;
        }
        float progress = 1.0f - ClientHorrorState.blinkTicks / (float) ClientHorrorState.blinkDuration;
        float lid = MathHelper.sin(progress * MathHelper.PI);
        int cover = (int) (lid * h * 0.55f);
        context.fill(0, 0, w, cover, 0xFF000000);
        context.fill(0, h - cover, w, h, 0xFF000000);
    }

    private static void renderFlash(DrawContext context, int w, int h) {
        if (ClientHorrorState.flashTicks <= 0) {
            return;
        }
        int alpha = Math.min(255, ClientHorrorState.flashTicks * 60);
        context.fill(0, 0, w, h, (alpha << 24) | 0xFFFFFF);
    }
}
