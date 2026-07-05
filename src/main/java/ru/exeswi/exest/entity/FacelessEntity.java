package ru.exeswi.exest.entity;

import net.minecraft.entity.EntityPose;
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
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import ru.exeswi.exest.ai.goal.StalkPlayerGoal;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

/**
 * A player-shaped figure with no face. It stands at a distance and mirrors you:
 * crouches when you crouch, swings when you swing. Study it for too long and it
 * decides you have seen enough.
 */
public class FacelessEntity extends AbstractHorrorEntity {

    public FacelessEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        setMaxLife(20 * 60 * 4);
    }

    public static DefaultAttributeContainer.Builder createFacelessAttributes() {
        return createBaseHorrorAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.add(2, new StalkPlayerGoal(this, 12.0, 26.0, 0.9));

        targetSelector.add(1, new RevengeGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient || isApparition()) {
            return;
        }
        ServerPlayerEntity mirrored = closestSurvivalPlayer(40.0);
        if (mirrored == null) {
            return;
        }
        // the mirror: it copies your body language with a slight, wrong delay
        if (getTarget() == null) {
            setPose(mirrored.isInSneakingPose() ? EntityPose.CROUCHING : EntityPose.STANDING);
            if (mirrored.handSwinging && !handSwinging && random.nextInt(3) == 0) {
                swingHand(Hand.MAIN_HAND);
            }
        }
        // being studied for five seconds flips the switch
        if (getTarget() == null && getStareTicks() > 100) {
            setTarget(mirrored);
            getWorld().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.ENTITY_ENDERMAN_SCREAM, net.minecraft.sound.SoundCategory.HOSTILE, 1.0f, 0.4f);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
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
