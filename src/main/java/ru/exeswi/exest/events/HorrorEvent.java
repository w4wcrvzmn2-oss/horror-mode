package ru.exeswi.exest.events;

import net.minecraft.server.network.ServerPlayerEntity;
import ru.exeswi.exest.config.HorrorConfig;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * A single horror event definition. Events are data: an id, a weight, a cooldown,
 * a difficulty gate, config/condition predicates and an action. The manager rolls
 * weighted dice over all eligible events, so behavior stays unpredictable while
 * every event remains individually testable via /horror event <id>.
 */
public final class HorrorEvent {

    public enum Category { AMBIENT, ENCOUNTER, FAKE, RARE, WORLD }

    private final String id;
    private final Category category;
    private final int weight;
    private final long cooldownTicks;
    private final int minDifficulty;
    private final Predicate<HorrorConfig> enabled;
    private final Predicate<ServerPlayerEntity> condition;
    private final BiConsumer<ServerPlayerEntity, HorrorEventManager> action;

    private HorrorEvent(Builder builder) {
        this.id = builder.id;
        this.category = builder.category;
        this.weight = builder.weight;
        this.cooldownTicks = builder.cooldownTicks;
        this.minDifficulty = builder.minDifficulty;
        this.enabled = builder.enabled;
        this.condition = builder.condition;
        this.action = builder.action;
    }

    public static Builder builder(String id, Category category) {
        return new Builder(id, category);
    }

    public String id() {
        return id;
    }

    public Category category() {
        return category;
    }

    public int weight() {
        return weight;
    }

    public long cooldownTicks() {
        return cooldownTicks;
    }

    public int minDifficulty() {
        return minDifficulty;
    }

    public boolean isEnabled(HorrorConfig config) {
        return enabled.test(config);
    }

    public boolean canRun(ServerPlayerEntity player) {
        return condition.test(player);
    }

    public void run(ServerPlayerEntity player, HorrorEventManager manager) {
        action.accept(player, manager);
    }

    public static final class Builder {
        private final String id;
        private final Category category;
        private int weight = 10;
        private long cooldownTicks = 600;
        private int minDifficulty = 0;
        private Predicate<HorrorConfig> enabled = config -> true;
        private Predicate<ServerPlayerEntity> condition = player -> true;
        private BiConsumer<ServerPlayerEntity, HorrorEventManager> action = (player, manager) -> {};

        private Builder(String id, Category category) {
            this.id = id;
            this.category = category;
        }

        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public Builder cooldown(long ticks) {
            this.cooldownTicks = ticks;
            return this;
        }

        public Builder minDifficulty(int level) {
            this.minDifficulty = level;
            return this;
        }

        public Builder enabledWhen(Predicate<HorrorConfig> enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder condition(Predicate<ServerPlayerEntity> condition) {
            this.condition = condition;
            return this;
        }

        public Builder action(BiConsumer<ServerPlayerEntity, HorrorEventManager> action) {
            this.action = action;
            return this;
        }

        public HorrorEvent register() {
            HorrorEvent event = new HorrorEvent(this);
            HorrorEventManager.register(event);
            return event;
        }
    }
}
