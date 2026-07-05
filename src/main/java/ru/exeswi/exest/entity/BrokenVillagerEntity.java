package ru.exeswi.exest.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import ru.exeswi.exest.ai.goal.OpenDoorsGoal;
import ru.exeswi.exest.ai.goal.SplitTargetGoal;
import ru.exeswi.exest.ai.goal.StalkPlayerGoal;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

/**
 * A villager that came back wrong. It wanders abandoned houses, twitches, snaps its
 * head at impossible angles and speaks too slowly. Gets violent at arm's length.
 */
public class BrokenVillagerEntity extends AbstractHorrorEntity {

    public BrokenVillagerEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createBrokenVillagerAttributes() {
        return createBaseHorrorAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 35.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new OpenDoorsGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.add(3, new StalkPlayerGoal(this, 6.0, 14.0, 0.8));
        goalSelector.add(4, new WanderAroundFarGoal(this, 0.6));

        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new SplitTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient || isApparition()) {
            return;
        }
        // the twitch: head and body snap to random angles for a few ticks
        if (random.nextInt(60) == 0) {
            float jerk = (random.nextFloat() - 0.5f) * 160.0f;
            setHeadYaw(getHeadYaw() + jerk);
            setBodyYaw(getBodyYaw() + jerk * 0.3f);
        }
    }

    @Override
    protected boolean dodgesPlayerAttacks() {
        return false; // still flesh and bone, whatever came back in it
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_VILLAGER_AMBIENT;
    }

    @Override
    public float getSoundPitch() {
        return 0.35f + random.nextFloat() * 0.25f;
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
