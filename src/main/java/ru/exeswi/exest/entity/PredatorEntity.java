package ru.exeswi.exest.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import ru.exeswi.exest.ai.goal.OpportunisticAttackGoal;
import ru.exeswi.exest.ai.goal.SplitTargetGoal;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

/**
 * The Unseen: a predator with no visible body. Its presence is a wet sniffing sound,
 * dust kicked off the ground, grass bending under nothing. It strikes once, hard,
 * and is somewhere else before the pain registers.
 */
public class PredatorEntity extends AbstractHorrorEntity {

    public PredatorEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        setMaxLife(20 * 60 * 4);
    }

    public static DefaultAttributeContainer.Builder createPredatorAttributes() {
        return createBaseHorrorAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 35.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.38)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new OpportunisticAttackGoal(this, 1.35));

        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new SplitTargetGoal(this));
    }

    @Override
    public boolean isInvisible() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient || isApparition() || !(getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        // the only traces it leaves: disturbed dust and breathing you can almost feel
        if (age % 8 == 0 && getVelocity().horizontalLengthSquared() > 0.003) {
            serverWorld.spawnParticles(ParticleTypes.POOF,
                    getX(), getY() + 0.05, getZ(), 2, 0.15, 0.02, 0.15, 0.01);
        }
        if (age % 90 == 0 && closestSurvivalPlayer(12.0) != null) {
            getWorld().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.ENTITY_WARDEN_SNIFF, SoundCategory.HOSTILE, 0.8f, 0.6f);
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit) {
            // hit-and-run: one strike, then gone
            teleportOutsideView(10.0, 20.0);
            setTarget(null);
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return SoundEvents.ENTITY_WARDEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WARDEN_DEATH;
    }
}
