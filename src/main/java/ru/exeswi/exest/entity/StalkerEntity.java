package ru.exeswi.exest.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import ru.exeswi.exest.ai.goal.AmbushTeleportGoal;
import ru.exeswi.exest.ai.goal.BreakWeakBlocksGoal;
import ru.exeswi.exest.ai.goal.CreepTowardsGoal;
import ru.exeswi.exest.ai.goal.FlankTargetGoal;
import ru.exeswi.exest.ai.goal.OpenDoorsGoal;
import ru.exeswi.exest.ai.goal.OpportunisticAttackGoal;
import ru.exeswi.exest.ai.goal.RetreatFeintGoal;
import ru.exeswi.exest.ai.goal.SplitTargetGoal;
import ru.exeswi.exest.ai.goal.StalkPlayerGoal;
import ru.exeswi.exest.ai.goal.VanishWhenStaredGoal;
import ru.exeswi.exest.ai.goal.WaitInDarknessGoal;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

/**
 * The flagship monster. It watches from a distance, vanishes when stared at, relocates
 * behind the player's back, sometimes just stands in silence, sometimes creeps closer,
 * rarely sprints, opens doors, smashes weak blocks and only commits to a kill when the
 * odds are on its side. Its entire schedule is randomized per individual.
 */
public class StalkerEntity extends AbstractHorrorEntity {

    public StalkerEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        if (getNavigation() instanceof MobNavigation nav) {
            nav.setCanPathThroughDoors(true);
        }
    }

    public static DefaultAttributeContainer.Builder createStalkerAttributes() {
        return createBaseHorrorAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new VanishWhenStaredGoal(this, 70, 0.65f));
        goalSelector.add(2, new OpenDoorsGoal(this));
        goalSelector.add(2, new OpportunisticAttackGoal(this, 1.25));
        goalSelector.add(3, new RetreatFeintGoal(this));
        goalSelector.add(4, new FlankTargetGoal(this, 1.15));
        goalSelector.add(5, new BreakWeakBlocksGoal(this));
        goalSelector.add(6, new AmbushTeleportGoal(this, 500));
        goalSelector.add(7, new CreepTowardsGoal(this, 0.7, 1.45, 0.04f));
        goalSelector.add(8, new StalkPlayerGoal(this, 16.0, 34.0, 0.9));
        goalSelector.add(9, new WaitInDarknessGoal(this));

        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new SplitTargetGoal(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        // silent by design; its presence is announced by the world, not by itself
        return null;
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
