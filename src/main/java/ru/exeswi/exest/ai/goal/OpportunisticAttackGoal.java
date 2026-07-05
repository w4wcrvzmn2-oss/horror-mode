package ru.exeswi.exest.ai.goal;

import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

/**
 * Melee attack that only engages when the situation favors the monster: darkness,
 * a wounded or isolated victim, or numerical advantage. Otherwise the mob keeps
 * stalking instead of mindlessly charging.
 */
public class OpportunisticAttackGoal extends MeleeAttackGoal {

    private final AbstractHorrorEntity horror;

    public OpportunisticAttackGoal(AbstractHorrorEntity mob, double speed) {
        super(mob, speed, true);
        this.horror = mob;
    }

    @Override
    public boolean canStart() {
        return super.canStart() && horror.isAttackAdvantageous();
    }

    @Override
    public boolean shouldContinue() {
        // once committed, only a drastic change of situation makes it break off
        return super.shouldContinue()
                && (horror.isAttackAdvantageous() || horror.getRandom().nextFloat() < 0.95f);
    }
}
