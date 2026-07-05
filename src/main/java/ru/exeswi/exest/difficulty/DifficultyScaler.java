package ru.exeswi.exest.difficulty;

import net.minecraft.server.world.ServerWorld;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.world.HorrorWorldState;

/**
 * Converts survived time into a horror level 0..10. Everything scales off this:
 * event frequency, monster stats, darkness depth, sanity drain and which
 * creatures are unlocked.
 */
public final class DifficultyScaler {

    public static final int MAX_LEVEL = 10;
    /** One horror level per two in-game days at difficultyScale = 1. */
    private static final long TICKS_PER_LEVEL = 48_000L;

    private DifficultyScaler() {
    }

    public static int level(ServerWorld world) {
        double scale = ConfigManager.get().difficultyScale;
        if (scale <= 0) {
            return 0;
        }
        long ticks = HorrorWorldState.get(world).horrorTicks;
        return (int) Math.min(MAX_LEVEL, ticks * scale / TICKS_PER_LEVEL);
    }

    /** 1.0 at level 0 growing to ~2.0 at max level; multiplies event rates. */
    public static double frequencyMultiplier(ServerWorld world) {
        return 1.0 + level(world) * 0.1;
    }

    /** Extra attack damage granted to freshly spawned horror mobs. */
    public static double bonusDamage(ServerWorld world) {
        return level(world) * 0.5;
    }

    /** Extra darkness contributed to the client lightmap, 0..0.3. */
    public static float bonusDarkness(ServerWorld world) {
        return level(world) * 0.03f;
    }

    /** Sanity drains faster as the horror matures. */
    public static float sanityDrainMultiplier(ServerWorld world) {
        return 1.0f + level(world) * 0.15f;
    }
}
