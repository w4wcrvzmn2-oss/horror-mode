package ru.exeswi.exest.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;
import ru.exeswi.exest.networking.HorrorEffect;
import ru.exeswi.exest.networking.HorrorNetworking;
import ru.exeswi.exest.networking.SoundCue;
import ru.exeswi.exest.sanity.SanityManager;

/**
 * Ridavoumax. The name from the journal that was never meant to be read aloud.
 * Benton read it. Murder heard it. You are next.
 *
 * It does not run, does not hide and does not go around: it walks toward you in a
 * perfectly straight line — through stone, through your walls, through the floor.
 * Weapons pass through it like through cold air. Light means nothing. It is slow,
 * and that is the whole horror of it: you can outrun it forever, but you have to
 * keep moving. One touch is all it needs — and all it came for.
 */
public class RidavoumaxEntity extends AbstractHorrorEntity {

    private static final int LIFETIME_TICKS = 2400;
    private static final double DRIFT_SPEED = 0.062;

    public RidavoumaxEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
    }

    public static DefaultAttributeContainer.Builder createRidavoumaxAttributes() {
        return createBaseHorrorAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 66.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
        // no goals: it has exactly one idea, and the idea is you
    }

    @Override
    public void tick() {
        // phasing must be set every tick, before movement is applied
        noClip = true;
        super.tick();
        if (getWorld().isClient || isApparition()) {
            return;
        }
        if (age > LIFETIME_TICKS) {
            vanish(true);
            return;
        }
        ServerPlayerEntity victim = closestSurvivalPlayer(64.0);
        if (victim == null) {
            setVelocity(Vec3d.ZERO);
            return;
        }
        setTarget(victim);
        lookAtEntity(victim, 360.0f, 90.0f);
        setHeadYaw(getYaw());
        setBodyYaw(getYaw());
        // the straight line: no pathfinding, no obstacles, no hurry
        Vec3d direction = victim.getEyePos().subtract(getPos().add(0, getStandingEyeHeight(), 0));
        if (direction.lengthSquared() < 2.2 * 2.2) {
            touch(victim);
            return;
        }
        setVelocity(direction.normalize().multiply(DRIFT_SPEED));
        velocityModified = true;
        // the closer it gets, the louder your own heart becomes
        if (age % 50 == 0 && direction.lengthSquared() < 20.0 * 20.0) {
            HorrorNetworking.sendCueBehind(victim, SoundCue.HEARTBEAT,
                    (float) ru.exeswi.exest.config.ConfigManager.get().audioIntensity);
        }
    }

    /** The touch: what it came for. Then it is gone — with a piece of you. */
    private void touch(ServerPlayerEntity victim) {
        victim.damage(getWorld().getDamageSources().mobAttack(this), 8.0f);
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 120, 0, true, false));
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1, true, false));
        HorrorNetworking.sendEffect(victim, HorrorEffect.JUMPSCARE_FACE, 1.0f, 18);
        HorrorNetworking.sendCueBehind(victim, SoundCue.VOICES,
                (float) ru.exeswi.exest.config.ConfigManager.get().audioIntensity);
        SanityManager.modify(victim, -25.0f);
        vanish(true);
    }

    /** Weapons pass through it like through cold air. It does not even slow down. */
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!getWorld().isClient && source.getAttacker() instanceof ServerPlayerEntity attacker) {
            HorrorNetworking.sendEffect(attacker, HorrorEffect.STATIC, 0.7f, 10);
            getWorld().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 0.8f, 0.5f);
            return false;
        }
        return super.damage(source, amount); // /kill and the void still apply
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WARDEN_HEARTBEAT;
    }

    @Override
    public float getSoundPitch() {
        return 0.45f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WARDEN_SONIC_BOOM;
    }
}
