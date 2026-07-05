package ru.exeswi.exest.events;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import ru.exeswi.exest.events.ApparitionSpawner.Placement;
import ru.exeswi.exest.networking.FakeUi;
import ru.exeswi.exest.networking.HorrorEffect;
import ru.exeswi.exest.sanity.SanityManager;
import ru.exeswi.exest.util.HorrorUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.exeswi.exest.events.HorrorEvent.Category.RARE;

/**
 * Extremely rare, deeply wrong moments: the inventory is not how you left it, the
 * torches are out, the game "crashes", you are standing somewhere else.
 */
public final class RareEvents {

    private RareEvents() {
    }

    public static void registerAll() {
        HorrorEvent.builder("screen_flash", RARE).weight(8).cooldown(6000)
                .enabledWhen(c -> c.enableJumpscares && c.enableVisualEffects)
                .action((p, m) -> {
                    m.effect(p, HorrorEffect.FLASH, 1.0f, 5);
                    ApparitionSpawner.spawnApparition(p, Placement.ONE_FRAME);
                    SanityManager.modify(p, -6.0f);
                }).register();

        HorrorEvent.builder("glitch_burst", RARE).weight(10).cooldown(4000)
                .enabledWhen(c -> c.enableVisualEffects)
                .action((p, m) -> m.effect(p, HorrorEffect.GLITCH, 0.8f, 40)).register();

        HorrorEvent.builder("static_burst", RARE).weight(10).cooldown(4000)
                .enabledWhen(c -> c.enableVisualEffects)
                .action((p, m) -> m.effect(p, HorrorEffect.STATIC, 0.7f, 30)).register();

        HorrorEvent.builder("blink", RARE).weight(8).cooldown(4800)
                .enabledWhen(c -> c.enableVisualEffects)
                .action((p, m) -> m.effect(p, HorrorEffect.BLINK, 1.0f, 30)).register();

        HorrorEvent.builder("hallucination_overlay", RARE).weight(6).cooldown(6000)
                .enabledWhen(c -> c.enableHallucinations && c.enableVisualEffects)
                .condition(p -> SanityManager.get(p) < 70.0f)
                .action((p, m) -> m.effect(p, HorrorEffect.HALLUCINATION_OVERLAY, 0.8f, 100)).register();

        HorrorEvent.builder("fake_ore", RARE).weight(8).cooldown(4800)
                .enabledWhen(c -> c.enableHallucinations)
                .condition(p -> SanityManager.get(p) < 70.0f)
                .action((p, m) -> m.effect(p, HorrorEffect.FAKE_ORE, 1.0f, 400)).register();

        HorrorEvent.builder("inventory_shuffle", RARE).weight(5).cooldown(20000)
                .action(RareEvents::shuffleInventory).register();

        HorrorEvent.builder("chest_tamper", RARE).weight(5).cooldown(20000)
                .condition(p -> findChest(p) != null)
                .action(RareEvents::shuffleChest).register();

        HorrorEvent.builder("torch_extinguish", RARE).weight(7).cooldown(9600)
                .enabledWhen(c -> c.enableDarkness)
                .condition(p -> hasTorchNearby(p))
                .action(RareEvents::extinguishTorches).register();

        HorrorEvent.builder("silent_teleport", RARE).weight(5).cooldown(16000)
                .action(RareEvents::teleportPlayer).register();

        HorrorEvent.builder("fake_crash", RARE).weight(3).cooldown(48000)
                .enabledWhen(c -> c.enableJumpscares)
                .action((p, m) -> {
                    m.fakeUi(p, FakeUi.CRASH);
                    SanityManager.modify(p, -8.0f);
                }).register();

        HorrorEvent.builder("fake_loading", RARE).weight(4).cooldown(36000)
                .enabledWhen(c -> c.enableJumpscares)
                .action((p, m) -> m.fakeUi(p, FakeUi.LOADING)).register();

        HorrorEvent.builder("fake_title", RARE).weight(3).cooldown(48000)
                .enabledWhen(c -> c.enableJumpscares)
                .action((p, m) -> {
                    m.fakeUi(p, FakeUi.TITLE);
                    SanityManager.modify(p, -6.0f);
                }).register();

        // weight 0: never rolled randomly — fired on join/respawn by AbductionSequence
        // itself, or manually via /horror event abduction
        HorrorEvent.builder("abduction", RARE).weight(0).cooldown(24000)
                .enabledWhen(c -> c.enableJumpscares)
                .condition(p -> p.getServerWorld().isSkyVisible(p.getBlockPos()))
                .action((p, m) -> AbductionSequence.start(p)).register();

        // it came to your home while you were away: torches out, door open, trampled
        // ground — and a sign it left for you. Completely silent; you find it later.
        HorrorEvent.builder("intrusion_traces", RARE).weight(6).cooldown(36000)
                .condition(p -> findDoorNear(p) != null)
                .action(RareEvents::leaveTraces).register();

        // weight 0: the final hunt normally starts itself at horror level 10;
        // this is the manual trigger for testing
        HorrorEvent.builder("final_hunt", RARE).weight(0).cooldown(72000)
                .enabledWhen(c -> c.enableMonsters)
                .action((p, m) -> FinalHunt.begin(p.getServerWorld())).register();
    }

