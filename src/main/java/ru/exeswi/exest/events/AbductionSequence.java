package ru.exeswi.exest.events;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.entity.StalkerEntity;
import ru.exeswi.exest.networking.HorrorEffect;
import ru.exeswi.exest.networking.HorrorNetworking;
import ru.exeswi.exest.networking.SoundCue;
import ru.exeswi.exest.registry.ModEntities;
import ru.exeswi.exest.sanity.SanityManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Abduction: a scripted multi-phase sequence that can greet a player who just
 * joined or respawned.
 *
 * 1. Something hurls the player far into the sky.
 * 2. On the way down a screaming face fills the screen — and just before impact the
 *    player is tossed back up. Possibly more than once.
 * 3. Instead of the killing blow of the ground: a sealed chamber. The player hangs
 *    between chains, unable to move, while the monster closes the distance in short
 *    left-right teleport jerks until it is touching your face. Then it is over.
 *
 * The fall never deals damage; the room is carved deep underground and fully restored
 * afterwards. Each phase degrades gracefully (water, caves, disconnects, timeouts).
 */
public final class AbductionSequence {

    private enum Phase { LAUNCH, AIRBORNE, ROOM, DONE }

    private static final Map<UUID, AbductionSequence> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> LAST_RUN = new HashMap<>();

    /** join/respawn triggers scheduled a few seconds into the future. */
    private record PendingStart(UUID player, long startAtTick) {
    }

    private static final List<PendingStart> PENDING = new ArrayList<>();

    private final UUID playerId;
    private Phase phase = Phase.LAUNCH;
    private int ticks;
    private int bounces;
    private final int maxBounces;
    private boolean faceShownThisFall;
    private Vec3d origin;

    // room phase
    private final Map<BlockPos, BlockState> savedBlocks = new HashMap<>();
    private Vec3d chainSpot;
    private StalkerEntity monster;
    private double monsterDistance;
    private int hopSide = 1;
    private int hopTimer;
    private int faceToFaceTicks;
    private int doneTicks;

    private AbductionSequence(ServerPlayerEntity player) {
        this.playerId = player.getUuid();
        this.origin = player.getPos();
        this.maxBounces = 1 + player.getRandom().nextInt(2);
    }

    // --- lifecycle ---

    /** Rolls the dice for a player who just entered the world. */
    public static void maybeSchedule(ServerPlayerEntity player) {
        if (!ConfigManager.get().enableJumpscares
                || player.isCreative() || player.isSpectator()
                || ACTIVE.containsKey(player.getUuid())) {
            return;
        }
        long now = player.getServerWorld().getTime();
        if (now - LAST_RUN.getOrDefault(player.getUuid(), Long.MIN_VALUE) < 24000) {
            return;
        }
        if (player.getRandom().nextDouble() >= ConfigManager.get().abductionChance) {
            return;
        }
        // give the world a few seconds to load and the player a false sense of safety
        PENDING.add(new PendingStart(player.getUuid(),
                player.getServer().getTicks() + 120 + player.getRandom().nextInt(200)));
    }

    /** Starts immediately (also used by /horror event abduction). */
    public static void start(ServerPlayerEntity player) {
        if (ACTIVE.containsKey(player.getUuid()) || player.isSpectator()) {
            return;
        }
        if (!player.getServerWorld().isSkyVisible(player.getBlockPos())) {
            return; // needs open sky to make sense
        }
        LAST_RUN.put(player.getUuid(), player.getServerWorld().getTime());
        ACTIVE.put(player.getUuid(), new AbductionSequence(player));
    }

    public static boolean isActive(ServerPlayerEntity player) {
        return ACTIVE.containsKey(player.getUuid());
    }

    /** A player vanishing mid-flight must not fall to their death on reconnect. */
    public static void onDisconnect(ServerPlayerEntity player) {
        AbductionSequence sequence = ACTIVE.remove(player.getUuid());
        if (sequence == null) {
            return;
        }
        ServerWorld world = player.getServerWorld();
        if (sequence.phase == Phase.AIRBORNE || sequence.phase == Phase.LAUNCH || sequence.phase == Phase.ROOM) {
            player.teleport(world, sequence.origin.x, sequence.origin.y, sequence.origin.z,
                    player.getYaw(), player.getPitch());
            player.fallDistance = 0.0f;
        }
        sequence.cleanup(world);
    }

