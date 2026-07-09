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
    /** One horror level per half an in-game day: max horror by day five. */
    private static final long TICKS_PER_LEVEL = 12_000L;

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

    /** Brisk from the very start (~1.25 at level 0), ~2.0 at max; multiplies event rates. */
    public static double frequencyMultiplier(ServerWorld world) {
        return 1.25 + level(world) * 0.08;
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
