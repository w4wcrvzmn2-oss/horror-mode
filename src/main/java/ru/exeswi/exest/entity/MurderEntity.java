package ru.exeswi.exest.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import ru.exeswi.exest.ai.goal.BreakWeakBlocksGoal;
import ru.exeswi.exest.ai.goal.OpenDoorsGoal;
import ru.exeswi.exest.ai.goal.SplitTargetGoal;
import ru.exeswi.exest.ai.goal.StalkPlayerGoal;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

/**
 * Murder. He came here looking for Benton — and found what Benton had become.
 * Whatever he was before that day, this is what is left: a man-shaped thing that
 * smells blood. He does not stalk, does not hesitate and does not vanish; you hear
 * his footsteps, ordinary and human, which is somehow worse. The wounded interest
 * him most of all — he exists to finish what something else started.
 */
public class MurderEntity extends AbstractHorrorEntity {

    public MurderEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createMurderAttributes() {
        return createBaseHorrorAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 80.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.33)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new OpenDoorsGoal(this));
        // no opportunism gate: Murder does not wait for an advantage, he makes one
        goalSelector.add(2, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.add(3, new BreakWeakBlocksGoal(this));
        goalSelector.add(4, new StalkPlayerGoal(this, 8.0, 18.0, 0.95));

        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new SplitTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient || isApparition()) {
            return;
        }
        // he smells blood: any wounded player within thirty blocks becomes the job
        if (getTarget() == null && age % 20 == 0) {
            ServerPlayerEntity wounded = closestSurvivalPlayer(30.0);
            if (wounded != null && wounded.getHealth() < wounded.getMaxHealth() * 0.66f) {
                setTarget(wounded);
                playSound(SoundEvents.ENTITY_PLAYER_BREATH, 1.0f, 0.5f);
            }
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof LivingEntity living
                && living.getHealth() < living.getMaxHealth() * 0.5f) {
            // the finisher: the weaker you are, the harder he swings
            living.damage(getWorld().getDamageSources().mobAttack(this), 4.0f);
        }
        return hit;
    }

    @Override
    protected boolean dodgesPlayerAttacks() {
        return false; // a man, still. Mostly.
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        // ordinary human footsteps — the one sound in this mod that is not distorted
        playSound(state.getSoundGroup().getStepSound(), 0.4f, 1.0f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public float getSoundPitch() {
        return 0.8f;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return SoundEvents.ENTITY_PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PLAYER_DEATH;
    }
}
