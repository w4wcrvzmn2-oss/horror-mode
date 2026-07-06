package ru.exeswi.exest.config;

/**
 * All user-facing settings. Loaded from config/exest-horror.json by {@link ConfigManager}.
 * Every value can be changed at runtime through /horror config or by editing the file
 * and running /horror reload.
 */
public class HorrorConfig {

    // Master switches
    public boolean enableJumpscares = true;
    public boolean enableDarkness = true;
    public boolean enableSanity = true;
    public boolean enableHallucinations = true;
    public boolean enableVisualEffects = true;
    public boolean enableFakeMessages = true;
    public boolean enableWorldEvents = true;
    public boolean enableCorruption = true;
    public boolean enableMonsters = true;

    // Tuning knobs, all multipliers around 1.0
    public double monsterFrequency = 1.0;
    public double eventFrequency = 3.0;
    public double audioIntensity = 1.0;
    public double difficultyScale = 1.0;
    public double spawnRateMultiplier = 1.0;

    /** Chance of the abduction sequence firing on join/respawn, 0..1. */
    public double abductionChance = 0.68;
    /** How many horror mobs may exist around a single player at once. */
    public int maxHorrorMobsPerPlayer = 2;
    /** Extra logging + relaxed cooldowns for testing. */
    public boolean debugMode = false;

    /** Clamps everything into sane ranges after loading a possibly hand-edited file. */
    public void sanitize() {
        monsterFrequency = clamp(monsterFrequency, 0.0, 5.0);
        eventFrequency = clamp(eventFrequency, 0.0, 5.0);
        audioIntensity = clamp(audioIntensity, 0.0, 2.0);
        difficultyScale = clamp(difficultyScale, 0.0, 5.0);
        spawnRateMultiplier = clamp(spawnRateMultiplier, 0.0, 5.0);
        abductionChance = clamp(abductionChance, 0.0, 1.0);
        maxHorrorMobsPerPlayer = (int) clamp(maxHorrorMobsPerPlayer, 0, 8);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
