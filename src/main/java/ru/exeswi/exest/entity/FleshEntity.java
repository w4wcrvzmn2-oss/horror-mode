package ru.exeswi.exest.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import ru.exeswi.exest.ai.goal.BreakWeakBlocksGoal;
import ru.exeswi.exest.ai.goal.SplitTargetGoal;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

/**
 * A wet amalgam of too many bodies. Slow, nearly unstoppable, walks straight through
 * weak barriers, and every landed blow ends in a roar that hurls everyone nearby off
 * their feet. You hear it squelching long before you see it.
 */
public class FleshEntity extends AbstractHorrorEntity {

    public FleshEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createFleshAttributes() {
        return createBaseHorrorAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 120.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.add(2, new BreakWeakBlocksGoal(this));

        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new SplitTargetGoal(this));
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit) {
            // the impact roar throws everything around it backwards
            getWorld().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.2f, 0.6f);
            for (LivingEntity nearby : getWorld().getEntitiesByClass(LivingEntity.class,
                    Box.of(getPos(), 7, 4, 7), e -> e != this && !(e instanceof AbstractHorrorEntity))) {
                double dx = nearby.getX() - getX();
                double dz = nearby.getZ() - getZ();
                nearby.takeKnockback(1.2, -dx, -dz);
            }
        }
        return hit;
    }

    @Override
    protected boolean dodgesPlayerAttacks() {
        return false; // it is very, very physical
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        // the one creature you are supposed to hear coming
        playSound(SoundEvents.ENTITY_SLIME_SQUISH, 0.7f, 0.5f + random.nextFloat() * 0.2f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(3) == 0 ? SoundEvents.ENTITY_RAVAGER_AMBIENT : SoundEvents.ENTITY_SLIME_SQUISH;
    }

    @Override
    public float getSoundPitch() {
        return 0.4f + random.nextFloat() * 0.2f;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return SoundEvents.ENTITY_RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_RAVAGER_DEATH;
    }
}
