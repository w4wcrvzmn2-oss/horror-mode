package ru.exeswi.exest.events;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static ru.exeswi.exest.events.HorrorEvent.Category.FAKE;

/**
 * Chat gaslighting. Join, leave, death and advancement messages that only the target
 * player receives — in multiplayer nobody else will confirm having seen them.
 */
public final class FakeMessages {

    private static final String[] NAMES = {
            "Marcus_", "elena2010", "Dima_Kr", "unknown", "Wanderer", "n1ghtcrawler",
            "Sasha", "Observer", "Kolya2007", "whoami"
    };

    private static final String[] ADVANCEMENTS = {
            "Free the End", "Adventuring Time", "We Need to Go Deeper",
            "Not Quite \"Nine\" Lives", "Hot Tourist Destinations", "Uneasy Alliance"
    };

    private FakeMessages() {
    }

    public static void registerAll() {
        HorrorEvent.builder("fake_join", FAKE).weight(6).cooldown(12000)
                .enabledWhen(c -> c.enableFakeMessages)
                .action((p, m) -> {
                    String name = pick(p, NAMES);
                    p.sendMessage(Text.translatable("multiplayer.player.joined",
                            Text.literal(name)).formatted(Formatting.YELLOW), false);
                    // ...and they leave a couple of minutes later, as quietly as they came
                    m.schedule(p.getServer(), 1200 + p.getRandom().nextInt(2400), () -> {
                        if (p.isAlive() && !p.isDisconnected()) {
                            p.sendMessage(Text.translatable("multiplayer.player.left",
                                    Text.literal(name)).formatted(Formatting.YELLOW), false);
                        }
                    });
                }).register();

        HorrorEvent.builder("fake_disconnect", FAKE).weight(4).cooldown(14000)
                .enabledWhen(c -> c.enableFakeMessages)
                .action((p, m) -> p.sendMessage(Text.translatable("multiplayer.player.left",
                        Text.literal(pick(p, NAMES))).formatted(Formatting.YELLOW), false)).register();

        HorrorEvent.builder("fake_death", FAKE).weight(4).cooldown(18000)
                .enabledWhen(c -> c.enableFakeMessages)
                .action((p, m) -> {
                    Text message = p.getRandom().nextBoolean()
                            ? Text.translatable("death.attack.generic", Text.literal(pick(p, NAMES)))
                            : Text.translatable("death.attack.mob", Text.literal(pick(p, NAMES)),
                                    Text.literal("???"));
                    p.sendMessage(message, false);
                }).register();

        HorrorEvent.builder("fake_advancement", FAKE).weight(5).cooldown(12000)
                .enabledWhen(c -> c.enableFakeMessages)
                .action((p, m) -> p.sendMessage(Text.translatable("chat.type.advancement.task",
                        Text.literal(pick(p, NAMES)),
                        Text.literal("[" + pick(p, ADVANCEMENTS) + "]").formatted(Formatting.GREEN)),
                        false)).register();

        // it knows your name. Rare on purpose — this one has to stay a knife, not a drum
        HorrorEvent.builder("personal_whisper", FAKE).weight(4).cooldown(24000)
                .enabledWhen(c -> c.enableFakeMessages)
                .action((p, m) -> {
                    String message = String.format(pick(p, PERSONAL), p.getGameProfile().getName());
                    p.sendMessage(Text.literal("<unknown> " + message)
                            .formatted(Formatting.GRAY, Formatting.ITALIC), false);
                    m.cueBehind(p, ru.exeswi.exest.networking.SoundCue.WHISPER, 0.6f);
                    ru.exeswi.exest.sanity.SanityManager.modify(p, -6.0f);
                }).register();

        // your own name joins the game while you are already in it
        HorrorEvent.builder("doppelganger_join", FAKE).weight(3).cooldown(36000)
                .enabledWhen(c -> c.enableFakeMessages)
                .action((p, m) -> {
                    p.sendMessage(Text.translatable("multiplayer.player.joined",
                            Text.literal(p.getGameProfile().getName())).formatted(Formatting.YELLOW), false);
                    ru.exeswi.exest.sanity.SanityManager.modify(p, -8.0f);
                }).register();

        // co-op gaslighting at its purest: "<your friend> left the game" — while he is
        // standing right there answering you in voice chat
        HorrorEvent.builder("friend_left", FAKE).weight(5).cooldown(18000)
                .enabledWhen(c -> c.enableFakeMessages)
                .condition(p -> p.getServer() != null
                        && p.getServer().getPlayerManager().getPlayerList().size() >= 2)
                .action((p, m) -> {
                    var others = p.getServer().getPlayerManager().getPlayerList().stream()
                            .filter(other -> other != p).toList();
                    var friend = others.get(p.getRandom().nextInt(others.size()));
                    p.sendMessage(Text.translatable("multiplayer.player.left",
                            Text.literal(friend.getGameProfile().getName()))
                            .formatted(Formatting.YELLOW), false);
                    ru.exeswi.exest.sanity.SanityManager.modify(p, -6.0f);
                }).register();
    }

    private static final String[] PERSONAL = {
            "i see you, %s",
            "%s. come outside",
            "you are not alone, %s",
            "%s, stop hiding",
            "behind you, %s",
            "%s, why did you stop digging",
            "%s, benton was asking about you",
            "%s. do not read the name aloud"
    };

    private static String pick(net.minecraft.server.network.ServerPlayerEntity player, String[] pool) {
        return pool[player.getRandom().nextInt(pool.length)];
    }
}
