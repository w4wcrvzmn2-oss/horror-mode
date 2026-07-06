package ru.exeswi.exest.sanity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.difficulty.DifficultyScaler;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;
import ru.exeswi.exest.networking.HorrorNetworking;
import ru.exeswi.exest.world.HorrorWorldState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hidden per-player sanity, 0..100. Darkness and monster proximity erode it,
 * daylight and sleep restore it. The value is persisted in {@link HorrorWorldState}
 * and mirrored to the client for effect scaling.
 */
public final class SanityManager {

    private static final Map<UUID, Float> lastSynced = new HashMap<>();
    private static final Map<UUID, Integer> lastSyncedLevel = new HashMap<>();
    private static final Map<UUID, Boolean> wasSleeping = new HashMap<>();
    private static final Map<UUID, Boolean> yankRolled = new HashMap<>();

    private SanityManager() {
    }

    public static float get(ServerPlayerEntity player) {
        return HorrorWorldState.get(player.getServerWorld()).getSanity(player.getUuid());
    }

    public static void modify(ServerPlayerEntity player, float delta) {
        if (!ConfigManager.get().enableSanity) {
            return;
        }
        HorrorWorldState state = HorrorWorldState.get(player.getServerWorld());
        state.setSanity(player.getUuid(), state.getSanity(player.getUuid()) + delta);
        syncIfChanged(player);
    }

    /** Called once per second from the main tick loop. */
    public static void tick(MinecraftServer server) {
        if (!ConfigManager.get().enableSanity) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        float drainMul = DifficultyScaler.sanityDrainMultiplier(world);
        float delta = 0.0f;

        BlockPos pos = player.getBlockPos();
        int light = world.getLightLevel(pos);
        if (light < 4) {
            delta -= 0.09f * drainMul;
        } else if (light > 10 && world.isDay()) {
            // daylight soothes, but far slower than the night wounds
            delta += 0.03f;
        }
        // night gnaws at the mind even in a lit base
        if (world.isNight()) {
            delta -= 0.03f * drainMul;
        }

        // being near an active horror creature is the worst thing for the mind
        boolean monsterNearby = !world.getEntitiesByClass(AbstractHorrorEntity.class,
                Box.of(player.getPos(), 32, 16, 32), e -> true).isEmpty();
        if (monsterNearby) {
            delta -= 0.15f * drainMul;
        }

        boolean sleeping = player.isSleeping();
        if (Boolean.TRUE.equals(wasSleeping.get(player.getUuid())) && !sleeping) {
            delta += handleWakeUp(player, world);
        }
        // mid-sleep: sometimes the night decides you are not allowed to skip it —
        // 15% you are ripped out of bed, another 25% something is already at the bed
        if (sleeping && !Boolean.TRUE.equals(yankRolled.get(player.getUuid()))
                && player.getSleepTimer() > 80) {
            yankRolled.put(player.getUuid(), true);
            if (ConfigManager.get().enableJumpscares) {
                float roll = player.getRandom().nextFloat();
                if (roll < 0.15f) {
                    yankFromBed(player, world);
                } else if (roll < 0.40f && ConfigManager.get().enableMonsters) {
                    nightVisitor(player, world);
                }
            }
        }
        if (!sleeping) {
            yankRolled.put(player.getUuid(), false);
        }
        wasSleeping.put(player.getUuid(), sleeping);

        if (delta != 0.0f) {
            HorrorWorldState state = HorrorWorldState.get(world);
            state.setSanity(player.getUuid(), state.getSanity(player.getUuid()) + delta);
        }
        syncIfChanged(player);
    }

    /**
     * Sleep usually restores a good chunk of sanity — but sometimes you wake up a few
     * blocks from where you lay down, to the sound of quiet laughter. Sleeping stops
     * being the guaranteed safe skip through the night.
     */
    private static float handleWakeUp(ServerPlayerEntity player, ServerWorld world) {
        if (ConfigManager.get().enableJumpscares && player.getRandom().nextFloat() < 0.25f) {
            BlockPos spot = ru.exeswi.exest.util.HorrorUtil.findGroundSpot(
                    world, player, 2.0, 6.0, false, world.random);
            if (spot != null) {
                player.teleport(world, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
                        player.getYaw(), player.getPitch());
                HorrorNetworking.sendCueBehind(player, ru.exeswi.exest.networking.SoundCue.CHILD_LAUGH,
                        0.5f * (float) ConfigManager.get().audioIntensity);
                return -5.0f;
            }
        }
        return 40.0f;
    }