    public static void tick(MinecraftServer server) {
        if (!PENDING.isEmpty()) {
            Iterator<PendingStart> it = PENDING.iterator();
            while (it.hasNext()) {
                PendingStart pending = it.next();
                if (server.getTicks() >= pending.startAtTick) {
                    it.remove();
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(pending.player);
                    if (player != null) {
                        start(player);
                    }
                }
            }
        }
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, AbductionSequence>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            AbductionSequence sequence = it.next().getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(sequence.playerId);
            if (player == null) {
                sequence.cleanup(server.getOverworld());
                it.remove();
                continue;
            }
            if (!sequence.tickSequence(player)) {
                sequence.cleanup(player.getServerWorld());
                it.remove();
            }
        }
    }

    // --- the sequence itself ---

    private boolean tickSequence(ServerPlayerEntity player) {
        ticks++;
        // the fall is theatre: it must never kill by itself
        player.fallDistance = 0.0f;
        if (ticks > 2400) {
            return false; // hard safety timeout, put everything back
        }
        switch (phase) {
            case LAUNCH -> tickLaunch(player);
            case AIRBORNE -> tickAirborne(player);
            case ROOM -> tickRoom(player);
            case DONE -> {
                if (++doneTicks > 80) {
                    return false;
                }
            }
        }
        return true;
    }

    private void tickLaunch(ServerPlayerEntity player) {
        origin = player.getPos();
        player.setVelocity(0.0, 4.6, 0.0);
        player.velocityModified = true;
        HorrorNetworking.sendEffect(player, HorrorEffect.GLITCH, 0.8f, 12);
        HorrorNetworking.sendCueBehind(player, SoundCue.SCREAM, 1.0f);
        SanityManager.modify(player, -10.0f);
        phase = Phase.AIRBORNE;
    }

    private void tickAirborne(ServerPlayerEntity player) {
        if (player.isFallFlying()) {
            player.stopFallFlying(); // no gliding out of this
        }
        boolean falling = player.getVelocity().y < -0.4;
        if (falling && !faceShownThisFall) {
            faceShownThisFall = true;
            HorrorNetworking.sendEffect(player, HorrorEffect.JUMPSCARE_FACE, 1.0f, 18);
            HorrorNetworking.sendEffect(player, HorrorEffect.STATIC, 0.6f, 18);
            HorrorNetworking.sendCueBehind(player, SoundCue.SCREAM, 1.2f);
        }
        boolean lowEnough = falling
                && (nearGround(player) || player.isTouchingWater()
                || player.getY() < player.getServerWorld().getBottomY() + 6);
        if (!lowEnough && !player.isOnGround()) {
            return;
        }
        if (bounces < maxBounces && !player.isTouchingWater()) {
            // caught a breath from the ground — and thrown right back up
            bounces++;
            faceShownThisFall = false;
            player.setVelocity((player.getRandom().nextDouble() - 0.5) * 0.4, 4.2, (player.getRandom().nextDouble() - 0.5) * 0.4);
            player.velocityModified = true;
            HorrorNetworking.sendEffect(player, HorrorEffect.STATIC, 0.5f, 8);
            player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0f, 0.3f);
        } else {
            enterRoom(player);
        }
    }

    private void enterRoom(ServerPlayerEntity player) {
        ru.exeswi.exest.stats.HorrorStats.inc(player, ru.exeswi.exest.stats.HorrorStats.Stat.ABDUCTIONS);
        ServerWorld world = player.getServerWorld();
        BlockPos center = new BlockPos((int) Math.floor(origin.x),
                Math.max(world.getBottomY() + 10, -50), (int) Math.floor(origin.z));
        buildRoom(world, center);

        chainSpot = new Vec3d(center.getX() - 1.5, center.getY() + 1, center.getZ() + 0.5);
        player.setVelocity(Vec3d.ZERO);
        player.velocityModified = true;
        player.teleport(world, chainSpot.x, chainSpot.y, chainSpot.z, -90.0f, 5.0f);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 1200, 250, true, false));
        HorrorNetworking.sendMood(player, 0.55f, 0.6f, 0, 900);
        HorrorNetworking.sendEffect(player, HorrorEffect.FLASH, 1.0f, 4);
        HorrorNetworking.sendCueBehind(player, SoundCue.HEARTBEAT, 1.0f);

        monster = ModEntities.STALKER.create(world);
        if (monster != null) {
            monsterDistance = 5.0;
            Vec3d spawn = monsterPos(0.0);
            monster.refreshPositionAndAngles(spawn.x, spawn.y, spawn.z, 90.0f, 0.0f);
            monster.setAiDisabled(true);
            monster.setPersistent();
            world.spawnEntity(monster);
        }
        hopTimer = 25;
        phase = Phase.ROOM;
    }

    private void tickRoom(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        // the chains hold: any attempt to move is snapped back
        if (player.getPos().squaredDistanceTo(chainSpot) > 1.0) {
            player.teleport(world, chainSpot.x, chainSpot.y, chainSpot.z,
                    player.getYaw(), player.getPitch());
        }
        if (ticks % 20 == 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 250, true, false));
        }
        if (monster == null || monster.isRemoved()) {
            phase = Phase.DONE; // someone/something removed it; let the player live
            return;
        }
        monster.lookAtEntity(player, 360.0f, 90.0f);
        monster.setHeadYaw(monster.getYaw());
        monster.setBodyYaw(monster.getYaw());

        if (monsterDistance <= 1.0) {
            // it just stands there, breathing into your face, before it ends you
            if (++faceToFaceTicks == 30) {
                HorrorNetworking.sendEffect(player, HorrorEffect.JUMPSCARE_FACE, 1.0f, 25);
                HorrorNetworking.sendCueBehind(player, SoundCue.SCREAM, 1.4f);
                player.damage(world.getDamageSources().mobAttack(monster), 500.0f);
                SanityManager.modify(player, -20.0f);
                phase = Phase.DONE;
            }
            return;
        }
        if (--hopTimer > 0) {
            return;
        }
        // the approach: a short teleport hop, jerking right, then left, then right...
        hopTimer = 7 + player.getRandom().nextInt(6);
        hopSide = -hopSide;
        monsterDistance = Math.max(0.9, monsterDistance - (0.5 + player.getRandom().nextDouble() * 0.4));
        double lateral = hopSide * monsterDistance * 0.35;
        Vec3d pos = monsterPos(lateral);
        monster.refreshPositionAndAngles(pos.x, pos.y, pos.z, 90.0f, 0.0f);
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.8f, 0.3f);
        if (monsterDistance < 2.5) {
            HorrorNetworking.sendEffect(player, HorrorEffect.GLITCH, 0.5f, 6);
        }
    }

    /** Monster stands east of the chained player (+X), offset sideways along Z. */
    private Vec3d monsterPos(double lateral) {
        return new Vec3d(chainSpot.x + monsterDistance, chainSpot.y, chainSpot.z + lateral);
    }

    // --- the chamber ---

    private void buildRoom(ServerWorld world, BlockPos center) {
        BlockState shell = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    boolean isShell = Math.abs(dx) == 4 || Math.abs(dz) == 4 || dy == 0 || dy == 4;
                    place(world, pos, isShell ? shell : air);
                }
            }
        }
        // chains hanging from the ceiling on both sides of the victim
        BlockState chain = Blocks.CHAIN.getDefaultState();
        for (int side : new int[]{-1, 1}) {
            place(world, center.add(-2, 3, side), chain);
            place(world, center.add(-2, 2, side), chain);
            place(world, center.add(-1, 3, side), chain);
        }
        // one dim soul lantern above where it will be standing
        place(world, center.add(3, 3, 0),
                Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
    }

    private void place(ServerWorld world, BlockPos pos, BlockState state) {
        BlockState current = world.getBlockState(pos);
        if (!current.equals(state)) {
            savedBlocks.putIfAbsent(pos.toImmutable(), current);
            world.setBlockState(pos, state);
        }
    }

    private void cleanup(ServerWorld world) {
        if (monster != null && !monster.isRemoved()) {
            monster.discard();
        }
        for (Map.Entry<BlockPos, BlockState> entry : savedBlocks.entrySet()) {
            world.setBlockState(entry.getKey(), entry.getValue());
        }
        savedBlocks.clear();
    }

    private static boolean nearGround(ServerPlayerEntity player) {
        // terminal fall speed is ~4 blocks per tick, so scan deep enough to react in time
        BlockPos pos = player.getBlockPos();
        for (int i = 1; i <= 8; i++) {
            BlockPos below = pos.down(i);
            if (!player.getServerWorld().getBlockState(below)
                    .getCollisionShape(player.getServerWorld(), below).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
