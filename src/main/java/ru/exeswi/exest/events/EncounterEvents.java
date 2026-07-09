package ru.exeswi.exest.events;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;
import ru.exeswi.exest.events.ApparitionSpawner.Placement;
import ru.exeswi.exest.networking.HorrorEffect;
import ru.exeswi.exest.networking.SoundCue;
import ru.exeswi.exest.registry.ModEntities;
import ru.exeswi.exest.sanity.SanityManager;
import ru.exeswi.exest.util.HorrorUtil;

import static ru.exeswi.exest.events.HorrorEvent.Category.ENCOUNTER;

/**
 * Sightings and spawns: figures on hills, faces in windows, something in the water,
 * a shape for a single frame — and the real hunters that follow you home. The visible
 * ones announce themselves with a faint sound so the player actually turns and looks.
 */
public final class EncounterEvents {

    private EncounterEvents() {
    }

    public static void registerAll() {
        apparition("apparition_behind", Placement.BEHIND, 8, 2400);
        apparition("apparition_forest", Placement.FOREST, 8, 2400);
        apparition("apparition_window", Placement.WINDOW, 6, 3600);
        apparition("apparition_underwater", Placement.UNDERWATER, 4, 3600);

        // a figure standing openly in your field of view, announced by a whisper
        HorrorEvent.builder("apparition_distance", ENCOUNTER).weight(14).cooldown(1600)
                .enabledWhen(c -> c.enableMonsters)
                .action((p, m) -> {
                    AbstractHorrorEntity ghost = ApparitionSpawner.spawnApparition(p, Placement.IN_VIEW);
                    if (ghost == null) {
                        ghost = ApparitionSpawner.spawnApparition(p, Placement.DISTANCE);
                    }
                    if (ghost != null) {
                        m.cueAt(p, SoundCue.WHISPER, ghost.getPos(), 0.9f);
                        SanityManager.modify(p, -2.0f);
                    }
                }).register();

        // the sharp one: the world goes dead silent, then it is suddenly RIGHT THERE
        HorrorEvent.builder("close_encounter", ENCOUNTER).weight(13).cooldown(3600)
                .enabledWhen(c -> c.enableMonsters && c.enableJumpscares)
                .action((p, m) -> m.silenceThen(p, 60, () -> {
                    AbstractHorrorEntity ghost = ApparitionSpawner.spawnApparition(p, Placement.IN_VIEW);
                    if (ghost != null) {
                        m.cueAt(p, SoundCue.STING, ghost.getPos(), 1.0f);
                        m.effect(p, HorrorEffect.GLITCH, 0.9f, 14);
                        SanityManager.modify(p, -6.0f);
                    }
                })).register();

        // total silence... then heavy breathing right behind you. Turn around.
        HorrorEvent.builder("behind_you", ENCOUNTER).weight(10).cooldown(4800)
                .enabledWhen(c -> c.enableMonsters && c.enableJumpscares)
                .action((p, m) -> m.silenceThen(p, 50, () -> {
                    AbstractHorrorEntity ghost = ApparitionSpawner.spawnApparition(p, Placement.BEHIND);
                    if (ghost != null) {
                        m.cueBehind(p, SoundCue.BREATHING, 1.0f);
                        SanityManager.modify(p, -5.0f);
                    }
                })).register();

        // THE CHASE. Silence, a sting, and something sprinting at your back. Running
        // into bright light shakes it off; getting caught costs one brutal hit.
        HorrorEvent.builder("night_chase", ENCOUNTER).weight(10).cooldown(12000)
                .enabledWhen(c -> c.enableMonsters && c.enableJumpscares)
                .condition(p -> p.getServerWorld().isNight())
                .action((p, m) -> m.silenceThen(p, 70, () -> startChase(p, m))).register();

        // daylight is not a rule you can rely on. Rare, late-game, unforgettable.
        HorrorEvent.builder("day_chase", ENCOUNTER).weight(2).cooldown(24000).minDifficulty(3)
                .enabledWhen(c -> c.enableMonsters && c.enableJumpscares)
                .condition(p -> p.getServerWorld().isDay())
                .action((p, m) -> m.silenceThen(p, 70, () -> startChase(p, m))).register();

        HorrorEvent.builder("apparition_hill", ENCOUNTER).weight(8).cooldown(2400)
                .enabledWhen(c -> c.enableMonsters)
                .condition(p -> p.getServerWorld().isSkyVisible(p.getBlockPos()))
                .action((p, m) -> {
                    if (ApparitionSpawner.spawnApparition(p, Placement.HILL) != null) {
                        SanityManager.modify(p, -2.0f);
                    }
                }).register();

        HorrorEvent.builder("apparition_cave", ENCOUNTER).weight(10).cooldown(1800)
                .enabledWhen(c -> c.enableMonsters)
                .condition(ApparitionSpawner::isUnderground)
                .action((p, m) -> {
                    AbstractHorrorEntity ghost = ApparitionSpawner.spawnApparition(p, Placement.CAVE);
                    if (ghost != null) {
                        m.cueAt(p, SoundCue.UNKNOWN, ghost.getPos(), 0.8f);
                        SanityManager.modify(p, -3.0f);
                    }
                }).register();

        HorrorEvent.builder("apparition_one_frame", ENCOUNTER).weight(7).cooldown(3600)
                .enabledWhen(c -> c.enableMonsters && c.enableJumpscares)
                .action((p, m) -> {
                    if (ApparitionSpawner.spawnApparition(p, Placement.ONE_FRAME) != null) {
                        m.effect(p, HorrorEffect.STATIC, 0.6f, 6);
                        m.cueBehind(p, SoundCue.STING, 0.7f);
                        SanityManager.modify(p, -5.0f);
                    }
                }).register();

        HorrorEvent.builder("screen_runner", ENCOUNTER).weight(6).cooldown(3600)
                .enabledWhen(c -> c.enableVisualEffects && c.enableJumpscares)
                .action((p, m) -> {
                    m.effect(p, HorrorEffect.SCREEN_RUNNER, 1.0f, 14);
                    m.cueBehind(p, SoundCue.STING, 0.9f);
                    SanityManager.modify(p, -4.0f);
                }).register();

        HorrorEvent.builder("hunter_spawn", ENCOUNTER).weight(20).cooldown(1200)
                .enabledWhen(c -> c.enableMonsters && c.spawnRateMultiplier > 0)
                .condition(p -> p.getRandom().nextDouble() < ConfigManager.get().spawnRateMultiplier)
                .action((p, m) -> ApparitionSpawner.spawnHunter(p)).register();

        HorrorEvent.builder("pack_hunt", ENCOUNTER).weight(6).cooldown(6000).minDifficulty(3)
                .enabledWhen(c -> c.enableMonsters && c.spawnRateMultiplier > 0)
                .action((p, m) -> {
                    int packSize = 2 + p.getRandom().nextInt(2);
                    for (int i = 0; i < packSize; i++) {
                        ApparitionSpawner.spawnHunter(p);
                    }
                    SanityManager.modify(p, -3.0f);
                }).register();

        HorrorEvent.builder("fake_mob", ENCOUNTER).weight(8).cooldown(1800)
                .enabledWhen(c -> c.enableHallucinations)
                .action((p, m) -> m.effect(p, HorrorEffect.FAKE_MOB, 1.0f, 300)).register();

        HorrorEvent.builder("fake_player", ENCOUNTER).weight(6).cooldown(3600)
                .enabledWhen(c -> c.enableHallucinations)
                .action((p, m) -> {
                    m.effect(p, HorrorEffect.FAKE_PLAYER, 1.0f, 500);
                    SanityManager.modify(p, -2.0f);
                }).register();
    }

