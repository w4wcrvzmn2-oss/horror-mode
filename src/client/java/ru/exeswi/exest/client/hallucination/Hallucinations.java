package ru.exeswi.exest.client.hallucination;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Purely client-side lies: ore that was never in the wall, a mob nobody else can see,
 * a player who is not on the server. Everything is tracked and quietly reverted, and
 * since none of it exists server-side, it can never actually hurt you. Physically.
 */
public final class Hallucinations {

    private record FakeBlock(BlockPos pos, BlockState fake, BlockState original, int expireAt) {
    }

    private record FakeEntity(Entity entity, int expireAt, double vanishDistance) {
    }

    private static final String[] GHOST_NAMES = {"Marcus_", "elena2010", "unknown", "Observer", "whoami"};

    private static final List<FakeBlock> FAKE_BLOCKS = new ArrayList<>();
    private static final List<FakeEntity> FAKE_ENTITIES = new ArrayList<>();
    private static final Random RANDOM = Random.create();

    private static int fakeEntityId = -1000;
    private static int clientTicks;

    private Hallucinations() {
    }

    public static void spawnFakeOre(MinecraftClient client, int durationTicks) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            return;
        }
        for (int attempt = 0; attempt < 30; attempt++) {
            BlockPos pos = client.player.getBlockPos().add(
                    RANDOM.nextInt(13) - 6, RANDOM.nextInt(7) - 3, RANDOM.nextInt(13) - 6);
            BlockState state = world.getBlockState(pos);
            if (!state.isOf(Blocks.STONE) && !state.isOf(Blocks.DEEPSLATE)) {
                continue;
            }
            BlockState fake = (state.isOf(Blocks.DEEPSLATE)
                    ? Blocks.DEEPSLATE_DIAMOND_ORE : Blocks.DIAMOND_ORE).getDefaultState();
            world.setBlockState(pos, fake);
            FAKE_BLOCKS.add(new FakeBlock(pos, fake, state, clientTicks + durationTicks));
            return;
        }
    }

    public static void spawnFakeMob(MinecraftClient client, int durationTicks) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            return;
        }
        Vec3d pos = spotNearPlayer(client, 8.0, 16.0);
        ZombieEntity zombie = new ZombieEntity(EntityType.ZOMBIE, world);
        zombie.setId(fakeEntityId--);
        zombie.refreshPositionAndAngles(pos.x, pos.y, pos.z, RANDOM.nextFloat() * 360.0f, 0.0f);
        world.addEntity(zombie);
        FAKE_ENTITIES.add(new FakeEntity(zombie, clientTicks + durationTicks, 5.0));
    }

    public static void spawnFakePlayer(MinecraftClient client, int durationTicks) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            return;
        }
        Vec3d pos = spotNearPlayer(client, 18.0, 30.0);
        GameProfile profile = new GameProfile(UUID.randomUUID(),
                GHOST_NAMES[RANDOM.nextInt(GHOST_NAMES.length)]);
        OtherClientPlayerEntity ghost = new OtherClientPlayerEntity(world, profile);
        ghost.setId(fakeEntityId--);
        ghost.refreshPositionAndAngles(pos.x, pos.y, pos.z, RANDOM.nextFloat() * 360.0f, 0.0f);
        world.addEntity(ghost);
        FAKE_ENTITIES.add(new FakeEntity(ghost, clientTicks + durationTicks, 10.0));
    }

    public static void tick(MinecraftClient client) {
        clientTicks++;
        if (client.world == null || client.player == null) {
            if (!FAKE_BLOCKS.isEmpty() || !FAKE_ENTITIES.isEmpty()) {
                FAKE_BLOCKS.clear();
                FAKE_ENTITIES.clear();
            }
            return;
        }
        Iterator<FakeBlock> blocks = FAKE_BLOCKS.iterator();
        while (blocks.hasNext()) {
            FakeBlock fake = blocks.next();
            if (clientTicks >= fake.expireAt) {
                // only revert if our lie is still in place; the server may have corrected it
                if (client.world.getBlockState(fake.pos).equals(fake.fake)) {
                    client.world.setBlockState(fake.pos, fake.original);
                }
                blocks.remove();
            }
        }
        Iterator<FakeEntity> entities = FAKE_ENTITIES.iterator();
        while (entities.hasNext()) {
            FakeEntity fake = entities.next();
            boolean expired = clientTicks >= fake.expireAt;
            boolean approached = client.player.squaredDistanceTo(fake.entity)
                    < fake.vanishDistance * fake.vanishDistance;
            if (expired || approached || fake.entity.isRemoved()) {
                fake.entity.discard();
                entities.remove();
            }
        }
    }

    private static Vec3d spotNearPlayer(MinecraftClient client, double min, double max) {
        Vec3d playerPos = client.player.getPos();
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = min + RANDOM.nextDouble() * (max - min);
            double x = playerPos.x + Math.cos(angle) * dist;
            double z = playerPos.z + Math.sin(angle) * dist;
            BlockPos.Mutable pos = BlockPos.ofFloored(x, playerPos.y + 4, z).mutableCopy();
            for (int dy = 0; dy < 10; dy++) {
                if (client.world.getBlockState(pos).isAir()
                        && !client.world.getBlockState(pos.down()).isAir()) {
                    return new Vec3d(x, pos.getY(), z);
                }
                pos.move(0, -1, 0);
            }
        }
        return playerPos.add(RANDOM.nextDouble() * 10 - 5, 0, RANDOM.nextDouble() * 10 - 5);
    }
}
