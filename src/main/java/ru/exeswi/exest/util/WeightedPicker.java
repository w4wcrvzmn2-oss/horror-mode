package ru.exeswi.exest.util;

import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Picks a random element from a list respecting integer weights. */
public final class WeightedPicker {

    private WeightedPicker() {
    }

    public static <T> T pick(List<T> items, ToIntFunction<T> weight, Random random) {
        int total = 0;
        for (T item : items) {
            total += Math.max(0, weight.applyAsInt(item));
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (T item : items) {
            roll -= Math.max(0, weight.applyAsInt(item));
            if (roll < 0) {
                return item;
            }
        }
        return null;
    }

    public static <T> List<T> filtered(List<T> items, java.util.function.Predicate<T> filter) {
        List<T> out = new ArrayList<>(items.size());
        for (T item : items) {
            if (filter.test(item)) {
                out.add(item);
            }
        }
        return out;
    }
}
