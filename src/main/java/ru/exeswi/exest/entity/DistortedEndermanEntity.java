package ru.exeswi.exest.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;
import ru.exeswi.exest.util.HorrorUtil;

/**
 * Something wearing an enderman's silhouette, badly. It circles the player in slow
 * teleport hops, muttering backwards. Unlike the real thing it tolerates eye contact —
 * for about a second. Then it screams and comes.
 */
public class DistortedEndermanEntity extends AbstractHorrorEntity {

    private int hopCooldown;

    public DistortedEndermanEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        setMaxLife(20 * 60 * 4);
    }

    public static DefaultAttributeContainer.Builder createDistortedEndermanAttributes() {
        return createBaseHorrorAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.34)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 1.3, true));

        targetSelector.add(1, new RevengeGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient || isApparition()) {
            return;
        }
        ServerPlayerEntity player = closestSurvivalPlayer(48.0);
        if (player == null) {
            return;
        }
        // eye contact is an invitation
        if (getTarget() == null && getStareTicks() > 25) {
            setTarget(player);
            getWorld().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 1.2f, 0.5f);
        }
        // while passive it circles the player in teleport hops
        if (getTarget() == null && --hopCooldown <= 0) {
            hopCooldown = 100 + random.nextInt(140);
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 8.0 + random.nextDouble() * 8.0;
            BlockPos spot = HorrorUtil.findStandablePos((ServerWorld) getWorld(),
                    (int) (player.getX() + Math.cos(angle) * dist),
                    player.getBlockPos().getY(),
                    (int) (player.getZ() + Math.sin(angle) * dist));
            if (spot != null && teleportToSpot(spot)) {
                getWorld().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.7f, 0.4f);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_ENDERMAN_AMBIENT;
    }

    @Override
    public float getSoundPitch() {
        // as close to "played backwards" as a pitch shift gets
        return 0.25f + random.nextFloat() * 0.15f;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return SoundEvents.ENTITY_ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ENDERMAN_DEATH;
    }
}