    public static void syncIfChanged(ServerPlayerEntity player) {
        float value = get(player);
        int level = DifficultyScaler.level(player.getServerWorld());
        Float last = lastSynced.get(player.getUuid());
        Integer lastLevel = lastSyncedLevel.get(player.getUuid());
        if (last == null || Math.abs(last - value) >= 0.5f
                || lastLevel == null || lastLevel != level) {
            lastSynced.put(player.getUuid(), value);
            lastSyncedLevel.put(player.getUuid(), level);
            HorrorNetworking.sendSanity(player, value, level);
        }
    }

    public static void forceSync(ServerPlayerEntity player) {
        lastSynced.remove(player.getUuid());
        lastSyncedLevel.remove(player.getUuid());
        syncIfChanged(player);
    }

    /**
     * The bed yank: just as the screen fades to black, something rips the player out
     * of bed and drops them a few blocks away in the dark, wide awake.
     */
    private static void yankFromBed(ServerPlayerEntity player, ServerWorld world) {
        player.wakeUp(true, true);
        BlockPos darkSpot = null;
        for (int i = 0; i < 10 && darkSpot == null; i++) {
            BlockPos candidate = ru.exeswi.exest.util.HorrorUtil.findGroundSpot(
                    world, player, 4.0, 9.0, false, world.random);
            if (candidate != null && world.getLightLevel(candidate) < 6) {
                darkSpot = candidate;
            }
        }
        if (darkSpot == null) {
            darkSpot = ru.exeswi.exest.util.HorrorUtil.findGroundSpot(
                    world, player, 4.0, 9.0, false, world.random);
        }
        if (darkSpot != null) {
            player.teleport(world, darkSpot.getX() + 0.5, darkSpot.getY(), darkSpot.getZ() + 0.5,
                    player.getYaw(), player.getPitch());
        }
        HorrorNetworking.sendEffect(player, ru.exeswi.exest.networking.HorrorEffect.GLITCH, 1.0f, 15);
        HorrorNetworking.sendCueBehind(player, ru.exeswi.exest.networking.SoundCue.STING,
                (float) ConfigManager.get().audioIntensity);
        HorrorNetworking.sendMood(player, 0.85f, 0.3f, 0, 120);
        modify(player, -12.0f);
        ru.exeswi.exest.events.ApparitionSpawner.spawnApparition(player,
                ru.exeswi.exest.events.ApparitionSpawner.Placement.BEHIND);
    }

    /**
     * The night visitor: sleep is interrupted and something is already standing at
     * the bed, hunting. Sleeping through the horror stops being a free skip button.
     */
    private static void nightVisitor(ServerPlayerEntity player, ServerWorld world) {
        BlockPos spot = ru.exeswi.exest.util.HorrorUtil.findGroundSpot(
                world, player, 2.0, 5.0, false, world.random);
        if (spot == null) {
            return;
        }
        var type = world.random.nextBoolean()
                ? ru.exeswi.exest.registry.ModEntities.STALKER
                : ru.exeswi.exest.registry.ModEntities.SMILER;
        ru.exeswi.exest.entity.base.AbstractHorrorEntity visitor = type.create(world);
        if (visitor == null) {
            return;
        }
        visitor.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
                world.random.nextFloat() * 360.0f, 0.0f);
        visitor.initialize(world, world.getLocalDifficulty(spot),
                net.minecraft.entity.SpawnReason.EVENT, null);
        visitor.setPersistent();
        if (!world.spawnEntity(visitor)) {
            return;
        }
        player.wakeUp(true, true);
        visitor.enrage(player);
        HorrorNetworking.sendCueBehind(player, ru.exeswi.exest.networking.SoundCue.STING,
                (float) ConfigManager.get().audioIntensity);
        HorrorNetworking.sendMood(player, 0.6f, 0.2f, 0, 150);
        modify(player, -10.0f);
    }

    /** Sanity below this triggers hallucination-grade events. */
    public static boolean isLow(ServerPlayerEntity player) {
        return get(player) < 35.0f;
    }
}
