package ru.exeswi.exest.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import ru.exeswi.exest.ai.goal.FreezeWhenSeenGoal;
import ru.exeswi.exest.ai.goal.SplitTargetGoal;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;
import ru.exeswi.exest.util.HorrorUtil;

/**
 * A low, skittering thing that hugs the floor. It freezes solid under direct
 * observation and only ever strikes a back that is turned to it.
 */
public class CrawlerEntity extends AbstractHorrorEntity {

    public CrawlerEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        setMaxLife(20 * 60 * 5);
    }

    public static DefaultAttributeContainer.Builder createCrawlerAttributes() {
        return createBaseHorrorAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 25.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new FreezeWhenSeenGoal(this));
        goalSelector.add(2, new BackstabAttackGoal(this, 1.35));
        goalSelector.add(3, new ru.exeswi.exest.ai.goal.BreakWeakBlocksGoal(this));

        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new SplitTargetGoal(this));
    }

    @Override
    protected boolean dodgesPlayerAttacks() {
        return false; // fast, but killable — if you can catch it looking away
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(3) == 0 ? SoundEvents.ENTITY_SPIDER_AMBIENT : null;
    }

    @Override
    public float getSoundPitch() {
        return 0.6f + random.nextFloat() * 0.2f;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return SoundEvents.ENTITY_SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_SPIDER_DEATH;
    }

    /** Melee that pauses whenever the victim is actually facing the crawler. */
    private static class BackstabAttackGoal extends MeleeAttackGoal {

        private final CrawlerEntity crawler;

        BackstabAttackGoal(CrawlerEntity crawler, double speed) {
            super(crawler, speed, true);
            this.crawler = crawler;
        }

        @Override
        public boolean canStart() {
            return super.canStart() && targetNotFacingUs();
        }

        @Override
        public boolean shouldContinue() {
            return super.shouldContinue() && targetNotFacingUs();
        }

        private boolean targetNotFacingUs() {
            LivingEntity target = crawler.getTarget();
            if (target instanceof PlayerEntity player) {
                return !HorrorUtil.isInViewCone(player, crawler.getBoundingBox().getCenter());
            }
            return true;
        }
    }
}
