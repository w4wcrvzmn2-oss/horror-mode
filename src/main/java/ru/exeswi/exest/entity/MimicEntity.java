package ru.exeswi.exest.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import ru.exeswi.exest.ai.goal.StalkPlayerGoal;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

import java.util.List;

/**
 * It looks like a player. It has a name like a player. On a server it borrows the name
 * of someone who is actually online. It keeps its distance and runs when approached —
 * but on a dark night, if you are alone, it stops running.
 */
public class MimicEntity extends AbstractHorrorEntity {

    private static final String[] FALLBACK_NAMES = {
            "Steve", "Alex02", "Miner_Dan", "xX_Hunter_Xx", "Sofia", "Craft3r", "Nick"
    };

    private boolean named;

    public MimicEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        setMaxLife(20 * 60 * 4);
    }

    public static DefaultAttributeContainer.Builder createMimicAttributes() {
        return createBaseHorrorAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.36)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, 10.0f, 1.1, 1.3,
                player -> getTarget() == null));
        goalSelector.add(3, new StalkPlayerGoal(this, 18.0, 34.0, 0.9));

        targetSelector.add(1, new RevengeGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) {
            return;
        }
        if (!named) {
            named = true;
            setCustomName(Text.literal(pickName()));
            setCustomNameVisible(true);
        }
        if (isApparition()) {
            return;
        }
        // alone, at night, in the dark — that's when it stops pretending
        if (getTarget() == null && age % 40 == 0 && getWorld().isNight()) {
            ServerPlayerEntity victim = closestSurvivalPlayer(16.0);
            if (victim != null
                    && getWorld().getLightLevel(victim.getBlockPos()) < 5
                    && ru.exeswi.exest.ai.PackCoordinator.huntersOf(victim) == 0
                    && random.nextInt(6) == 0) {
                setTarget(victim);
            }
        }
    }

    private String pickName() {
        MinecraftServer server = getServer();
        if (server != null) {
            List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
            if (players.size() > 1 || (players.size() == 1 && random.nextBoolean())) {
                // wearing the name of someone who is really here is far more disturbing
                return players.get(random.nextInt(players.size())).getGameProfile().getName();
            }
        }
        return FALLBACK_NAMES[random.nextInt(FALLBACK_NAMES.length)];
    }

    @Override
    protected net.minecraft.sound.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected net.minecraft.sound.SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return net.minecraft.sound.SoundEvents.ENTITY_PLAYER_HURT;
    }

    @Override
    protected net.minecraft.sound.SoundEvent getDeathSound() {
        return net.minecraft.sound.SoundEvents.ENTITY_PLAYER_DEATH;
    }
}
