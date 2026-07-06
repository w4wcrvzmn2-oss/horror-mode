package ru.exeswi.exest.events;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.exeswi.exest.networking.HorrorEffect;
import ru.exeswi.exest.networking.SoundCue;
import ru.exeswi.exest.sanity.SanityManager;
import ru.exeswi.exest.util.HorrorUtil;

import static ru.exeswi.exest.events.HorrorEvent.Category.AMBIENT;

/**
 * The constant low-grade dread: sounds with no source, doors that move on their own,
 * blocks that were not like that a minute ago. Frequent, subtle, never harmful.
 */
public final class AmbientEvents {

    private AmbientEvents() {
    }

    public static void registerAll() {
        HorrorEvent.builder("footsteps", AMBIENT).weight(20).cooldown(600)
                .action((p, m) -> {
                    m.cueAt(p, SoundCue.DISTANT_FOOTSTEPS, soundSpot(p, 8, 18), 0.9f);
                    SanityManager.modify(p, -0.5f);
                }).register();

        HorrorEvent.builder("breathing", AMBIENT).weight(12).cooldown(1200)
                .action((p, m) -> {
                    m.cueBehind(p, SoundCue.BREATHING, 0.55f);
                    SanityManager.modify(p, -1.5f);
                }).register();

        HorrorEvent.builder("whisper", AMBIENT).weight(14).cooldown(900)
                .action((p, m) -> {
                    m.cueBehind(p, SoundCue.WHISPER, 0.45f);
                    SanityManager.modify(p, -1.0f);
                }).register();

        HorrorEvent.builder("unknown_sound", AMBIENT).weight(12).cooldown(900)
                .action((p, m) -> m.cueAt(p, SoundCue.UNKNOWN, soundSpot(p, 6, 20), 0.7f)).register();

        HorrorEvent.builder("reversed_sound", AMBIENT).weight(8).cooldown(1600)
                .action((p, m) -> m.cueAt(p, SoundCue.REVERSED, soundSpot(p, 10, 24), 0.7f)).register();

        HorrorEvent.builder("fake_cave_sound", AMBIENT).weight(12).cooldown(900)
                .condition(p -> p.getServerWorld().isSkyVisible(p.getBlockPos()))
                .action((p, m) -> m.cueAt(p, SoundCue.CAVE, soundSpot(p, 8, 16), 1.0f)).register();

        HorrorEvent.builder("distant_crying", AMBIENT).weight(6).cooldown(2000)
                .action((p, m) -> {
                    m.cueAt(p, SoundCue.CRYING, soundSpot(p, 20, 40), 0.8f);
                    SanityManager.modify(p, -1.0f);
                }).register();

        HorrorEvent.builder("child_laugh", AMBIENT).weight(5).cooldown(2400)
                .condition(p -> SanityManager.get(p) < 85.0f)
                .action((p, m) -> {
                    m.cueBehind(p, SoundCue.CHILD_LAUGH, 0.5f);
                    SanityManager.modify(p, -2.0f);
                }).register();

        HorrorEvent.builder("radio_static", AMBIENT).weight(6).cooldown(2000)
                .action((p, m) -> m.cueAt(p, SoundCue.RADIO, soundSpot(p, 6, 14), 0.6f)).register();

        HorrorEvent.builder("heartbeat", AMBIENT).weight(8).cooldown(1200)
                .condition(p -> SanityManager.get(p) < 40.0f)
                .action((p, m) -> m.cueBehind(p, SoundCue.HEARTBEAT, 0.8f)).register();

        HorrorEvent.builder("distant_scream", AMBIENT).weight(6).cooldown(2400)
                .action((p, m) -> {
                    m.cueAt(p, SoundCue.SCREAM, soundSpot(p, 25, 45), 0.9f);
                    SanityManager.modify(p, -1.5f);
                }).register();

        // weight 0: manual test hook for the custom approach sting
        // (/horror event approach_sound)
        HorrorEvent.builder("approach_sound", AMBIENT).weight(0).cooldown(100)
                .action((p, m) -> m.cueBehind(p, SoundCue.APPROACH, 1.0f)).register();

        // the voices: a custom stereo recording that crawls into both ears at once.
        // rare and sanity-gated so it stays an event, not a background loop
        HorrorEvent.builder("voices", AMBIENT).weight(7).cooldown(6000)
                .enabledWhen(c -> c.audioIntensity > 0)
                .condition(p -> SanityManager.get(p) < 65.0f)
                .action((p, m) -> {
                    m.cueBehind(p, SoundCue.VOICES, 0.9f);
                    SanityManager.modify(p, -4.0f);
                }).register();

        // the lights go out: sudden near-total darkness for about five seconds
        HorrorEvent.builder("darkness_pulse", AMBIENT).weight(9).cooldown(3600)
                .enabledWhen(c -> c.enableDarkness)
                .action((p, m) -> {
                    m.mood(p, 0.92f, 0.35f, 0, 100);
                    m.cueBehind(p, SoundCue.WHISPER, 0.5f);
                    SanityManager.modify(p, -3.0f);
                }).register();

        HorrorEvent.builder("music_distort", AMBIENT).weight(6).cooldown(3000)
                .enabledWhen(c -> c.audioIntensity > 0)
                .action((p, m) -> m.effect(p, HorrorEffect.MUSIC_DISTORT, 1.0f, 400)).register();

        HorrorEvent.builder("door_moves", AMBIENT).weight(10).cooldown(1400)
                .condition(p -> findDoor(p) != null)
                .action(AmbientEvents::toggleDoor).register();

        HorrorEvent.builder("block_tamper", AMBIENT).weight(8).cooldown(1800)
                .action(AmbientEvents::tamperWithBlock).register();

        // someone knocks. Four, five times. Then the door slowly opens on its own,
        // and there is nobody outside
        HorrorEvent.builder("door_knock", AMBIENT).weight(9).cooldown(6000)
                .condition(p -> findDoor(p) != null)
                .action(AmbientEvents::knockSequence).register();
    }

