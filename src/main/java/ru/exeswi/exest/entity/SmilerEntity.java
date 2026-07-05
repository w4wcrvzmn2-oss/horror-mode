package ru.exeswi.exest.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import ru.exeswi.exest.ai.goal.CreepTowardsGoal;
import ru.exeswi.exest.ai.goal.FreezeWhenSeenGoal;
import ru.exeswi.exest.ai.goal.SplitTargetGoal;
import ru.exeswi.exest.ai.goal.VanishWhenStaredGoal;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

/**
 * The Smiler: perfectly motionless while you watch it, horrifyingly fast the moment
 * you don't. Its giggle is high-pitched and wrong.
 */
public class SmilerEntity extends AbstractHorrorEntity {

    public SmilerEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        setMaxLife(20 * 60 * 5);
    }

    public static DefaultAttributeContainer.Builder createSmilerAttributes() {
        return createBaseHorrorAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 45.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.36)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new FreezeWhenSeenGoal(this));
        goalSelector.add(2, new VanishWhenStaredGoal(this, 500, 0.8f));
        goalSelector.add(3, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.add(4, new ru.exeswi.exest.ai.goal.BreakWeakBlocksGoal(this));
        goalSelector.add(5, new CreepTowardsGoal(this, 1.2, 1.5, 0.15f));

        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new SplitTargetGoal(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(5) == 0 ? SoundEvents.ENTITY_VILLAGER_AMBIENT : null;
    }

    @Override
    public float getSoundPitch() {
        // the giggle sits far above a normal voice
        return 1.7f + random.nextFloat() * 0.4f;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return SoundEvents.ENTITY_VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VILLAGER_DEATH;
    }
}
