package ru.exeswi.exest.entity.base;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import ru.exeswi.exest.config.ConfigManager;
import ru.exeswi.exest.difficulty.DifficultyScaler;
import ru.exeswi.exest.networking.HorrorEffect;
import ru.exeswi.exest.networking.HorrorNetworking;
import ru.exeswi.exest.networking.SoundCue;
import ru.exeswi.exest.sanity.SanityManager;
import ru.exeswi.exest.util.HorrorUtil;

/**
 * Shared foundation of every Horror Mode creature.
 *
 * Provides stare tracking (players looking directly at the mob), silent vanishing,
 * out-of-sight teleportation, a sanity-drain aura, apparition mode (a harmless visual
 * that despawns when approached) and the "attack only when advantageous" heuristic.
 * A per-instance {@code unpredictability} value seeds all behavior thresholds so no
 * two individuals feel identical.
 */
public abstract class AbstractHorrorEntity extends HostileEntity {

    private final float unpredictability;
    private int stareTicks;
    private int lifeTicks;
    private int maxLifeTicks;
    private boolean apparition;
    private int apparitionMinDistance = 8;
    private boolean enraged;
    private int enragedTicks;
    private boolean pointBlankUsed;
    private int frustrationTicks;
    private double lastTargetDistSq = Double.MAX_VALUE;
    private BlockPos supportBreakPos;
    private int supportBreakAt;
    private boolean finalBoss;
    private boolean spawnedBehind;
    private boolean everStared;