    private static BlockPos findDoorNear(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        for (BlockPos pos : BlockPos.iterateOutwards(player.getBlockPos(), 16, 6, 16)) {
            BlockState state = world.getBlockState(pos);
            if (state.isIn(net.minecraft.registry.tag.BlockTags.WOODEN_DOORS)
                    && state.getBlock() instanceof net.minecraft.block.DoorBlock
                    && state.get(net.minecraft.block.DoorBlock.HALF) == net.minecraft.block.enums.DoubleBlockHalf.LOWER) {
                return pos.toImmutable();
            }
        }
        return null;
    }

    private static void leaveTraces(ServerPlayerEntity player, HorrorEventManager manager) {
        ServerWorld world = player.getServerWorld();
        BlockPos door = findDoorNear(player);
        if (door == null) {
            return;
        }
        // the door is left open
        BlockState doorState = world.getBlockState(door);
        if (doorState.getBlock() instanceof net.minecraft.block.DoorBlock doorBlock
                && !doorState.get(net.minecraft.block.DoorBlock.OPEN)) {
            doorBlock.setOpen(null, world, doorState, door, true);
        }
        // torches around go out, silently
        int snuffed = 0;
        for (BlockPos pos : BlockPos.iterateOutwards(door, 10, 5, 10)) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(Blocks.TORCH) || state.isOf(Blocks.WALL_TORCH)) {
                world.removeBlock(pos, false);
                world.spawnParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.1, 0.2, 0.1, 0.01);
                if (++snuffed >= 3) {
                    break;
                }
            }
        }
        // trampled ground in front of the house
        for (int i = 0; i < 6; i++) {
            BlockPos spot = door.add(world.random.nextInt(13) - 6, 0, world.random.nextInt(13) - 6);
            BlockPos ground = HorrorUtil.findStandablePos(world, spot.getX(), spot.getY(), spot.getZ());
            if (ground != null && world.getBlockState(ground.down()).isOf(Blocks.GRASS_BLOCK)) {
                world.setBlockState(ground.down(), Blocks.COARSE_DIRT.getDefaultState());
            }
        }
        placeSign(world, door);
        SanityManager.modify(player, -8.0f);
    }

    private static void placeSign(ServerWorld world, BlockPos door) {
        String[] phrases = {"I WAS HERE", "I SEE YOU", "YOU SLEEP LOUD", "DON'T HIDE", "COME OUTSIDE"};
        for (int i = 0; i < 12; i++) {
            BlockPos spot = door.add(world.random.nextInt(7) - 3, 0, world.random.nextInt(7) - 3);
            BlockPos ground = HorrorUtil.findStandablePos(world, spot.getX(), spot.getY(), spot.getZ());
            if (ground == null || !world.getBlockState(ground).isAir()) {
                continue;
            }
            world.setBlockState(ground, Blocks.OAK_SIGN.getDefaultState()
                    .with(net.minecraft.state.property.Properties.ROTATION, world.random.nextInt(16)));
            if (world.getBlockEntity(ground) instanceof net.minecraft.block.entity.SignBlockEntity sign) {
                String phrase = phrases[world.random.nextInt(phrases.length)];
                sign.setText(sign.getText(true).withMessage(1,
                        net.minecraft.text.Text.literal(phrase)), true);
                sign.markDirty();
            }
            return;
        }
    }

    private static void shuffleInventory(ServerPlayerEntity player, HorrorEventManager manager) {
        PlayerInventory inventory = player.getInventory();
        int swaps = 2 + player.getRandom().nextInt(3);
        for (int i = 0; i < swaps; i++) {
            int a = player.getRandom().nextInt(36);
            int b = player.getRandom().nextInt(36);
            ItemStack tmp = inventory.getStack(a);
            inventory.setStack(a, inventory.getStack(b));
            inventory.setStack(b, tmp);
        }
        player.playerScreenHandler.sendContentUpdates();
        SanityManager.modify(player, -4.0f);
    }

    private static BlockPos findChest(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        for (BlockPos pos : BlockPos.iterateOutwards(player.getBlockPos(), 12, 5, 12)) {
            if (world.getBlockEntity(pos) instanceof LootableContainerBlockEntity) {
                return pos.toImmutable();
            }
        }
        return null;
    }

    private static void shuffleChest(ServerPlayerEntity player, HorrorEventManager manager) {
        BlockPos pos = findChest(player);
        if (pos == null
                || !(player.getServerWorld().getBlockEntity(pos) instanceof LootableContainerBlockEntity chest)) {
            return;
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < chest.size(); i++) {
            stacks.add(chest.getStack(i));
        }
        Collections.shuffle(stacks, new java.util.Random(player.getRandom().nextLong()));
        for (int i = 0; i < chest.size(); i++) {
            chest.setStack(i, stacks.get(i));
        }
        chest.markDirty();
    }

    private static boolean hasTorchNearby(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        for (BlockPos pos : BlockPos.iterateOutwards(player.getBlockPos(), 10, 5, 10)) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(Blocks.TORCH) || state.isOf(Blocks.WALL_TORCH)) {
                return true;
            }
        }
        return false;
    }

    private static void extinguishTorches(ServerPlayerEntity player, HorrorEventManager manager) {
        ServerWorld world = player.getServerWorld();
        int removed = 0;
        for (BlockPos pos : BlockPos.iterateOutwards(player.getBlockPos(), 10, 5, 10)) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(Blocks.TORCH) || state.isOf(Blocks.WALL_TORCH)) {
                world.removeBlock(pos, false);
                world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH,
                        SoundCategory.BLOCKS, 0.6f, 0.8f);
                world.spawnParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.1, 0.2, 0.1, 0.01);
                if (++removed >= 6) {
                    break;
                }
            }
        }
        if (removed > 0) {
            SanityManager.modify(player, -3.0f * removed);
        }
    }

    private static void teleportPlayer(ServerPlayerEntity player, HorrorEventManager manager) {
        ServerWorld world = player.getServerWorld();
        BlockPos spot = HorrorUtil.findGroundSpot(world, player, 3.0, 7.0, false, world.random);
        if (spot == null) {
            return;
        }
        player.teleport(world, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
                player.getYaw(), player.getPitch());
        manager.effect(player, HorrorEffect.STATIC, 0.5f, 8);
        SanityManager.modify(player, -5.0f);
    }
}
