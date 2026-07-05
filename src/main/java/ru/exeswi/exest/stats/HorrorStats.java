package ru.exeswi.exest.stats;

import net.minecraft.server.network.ServerPlayerEntity;
import ru.exeswi.exest.world.HorrorWorldState;

/**
 * Per-player horror statistics, persisted with the world and shown by /horror stats.
 * The names are written from *its* point of view on purpose.
 */
public final class HorrorStats {

    public enum Stat {
        EVENTS("Событий пережито"),
        SEEN("Раз вы смотрели друг на друга"),
        CHASES_ESCAPED("Погонь пережито"),
        CHASES_CAUGHT("Раз оно догнало"),
        BEHIND_UNSEEN("Раз оно стояло за спиной, а ты не обернулся"),
        ABDUCTIONS("Похищений"),
        DEATHS("Смертей от него");

        public final String label;

        Stat(String label) {
            this.label = label;
        }
    }

    private HorrorStats() {
    }

    public static void inc(ServerPlayerEntity player, Stat stat) {
        HorrorWorldState.get(player.getServerWorld()).incStat(player.getUuid(), stat.ordinal());
    }

    public static int get(ServerPlayerEntity player, Stat stat) {
        return HorrorWorldState.get(player.getServerWorld()).getStats(player.getUuid())[stat.ordinal()];
    }
}