    protected AbstractHorrorEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 0;
        this.unpredictability = this.random.nextFloat();
        this.setPathfindingPenalty(PathNodeType.DOOR_WOOD_CLOSED, 0.0f);
    }

    public static DefaultAttributeContainer.Builder createBaseHorrorAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) {
            return;
        }
        lifeTicks++;
        updateStare();
        if (apparition) {
            tickApparition();
            return;
        }
        if (enraged) {
            tickEnrage();
            if (isRemoved()) {
                return;
            }
        } else {
            tickPointBlankScare();
            if (isRemoved()) {
                return;
            }
        }
        tickCampingCounter();
        // creatures that overstayed their welcome leave quietly, but never on camera
        if (maxLifeTicks > 0 && lifeTicks > maxLifeTicks && !isSeenByAnyone()) {
            discard();
            return;
        }
        // just being close to one of these things and seeing it erodes the mind
        if (lifeTicks % 20 == 0) {
            PlayerEntity player = closestSurvivalPlayer(12.0);
            if (player instanceof ServerPlayerEntity serverPlayer && HorrorUtil.isLookedAtBy(player, this)) {
                SanityManager.modify(serverPlayer, -0.3f * DifficultyScaler.sanityDrainMultiplier((ServerWorld) getWorld()));
            }
        }
    }

    private void updateStare() {
        boolean stared = false;
        for (PlayerEntity player : getWorld().getPlayers()) {
            if (!player.isSpectator() && player.isAlive()
                    && player.squaredDistanceTo(this) < 64.0 * 64.0
                    && HorrorUtil.isLookedAtBy(player, this)) {
                stared = true;
                if (stareTicks == 20 && player instanceof ServerPlayerEntity serverPlayer) {
                    // one full second of eye contact counts as a real encounter
                    everStared = true;
                    ru.exeswi.exest.stats.HorrorStats.inc(serverPlayer,
                            ru.exeswi.exest.stats.HorrorStats.Stat.SEEN);
                }
                break;
            }
        }
        stareTicks = stared ? stareTicks + 1 : 0;
    }

    private void tickApparition() {
        setAiDisabled(true);
        PlayerEntity player = closestSurvivalPlayer(64.0);
        if (player == null) {
            discard();
            return;
        }
        lookAtEntity(player, 360.0f, 90.0f);
        setHeadYaw(getYaw());
        setBodyYaw(getYaw());
        boolean approached = squaredDistanceTo(player) < apparitionMinDistance * apparitionMinDistance;
        boolean staredOut = stareTicks > 50 + random.nextInt(60);
        boolean expired = maxLifeTicks > 0 && lifeTicks > maxLifeTicks;
        if (approached || staredOut || expired) {
            // it stood right behind them the whole time and they never turned around
            if (expired && spawnedBehind && !everStared
                    && player instanceof ServerPlayerEntity serverPlayer) {
                ru.exeswi.exest.stats.HorrorStats.inc(serverPlayer,
                        ru.exeswi.exest.stats.HorrorStats.Stat.BEHIND_UNSEEN);
            }
            vanish(approached || staredOut);
        }
    }

    public void markSpawnedBehind() {
        spawnedBehind = true;
    }

    /**
     * The chase mode: relentless sprint after one victim, no vanishing under eye
     * contact, loud footsteps. Ends with a single brutal hit, an escape into bright
     * light, too much distance, or a timeout — and the thing is gone in smoke.
     */
    public void enrage(ServerPlayerEntity victim) {
        enraged = true;
        setTarget(victim);
        var speed = getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * 1.35);
        }
    }

    public boolean isEnraged() {
        return enraged;
    }

    /**
     * The final hunt's boss: enormous health, no vanishing rules, no escape into
     * light, no one-hit-and-gone. It stops only when it is dead — or you are.
     */
    public void makeFinalBoss() {
        finalBoss = true;
        setPersistent();
        var health = getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(300.0);
            setHealth(300.0f);
        }
        var damage = getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(12.0);
        }
    }

    public boolean isFinalBoss() {
        return finalBoss;
    }

    private void tickEnrage() {
        enragedTicks++;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            if (finalBoss) {
                // the boss never gives up — it just picks the next victim
                PlayerEntity next = getWorld().getClosestPlayer(this, 128.0);
                if (next instanceof ServerPlayerEntity serverPlayer
                        && !next.isCreative() && !next.isSpectator()) {
                    setTarget(serverPlayer);
                }
                return;
            }
            vanish(false);
            return;
        }
        if (finalBoss) {
            // fell too far behind: it simply appears closer
            if (squaredDistanceTo(target) > 40.0 * 40.0 && enragedTicks % 60 == 0) {
                teleportOutsideView(12.0, 20.0);
            }
            return;
        }
        if (enragedTicks > 900) {
            reportChaseEnd(target, true);
            vanish(false);
            return;
        }
        if (squaredDistanceTo(target) > 40.0 * 40.0) {
            reportChaseEnd(target, true);
            vanish(true);
            return;
        }
        // standing in bright light breaks the hunt — light is the way out
        if (getWorld().getLightLevel(target.getBlockPos()) >= 12
                && enragedTicks > 80 && random.nextInt(50) == 0) {
            reportChaseEnd(target, true);
            vanish(true);
        }
    }

    private void reportChaseEnd(LivingEntity target, boolean escaped) {
        if (target instanceof ServerPlayerEntity player) {
            ru.exeswi.exest.stats.HorrorStats.inc(player, escaped
                    ? ru.exeswi.exest.stats.HorrorStats.Stat.CHASES_ESCAPED
                    : ru.exeswi.exest.stats.HorrorStats.Stat.CHASES_CAUGHT);
        }
    }

    /**
     * The point-blank scare: a passively stalking creature caught within three blocks,
     * eye to eye, screams into the player's face once and is gone.
     */
    private void tickPointBlankScare() {
        if (apparition || pointBlankUsed || getTarget() != null
                || lifeTicks % 5 != 0 || !ConfigManager.get().enableJumpscares) {
            return;
        }
        ServerPlayerEntity victim = closestSurvivalPlayer(3.0);
        if (victim != null && HorrorUtil.isLookedAtBy(victim, this)) {
            pointBlankUsed = true;
            HorrorNetworking.sendEffect(victim, HorrorEffect.JUMPSCARE_FACE, 1.0f, 14);
            HorrorNetworking.sendCueBehind(victim, SoundCue.STING, 1.0f);
            SanityManager.modify(victim, -10.0f);
            vanish(true);
        }
    }

    @Override
    public boolean tryAttack(net.minecraft.entity.Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && enraged && !finalBoss) {
            // one hit is all it wanted
            if (target instanceof LivingEntity living) {
                reportChaseEnd(living, false);
            }
            vanish(true);
        }
        return hit;
    }

    /**
     * Phantom creatures cannot be fought: a player's swing goes through empty air, the
     * screen tears with static, and the thing is already somewhere behind you. Only
     * enraged (chasing) individuals are solid enough to hurt — running is not the only
     * answer then, but it is still the best one.
     */
    @Override
    public boolean damage(net.minecraft.entity.damage.DamageSource source, float amount) {
        if (!getWorld().isClient && dodgesPlayerAttacks() && !enraged
                && source.getAttacker() instanceof ServerPlayerEntity attacker) {
            if (apparition) {
                vanish(true);
            } else {
                getWorld().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.6f, 0.3f);
                if (!teleportOutsideView(8.0, 20.0)) {
                    vanish(true);
                }
            }
            HorrorNetworking.sendEffect(attacker, HorrorEffect.STATIC, 0.5f, 8);
            SanityManager.modify(attacker, -3.0f);
            return false;
        }
        return super.damage(source, amount);
    }

    /** Physical creatures (zombie-like, the Flesh...) override this to stay fightable. */
    protected boolean dodgesPlayerAttacks() {
        return true;
    }

    /**
     * Anti-camping: an ice floe, a one-block island in the sea, a dirt pillar in the
     * sky — none of it is safety. A hunter that cannot close the distance for ~8
     * seconds either teleports right next to its prey, or — when there is nowhere to
     * stand at all — knocks from below and shatters the single block they stand on.
     */
    private void tickCampingCounter() {
        if (supportBreakPos != null) {
            tickSupportBreak();
            return;
        }
        if (age % 20 != 0) {
            return;
        }
        if (!(getTarget() instanceof ServerPlayerEntity victim) || !victim.isAlive()) {
            frustrationTicks = 0;
            return;
        }
        double distSq = squaredDistanceTo(victim);
        if (distSq > 9.0 && distSq >= lastTargetDistSq - 1.0) {
            frustrationTicks += 20;
        } else {
            frustrationTicks = 0;
        }
        lastTargetDistSq = distSq;
        if (frustrationTicks >= 160) {
            frustrationTicks = 0;
            punishCamping(victim);
        }
    }

    private void punishCamping(ServerPlayerEntity victim) {
        ServerWorld world = (ServerWorld) getWorld();
        // any standable spot right next to the victim will do — view cone be damned
        BlockPos spot = HorrorUtil.findGroundSpot(world, victim, 1.0, 4.0, false, random);
        if (spot != null && teleportToSpot(spot)) {
            HorrorNetworking.sendEffect(victim, HorrorEffect.STATIC, 0.6f, 10);
            world.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.9f, 0.3f);
            return;
        }
        // nowhere to stand: take away the one block holding them up
        BlockPos support = victim.getBlockPos().down();
        net.minecraft.block.BlockState state = world.getBlockState(support);
        if (victim.isOnGround() && !state.isAir() && !state.hasBlockEntity()
                && state.getHardness(world, support) >= 0.0f) {
            supportBreakPos = support.toImmutable();
            supportBreakAt = age + 45;
        }
    }

    private void tickSupportBreak() {
        ServerWorld world = (ServerWorld) getWorld();
        net.minecraft.block.BlockState state = world.getBlockState(supportBreakPos);
        if (state.isAir()) {
            supportBreakPos = null;
            return;
        }
        // knocking from below before it gives way
        if (age % 12 == 0 && age < supportBreakAt) {
            world.playSound(null, supportBreakPos, state.getSoundGroup().getHitSound(),
                    SoundCategory.HOSTILE, 1.0f, 0.5f);
        }
        if (age >= supportBreakAt) {
            world.breakBlock(supportBreakPos, false, this);
            supportBreakPos = null;
        }
    }

    /**
     * A pounding heart is a beacon. Every time the presence radar makes the player's
     * heart beat, the creature that caused it learns exactly where they are — walls,
     * darkness and hiding change nothing. Close enough, and it commits to the kill.
     */
    public void onHeartbeatHeard(ServerPlayerEntity player) {
        if (apparition || !player.isAlive()) {
            return;
        }
        if (squaredDistanceTo(player) < 10.0 * 10.0) {
            if (getTarget() == null) {
                setTarget(player);
            }
        } else if (getNavigation().isIdle()) {
            getNavigation().startMovingTo(player.getX(), player.getY(), player.getZ(), 1.0);
        }
    }

    // --- helpers used by AI goals and spawners ---

    public float unpredictability() {
        return unpredictability;
    }

    public int getStareTicks() {
        return stareTicks;
    }

    public void resetStare() {
        stareTicks = 0;
    }

    public boolean isApparition() {
        return apparition;
    }

    /** Turns the mob into a harmless apparition that despawns when approached within the given distance. */
    public void becomeApparition(int maxLifeTicks, int minDistance) {
        this.apparition = true;
        this.maxLifeTicks = maxLifeTicks;
        this.apparitionMinDistance = minDistance;
        setAiDisabled(true);
        setPersistent();
    }

    public void setMaxLife(int ticks) {
        this.maxLifeTicks = ticks;
    }

    @Nullable
    public ServerPlayerEntity closestSurvivalPlayer(double range) {
        PlayerEntity player = getWorld().getClosestPlayer(this, range);
        if (player instanceof ServerPlayerEntity serverPlayer
                && !player.isCreative() && !player.isSpectator() && player.isAlive()) {
            return serverPlayer;
        }
        return null;
    }

    public boolean isSeenByAnyone() {
        for (PlayerEntity player : getWorld().getPlayers()) {
            if (!player.isSpectator() && player.squaredDistanceTo(this) < 128.0 * 128.0
                    && HorrorUtil.isInViewCone(player, getBoundingBox().getCenter())) {
                return true;
            }
        }
        return false;
    }

    public boolean teleportToSpot(BlockPos pos) {
        boolean moved = teleport(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, false);
        if (moved) {
            getNavigation().stop();
            resetStare();
        }
        return moved;
    }

    public boolean teleportOutsideView(double minRadius, double maxRadius) {
        PlayerEntity player = closestSurvivalPlayer(64.0);
        if (player == null || !(getWorld() instanceof ServerWorld serverWorld)) {
            return false;
        }
        BlockPos spot = HorrorUtil.findGroundSpot(serverWorld, player, minRadius, maxRadius, true, random);
        return spot != null && teleportToSpot(spot);
    }

    /** Removes the mob, optionally with a puff of smoke and a faint sound the player can notice. */
    public void vanish(boolean noticeable) {
        if (getWorld() instanceof ServerWorld serverWorld) {
            if (noticeable) {
                serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE,
                        getX(), getBodyY(0.5), getZ(), 12, 0.3, 0.6, 0.3, 0.02);
                serverWorld.playSound(null, getX(), getY(), getZ(),
                        SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.4f, 0.3f);
            }
        }
        discard();
    }

    /**
     * "Attack only when advantageous": darkness, a weakened or isolated victim,
     * numeric superiority, or simply a bold individual in a mature horror world.
     */
    public boolean isAttackAdvantageous() {
        if (enraged) {
            return true;
        }
        LivingEntity target = getTarget();
        if (target == null) {
            return false;
        }
        if (target.getHealth() <= 10.0f) {
            return true;
        }
        if (getWorld().getLightLevel(target.getBlockPos()) < 4) {
            return true;
        }
        if (target instanceof PlayerEntity player) {
            boolean isolated = getWorld().getPlayers().stream()
                    .noneMatch(p -> p != player && !p.isSpectator()
                            && p.squaredDistanceTo(player) < 24.0 * 24.0);
            if (isolated && getWorld().isNight()) {
                return true;
            }
        }
        long packSize = getWorld().getEntitiesByClass(AbstractHorrorEntity.class,
                Box.of(target.getPos(), 32, 16, 32), e -> !e.isApparition()).size();
        if (packSize >= 3) {
            return true;
        }
        int level = getWorld() instanceof ServerWorld sw ? DifficultyScaler.level(sw) : 0;
        return unpredictability > 0.85f && level >= 5;
    }

    /** Grants the difficulty-scaled damage bonus; call right after spawning. */
    public void applyDifficultyBonuses(ServerWorld world) {
        var attr = getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attr != null) {
            attr.setBaseValue(attr.getBaseValue() + DifficultyScaler.bonusDamage(world));
        }
    }

    // --- presentation defaults: silent steps, distorted voice ---

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.block.BlockState state) {
        // silent by default; the chase is the one time you are meant to hear it coming
        if (enraged) {
            playSound(state.getSoundGroup().getStepSound(), 0.6f, 0.6f);
        }
    }

    @Override
    public float getSoundPitch() {
        return 0.5f + random.nextFloat() * 0.3f;
    }

    protected Vec3d eyesPos() {
        return getEyePos();
    }
}
