package ru.exeswi.exest.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import ru.exeswi.exest.ai.goal.CreepTowardsGoal;
import ru.exeswi.exest.ai.goal.StalkPlayerGoal;
import ru.exeswi.exest.ai.goal.VanishWhenStaredGoal;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;
import ru.exeswi.exest.sanity.SanityManager;

/**
 * The Tall One: a towering shadow that exists only in darkness. It never swings a fist;
 * instead its mere proximity smothers light, slows the body and shreds sanity. Bright
 * light destroys it.
 */
public class ShadowEntity extends AbstractHorrorEntity {

    public ShadowEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        setMaxLife(20 * 60 * 4);
    }

    public static DefaultAttributeContainer.Builder createShadowAttributes() {
        return createBaseHorrorAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new VanishWhenStaredGoal(this, 120, 0.5f));
        goalSelector.add(2, new CreepTowardsGoal(this, 0.8, 1.1, 0.0f));
        goalSelector.add(3, new StalkPlayerGoal(this, 10.0, 24.0, 0.9));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient || isApparition() || age % 20 != 0) {
            return;
        }
        // light is lethal to it
        if (getWorld().getLightLevel(getBlockPos()) > 11) {
            vanish(true);
            return;
        }
        // fear aura: darkness, heavy limbs, crumbling mind
        ServerPlayerEntity victim = closestSurvivalPlayer(5.0);
        if (victim != null) {
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0, true, false));
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1, true, false));
            SanityManager.modify(victim, -1.5f);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(4) == 0 ? SoundEvents.ENTITY_WARDEN_AMBIENT : null;
    }

    @Override
    public float getSoundPitch() {
        return 0.3f + random.nextFloat() * 0.2f;
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
