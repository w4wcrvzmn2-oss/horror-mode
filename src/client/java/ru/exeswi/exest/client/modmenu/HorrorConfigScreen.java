package ru.exeswi.exest.client.modmenu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.config.HorrorConfig;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * Mouse-friendly config: two columns of on/off toggles for every horror system plus
 * sliders for the frequency/intensity knobs. Everything writes straight into the
 * live config and is saved to JSON on Done.
 */
public class HorrorConfigScreen extends Screen {

    private final Screen parent;

    public HorrorConfigScreen(Screen parent) {
        super(Text.literal("Horror Mode"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        HorrorConfig config = ConfigManager.get();
        int colWidth = 150;
        int leftX = width / 2 - colWidth - 5;
        int rightX = width / 2 + 5;
        int y = 40;
        int step = 24;

        toggle(leftX, y, "Скримеры", config.enableJumpscares, v -> config.enableJumpscares = v);
        toggle(rightX, y, "Тьма и туман", config.enableDarkness, v -> config.enableDarkness = v);
        y += step;
        toggle(leftX, y, "Рассудок", config.enableSanity, v -> config.enableSanity = v);
        toggle(rightX, y, "Галлюцинации", config.enableHallucinations, v -> config.enableHallucinations = v);
        y += step;
        toggle(leftX, y, "Визуальные эффекты", config.enableVisualEffects, v -> config.enableVisualEffects = v);
        toggle(rightX, y, "Фейковые сообщения", config.enableFakeMessages, v -> config.enableFakeMessages = v);
        y += step;
        toggle(leftX, y, "Мировые события", config.enableWorldEvents, v -> config.enableWorldEvents = v);
        toggle(rightX, y, "Коррупция мира", config.enableCorruption, v -> config.enableCorruption = v);
        y += step;
        toggle(leftX, y, "Монстры", config.enableMonsters, v -> config.enableMonsters = v);
        y += step + 6;

        slider(leftX, y, "Частота событий", config.eventFrequency / 5.0,
                v -> config.eventFrequency = v * 5.0, () -> String.format("%.1f", config.eventFrequency));
        slider(rightX, y, "Частота спавна", config.spawnRateMultiplier / 5.0,
                v -> config.spawnRateMultiplier = v * 5.0, () -> String.format("%.1f", config.spawnRateMultiplier));
        y += step;
        slider(leftX, y, "Громкость ужаса", config.audioIntensity / 2.0,
                v -> config.audioIntensity = v * 2.0, () -> String.format("%.1f", config.audioIntensity));
        slider(rightX, y, "Шанс похищения", config.abductionChance,
                v -> config.abductionChance = v, () -> String.format("%.0f%%", config.abductionChance * 100));
        y += step + 8;

        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(width / 2 - 75, Math.min(y, height - 28), 150, 20).build());
    }

    private void toggle(int x, int y, String label, boolean value, Consumer<Boolean> setter) {
        addDrawableChild(CyclingButtonWidget.onOffBuilder(value)
                .build(x, y, 150, 20, Text.literal(label), (button, newValue) -> setter.accept(newValue)));
    }

    private void slider(int x, int y, String label, double normalized,
                        DoubleConsumer setter, java.util.function.Supplier<String> display) {
        addDrawableChild(new SliderWidget(x, y, 150, 20,
                Text.literal(label + ": " + display.get()), MathHelper.clamp(normalized, 0.0, 1.0)) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal(label + ": " + display.get()));
            }

            @Override
            protected void applyValue() {
                setter.accept(value);
                updateMessage();
            }
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFB03030);
    }

    @Override
    public void close() {
        ConfigManager.get().sanitize();
        ConfigManager.save();
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
