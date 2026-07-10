package ru.exeswi.exest.events;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import ru.exeswi.exest.sanity.SanityManager;
import ru.exeswi.exest.world.HorrorWorldState;

import java.util.ArrayList;
import java.util.List;

import static ru.exeswi.exest.events.HorrorEvent.Category.RARE;

/**
 * The Journal: a written book that quietly appears in the player's inventory. Its
 * author is "???", its entries multiply over time, and the newest one always
 * describes — accurately — what the player was doing about five minutes ago.
 * Nobody writes it. It gets written.
 */
public final class JournalEvents {

    private static final String[] TEMPLATES = {
            "Я видел, как ты %s.",
            "Ты %s. Я стоял совсем рядом.",
            "Пока ты %s, я считал твои вздохи.",
            "Ты %s и ни разу не обернулся.",
            "Сегодня ты %s. Мне понравилось."
    };

    private static final String[] CLOSERS = {
            "Осталось недолго.", "Ты хорошо спишь.", "Не читай это вслух.",
            "Продолжай. Я записываю.", "Ты почти готов.",
            "Бентон тоже вёл дневник.", "Murder спрашивал о тебе.",
            "РИДАВУМАКС. Теперь ты прочитал."
    };

    private static final java.util.Map<java.util.UUID, Integer> TIMER = new java.util.HashMap<>();

    private JournalEvents() {
    }

    public static void registerAll() {
        // weight 0: the journal runs on its own per-player schedule below;
        // this stays as a manual trigger for /horror event journal
        HorrorEvent.builder("journal", RARE).weight(0).cooldown(100)
                .enabledWhen(c -> c.enableHallucinations)
                .action(JournalEvents::updateJournal).register();
    }

    /**
     * The journal keeps its own clock, one per player: the first entry lands a couple
     * of minutes into the session, then every 5-9 minutes — for everyone on the
     * server independently. No shared cooldowns, no dice: it always gets written.
     * Called every 20 ticks from the main loop.
     */
    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (!ru.exeswi.exest.config.ConfigManager.get().enableHallucinations) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator()) {
                continue;
            }
            java.util.UUID id = player.getUuid();
            if (!TIMER.containsKey(id)) {
                TIMER.put(id, 1200 + player.getRandom().nextInt(1800));
                continue;
            }
            int left = TIMER.merge(id, -20, Integer::sum);
            if (left <= 0) {
                TIMER.put(id, 6000 + player.getRandom().nextInt(4800));
                updateJournal(player, null);
            }
        }
    }

    /** Death or a lost book cannot erase it: whatever it wrote about you comes back. */
    public static void restoreBook(ServerPlayerEntity player) {
        List<String> entries = HorrorWorldState.get(player.getServerWorld())
                .getJournal(player.getUuid());
        if (!entries.isEmpty()) {
            giveOrRefreshBook(player, entries);
        }
    }

    private static void updateJournal(ServerPlayerEntity player, HorrorEventManager manager) {
        HorrorWorldState state = HorrorWorldState.get(player.getServerWorld());
        long day = player.getServerWorld().getTime() / 24000L + 1;

        // the newest entry quotes what the player really did ~5 minutes ago
        String activity = ActivityLog.activityAgo(player, 6000);
        String template = TEMPLATES[player.getRandom().nextInt(TEMPLATES.length)];
        String entry = "День " + day + ". " + String.format(template, activity);
        if (player.getRandom().nextInt(3) == 0) {
            entry += " " + CLOSERS[player.getRandom().nextInt(CLOSERS.length)];
        }
        state.addJournalEntry(player.getUuid(), entry);

        giveOrRefreshBook(player, state.getJournal(player.getUuid()));
        SanityManager.modify(player, -5.0f);
    }

    private static void giveOrRefreshBook(ServerPlayerEntity player, List<String> entries) {
        List<RawFilteredPair<Text>> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        for (String entry : entries) {
            if (page.length() + entry.length() > 220) {
                pages.add(RawFilteredPair.of(Text.literal(page.toString())));
                page = new StringBuilder();
            }
            page.append(entry).append("\n\n");
        }
        if (!page.isEmpty()) {
            pages.add(RawFilteredPair.of(Text.literal(page.toString())));
        }
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, new WrittenBookContentComponent(
                RawFilteredPair.of("Дневник"), "???", 0, pages, true));

        // refresh the existing journal in place, otherwise slip it into the inventory
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(Items.WRITTEN_BOOK)) {
                WrittenBookContentComponent content = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
                if (content != null && "???".equals(content.author())) {
                    player.getInventory().setStack(slot, book);
                    player.playerScreenHandler.sendContentUpdates();
                    return;
                }
            }
        }
        if (!player.getInventory().insertStack(book)) {
            player.dropItem(book, false);
        }
        player.playerScreenHandler.sendContentUpdates();
    }
}