    private static void knockSequence(ServerPlayerEntity player, HorrorEventManager manager) {
        BlockPos door = findDoor(player);
        if (door == null) {
            return;
        }
        ServerWorld world = player.getServerWorld();
        int knocks = 4 + world.random.nextInt(3);
        for (int i = 0; i < knocks; i++) {
            manager.schedule(player.getServer(), i * 11 + world.random.nextInt(4), () ->
                    world.playSound(null, door, net.minecraft.sound.SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,
                            net.minecraft.sound.SoundCategory.HOSTILE, 1.0f,
                            0.5f + world.random.nextFloat() * 0.15f));
        }
        manager.schedule(player.getServer(), knocks * 11 + 45, () -> {
            BlockState state = world.getBlockState(door);
            if (state.getBlock() instanceof DoorBlock doorBlock && !state.get(DoorBlock.OPEN)) {
                doorBlock.setOpen(null, world, state, door, true);
            }
            if (player.isAlive() && !player.isDisconnected()) {
                SanityManager.modify(player, -6.0f);
            }
        });
    }

    /** A believable point for a sound: real ground near the player, ideally unseen. */
    private static Vec3d soundSpot(ServerPlayerEntity player, double min, double max) {
        BlockPos pos = HorrorUtil.findGroundSpot(player.getServerWorld(), player, min, max, true,
                player.getServerWorld().random);
        if (pos != null) {
            return Vec3d.ofCenter(pos);
        }
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        double dist = min + player.getRandom().nextDouble() * (max - min);
        return player.getPos().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
    }

    private static BlockPos findDoor(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        for (BlockPos pos : BlockPos.iterateOutwards(player.getBlockPos(), 12, 4, 12)) {
            BlockState state = world.getBlockState(pos);
            if (state.isIn(BlockTags.WOODEN_DOORS) && state.getBlock() instanceof DoorBlock
                    && state.get(DoorBlock.HALF) == net.minecraft.block.enums.DoubleBlockHalf.LOWER) {
                return pos.toImmutable();
            }
        }
        return null;
    }

    private static void toggleDoor(ServerPlayerEntity player, HorrorEventManager manager) {
        BlockPos pos = findDoor(player);
        if (pos == null) {
            return;
        }
        ServerWorld world = player.getServerWorld();
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock door) {
            door.setOpen(null, world, state, pos, !state.get(DoorBlock.OPEN));
            SanityManager.modify(player, -2.0f);
        }
    }

    private static void tamperWithBlock(ServerPlayerEntity player, HorrorEventManager manager) {
        ServerWorld world = player.getServerWorld();
        BlockPos spot = HorrorUtil.findGroundSpot(world, player, 6.0, 14.0, true, world.random);
        if (spot == null) {
            return;
        }
        if (world.random.nextBoolean()) {
            // something built a little pile while you were not looking
            world.setBlockState(spot, world.random.nextBoolean()
                    ? Blocks.COBBLESTONE.getDefaultState() : Blocks.COARSE_DIRT.getDefaultState());
        } else {
            // or quietly took the ground apart
            BlockPos below = spot.down();
            if (world.getBlockState(below).getHardness(world, below) >= 0) {
                world.breakBlock(below, false);
            }
        }
    }
}