    private static void startChase(ServerPlayerEntity player, HorrorEventManager manager) {
        ServerWorld world = player.getServerWorld();
        EntityType<? extends AbstractHorrorEntity> type =
                world.random.nextBoolean() ? ModEntities.STALKER : ModEntities.SMILER;
        BlockPos pos = HorrorUtil.posBehindPlayer(world, player, 14.0);
        if (pos == null) {
            pos = HorrorUtil.findGroundSpot(world, player, 10.0, 18.0, true, world.random);
        }
        if (pos == null) {
            return;
        }
        AbstractHorrorEntity hunter = type.create(world);
        if (hunter == null) {
            return;
        }
        hunter.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                world.random.nextFloat() * 360.0f, 0.0f);
        hunter.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null);
        hunter.applyDifficultyBonuses(world);
        hunter.setPersistent();
        if (!world.spawnEntity(hunter)) {
            return;
        }
        hunter.enrage(player);
        manager.cueBehind(player, SoundCue.STING, 1.1f);
        manager.cueBehind(player, SoundCue.HEARTBEAT, 1.0f);
        manager.mood(player, 0.45f, 0.2f, 0, 200);
        SanityManager.modify(player, -8.0f);
    }

    private static void apparition(String id, Placement placement, int weight, long cooldown) {
        HorrorEvent.builder(id, ENCOUNTER).weight(weight).cooldown(cooldown)
                .enabledWhen(c -> c.enableMonsters)
                .action((p, m) -> {
                    if (ApparitionSpawner.spawnApparition(p, placement) != null) {
                        SanityManager.modify(p, -2.0f);
                    }
                }).register();
    }
}
