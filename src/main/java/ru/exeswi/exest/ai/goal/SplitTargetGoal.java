package ru.exeswi.exest.ai.goal;

import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import ru.exeswi.exest.ai.PackCoordinator;
import ru.exeswi.exest.entity.base.AbstractHorrorEntity;

/**
 * Target selection that consults the {@link PackCoordinator}: in multiplayer, hunters
 * spread across different players instead of dog-piling the same victim.
 */
public class SplitTargetGoal extends ActiveTargetGoal<PlayerEntity> {

    private final AbstractHorrorEntity horror;

    public SplitTargetGoal(AbstractHorrorEntity mob) {
        super(mob, PlayerEntity.class, true);
        this.horror = mob;
    }

    @Override
    public boolean canStart() {
        double range = horror.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE);
        PlayerEntity picked = PackCoordinator.pickTarget(horror, range);
        if (picked == null) {
            return false;
        }
        this.targetEntity = picked;
        return true;
    }
}
