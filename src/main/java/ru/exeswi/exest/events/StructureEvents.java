package ru.exeswi.exest.events;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import ru.exeswi.exest.sanity.SanityManager;
import ru.exeswi.exest.util.HorrorUtil;
import ru.exeswi.exest.world.HorrorWorldState;

import static ru.exeswi.exest.events.HorrorEvent.Category.RARE;

/**
 * Places that should not exist appear quietly in the world ahead of the player:
 * an abandoned hut frozen mid-struggle, and a forest altar that a trail of signs
 * politely invites you to visit. Curiosity does the rest — standing at the altar
 * has consequences (see the proximity check in the main tick loop).
 */
public final class StructureEvents {

    private static final String[] TRAIL_SIGNS = {"ИДИ", "БЛИЖЕ", "ОНО ЖДЁТ"};

    private StructureEvents() {
    }

    public static void registerAll() {
        HorrorEvent.builder("abandoned_hut", RARE).weight(5).cooldown(48000)
                .condition(p -> p.getServerWorld().isSkyVisible(p.getBlockPos()))
                .action(StructureEvents::placeHut).register();

        HorrorEvent.builder("forest_altar", RARE).weight(5).cooldown(48000)
                .condition(p -> p.getServerWorld().isSkyVisible(p.getBlockPos()))
                .action(StructureEvents::placeAltar).register();
    }

    // --- the hut: somebody lived here, briefly ---

