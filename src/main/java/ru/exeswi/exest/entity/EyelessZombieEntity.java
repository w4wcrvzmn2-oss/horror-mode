package ru.exeswi.exest.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

/**
 * An eyeless corpse that hunts purely by ear — it acquires targets through walls, but
 * only players who make noise. Stand perfectly still while sneaking and it forgets
 * you exist.
 */
public class EyelessZombieEntity extends AbstractHorrorEntity {

    public EyelessZombieEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createEyelessZombieAttributes() {
        return createBaseHorrorAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.add(2, new ru.exeswi.exest.ai.goal.BreakWeakBlocksGoal(this));
        goalSelector.add(3, new WanderAroundFarGoal(this, 0.7));

        targetSelector.add(1, new RevengeGoal(this));
        // no visibility check: it does not see, it listens
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, false, false,
                entity -> !entity.isSneaking()
                        && entity.getVelocity().horizontalLengthSquared() > 0.004));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient || isApparition()) {
            return;
        }
        // a victim that goes quiet fades out of its perception
        if (getTarget() instanceof PlayerEntity player && player.isSneaking()
                && player.getVelocity().horizontalLengthSquared() < 0.002
                && random.nextInt(40) == 0) {
            setTarget(null);
        }
    }

    @Override
    protected boolean dodgesPlayerAttacks() {
        return false; // a corpse is a corpse: it can be put down
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return SoundEvents.ENTITY_ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ZOMBIE_DEATH;
    }
}