    private static void placeHut(ServerPlayerEntity player, HorrorEventManager manager) {
        ServerWorld world = player.getServerWorld();
        BlockPos center = HorrorUtil.findGroundSpot(world, player, 50.0, 80.0, true, world.random);
        if (center == null) {
            return;
        }
        Random random = world.random;
        var floor = Blocks.OAK_PLANKS.getDefaultState();
        var wall = Blocks.SPRUCE_PLANKS.getDefaultState();
        var air = Blocks.AIR.getDefaultState();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlockState(center.add(dx, -1, dz), floor);
                for (int dy = 0; dy <= 2; dy++) {
                    boolean isWall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    // holes in the walls: something left in a hurry, through them
                    boolean ruined = isWall && random.nextInt(6) == 0;
                    world.setBlockState(center.add(dx, dy, dz), isWall && !ruined ? wall : air);
                }
                world.setBlockState(center.add(dx, 3, dz), Blocks.OAK_SLAB.getDefaultState());
            }
        }
        // a doorway with the door torn open
        world.setBlockState(center.add(2, 0, 0), air);
        world.setBlockState(center.add(2, 1, 0), air);
        // signs of the struggle
        world.setBlockState(center.add(-1, 0, 1), Blocks.COBWEB.getDefaultState());
        world.setBlockState(center.add(1, 1, -1), Blocks.COBWEB.getDefaultState());
        world.setBlockState(center.add(-1, -1, -1), Blocks.NETHER_WART_BLOCK.getDefaultState());
        // a chest with what they could not take along
        world.setBlockState(center.add(-1, 0, -1), Blocks.CHEST.getDefaultState());
        if (world.getBlockEntity(center.add(-1, 0, -1)) instanceof ChestBlockEntity chest) {
            chest.setStack(random.nextInt(chest.size()), new ItemStack(Items.BREAD, 2 + random.nextInt(3)));
            chest.setStack(random.nextInt(chest.size()), new ItemStack(Items.TORCH, 1 + random.nextInt(4)));
            chest.setStack(random.nextInt(chest.size()), new ItemStack(Items.PAPER, 1));
            chest.markDirty();
        }
        placeSign(world, center.add(0, 0, 0),
                world.random.nextBoolean() ? "ОНИ ПРИШЛИ НОЧЬЮ" : "БЕНТОН, БЕГИ");
    }

    // --- the altar: the signs lead you there themselves ---

    private static void placeAltar(ServerPlayerEntity player, HorrorEventManager manager) {
        ServerWorld world = player.getServerWorld();
        BlockPos altar = HorrorUtil.findGroundSpot(world, player, 40.0, 70.0, true, world.random);
        if (altar == null) {
            return;
        }
        // the pedestal and the skull
        world.setBlockState(altar, Blocks.MOSSY_COBBLESTONE.getDefaultState());
        world.setBlockState(altar.up(), Blocks.MOSSY_COBBLESTONE_WALL.getDefaultState());
        world.setBlockState(altar.up(2), Blocks.SKELETON_SKULL.getDefaultState()
                .with(Properties.ROTATION, world.random.nextInt(16)));
        // a ring of dead ground, soul torches and wither roses
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 3.5 || (dx == 0 && dz == 0)) {
                    continue;
                }
                BlockPos ground = HorrorUtil.findStandablePos(world,
                        altar.getX() + dx, altar.getY(), altar.getZ() + dz);
                if (ground == null) {
                    continue;
                }
                world.setBlockState(ground.down(), Blocks.COARSE_DIRT.getDefaultState());
                if (dist > 2.5 && (dx + dz) % 3 == 0) {
                    world.setBlockState(ground, Blocks.SOUL_TORCH.getDefaultState());
                } else if (world.random.nextInt(5) == 0
                        && Blocks.WITHER_ROSE.getDefaultState().canPlaceAt(world, ground)) {
                    world.setBlockState(ground, Blocks.WITHER_ROSE.getDefaultState());
                }
            }
        }
        // the trail: three signs between the player and the altar
        Vec3d from = player.getPos();
        Vec3d to = Vec3d.ofCenter(altar);
        for (int i = 0; i < TRAIL_SIGNS.length; i++) {
            double t = 0.25 + i * 0.25;
            Vec3d point = from.lerp(to, t);
            BlockPos ground = HorrorUtil.findStandablePos(world,
                    (int) point.x, (int) point.y, (int) point.z);
            if (ground != null && world.getBlockState(ground).isAir()) {
                placeSign(world, ground, TRAIL_SIGNS[i]);
            }
        }
        // remember it: standing next to the altar later triggers the consequences
        HorrorWorldState.get(world).addAltar(altar.up());
        SanityManager.modify(player, -2.0f);
    }

    private static void placeSign(ServerWorld world, BlockPos pos, String line) {
        if (!world.getBlockState(pos).isAir()) {
            return;
        }
        world.setBlockState(pos, Blocks.OAK_SIGN.getDefaultState()
                .with(Properties.ROTATION, world.random.nextInt(16)));
        if (world.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            sign.setText(sign.getText(true).withMessage(1, Text.literal(line)), true);
            sign.markDirty();
        }
    }

    /**
     * Altar proximity check, called once a second per player from the main loop.
     * The first visitor gets the full welcome: whispers, sanity loss and a ring of
     * figures around the clearing that were not there a second ago.
     */
    public static void checkAltars(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        HorrorWorldState state = HorrorWorldState.get(world);
        BlockPos found = null;
        for (BlockPos altar : state.getAltars()) {
            if (altar.isWithinDistance(player.getBlockPos(), 6.0)) {
                found = altar;
                break;
            }
        }
        if (found == null) {
            return;
        }
        state.removeAltar(found);
        ru.exeswi.exest.networking.HorrorNetworking.sendCueBehind(player,
                ru.exeswi.exest.networking.SoundCue.WHISPER, 1.0f);
        ru.exeswi.exest.networking.HorrorNetworking.sendEffect(player,
                ru.exeswi.exest.networking.HorrorEffect.HALLUCINATION_OVERLAY, 1.0f, 120);
        SanityManager.modify(player, -10.0f);
        for (int i = 0; i < 3; i++) {
            ApparitionSpawner.spawnApparition(player, ApparitionSpawner.Placement.DISTANCE);
        }
    }

}
