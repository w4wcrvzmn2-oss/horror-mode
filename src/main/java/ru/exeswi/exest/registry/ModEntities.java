package ru.exeswi.exest.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import ru.exeswi.exest.Exest;
import ru.exeswi.exest.entity.BrokenVillagerEntity;
import ru.exeswi.exest.entity.CrawlerEntity;
import ru.exeswi.exest.entity.DistortedEndermanEntity;
import ru.exeswi.exest.entity.EyelessZombieEntity;
import ru.exeswi.exest.entity.FacelessEntity;
import ru.exeswi.exest.entity.FleshEntity;
import ru.exeswi.exest.entity.MimicEntity;
import ru.exeswi.exest.entity.MurderEntity;
import ru.exeswi.exest.entity.RidavoumaxEntity;
import ru.exeswi.exest.entity.PredatorEntity;
import ru.exeswi.exest.entity.ShadowEntity;
import ru.exeswi.exest.entity.SmilerEntity;
import ru.exeswi.exest.entity.StalkerEntity;

/**
 * All horror creatures. They never spawn through the vanilla spawn cycle: the
 * {@link ru.exeswi.exest.events.HorrorEventManager} places them deliberately, always
 * outside the player's field of view, so encounters stay orchestrated and rare.
 */
public final class ModEntities {

    public static final EntityType<StalkerEntity> STALKER = register("stalker",
            EntityType.Builder.create(StalkerEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.99f).maxTrackingRange(96));
    public static final EntityType<ShadowEntity> SHADOW = register("shadow",
            EntityType.Builder.create(ShadowEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.7f, 2.6f).maxTrackingRange(96));
    public static final EntityType<SmilerEntity> SMILER = register("smiler",
            EntityType.Builder.create(SmilerEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.95f).maxTrackingRange(96));
    public static final EntityType<BrokenVillagerEntity> BROKEN_VILLAGER = register("broken_villager",
            EntityType.Builder.create(BrokenVillagerEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.95f).maxTrackingRange(96));
    public static final EntityType<CrawlerEntity> CRAWLER = register("crawler",
            EntityType.Builder.create(CrawlerEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.1f, 0.6f).maxTrackingRange(96));
    public static final EntityType<FacelessEntity> FACELESS = register("faceless",
            EntityType.Builder.create(FacelessEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.8f).maxTrackingRange(96));
    public static final EntityType<EyelessZombieEntity> EYELESS_ZOMBIE = register("eyeless_zombie",
            EntityType.Builder.create(EyelessZombieEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.95f).maxTrackingRange(96));
    public static final EntityType<DistortedEndermanEntity> DISTORTED_ENDERMAN = register("distorted_enderman",
            EntityType.Builder.create(DistortedEndermanEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 2.9f).maxTrackingRange(96));
    public static final EntityType<FleshEntity> FLESH = register("flesh",
            EntityType.Builder.create(FleshEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.0f, 2.2f).maxTrackingRange(96));
    public static final EntityType<MimicEntity> MIMIC = register("mimic",
            EntityType.Builder.create(MimicEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.8f).maxTrackingRange(96));
    public static final EntityType<PredatorEntity> PREDATOR = register("predator",
            EntityType.Builder.create(PredatorEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.9f).maxTrackingRange(96));
    public static final EntityType<MurderEntity> MURDER = register("murder",
            EntityType.Builder.create(MurderEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.95f).maxTrackingRange(96));
    public static final EntityType<RidavoumaxEntity> RIDAVOUMAX = register("ridavoumax",
            EntityType.Builder.create(RidavoumaxEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 2.1f).maxTrackingRange(96));

    private ModEntities() {
    }

    private static <T extends net.minecraft.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        return Registry.register(Registries.ENTITY_TYPE, Exest.id(name), builder.build());
    }

    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(STALKER, StalkerEntity.createStalkerAttributes());
        FabricDefaultAttributeRegistry.register(SHADOW, ShadowEntity.createShadowAttributes());
        FabricDefaultAttributeRegistry.register(SMILER, SmilerEntity.createSmilerAttributes());
        FabricDefaultAttributeRegistry.register(BROKEN_VILLAGER, BrokenVillagerEntity.createBrokenVillagerAttributes());
        FabricDefaultAttributeRegistry.register(CRAWLER, CrawlerEntity.createCrawlerAttributes());
        FabricDefaultAttributeRegistry.register(FACELESS, FacelessEntity.createFacelessAttributes());
        FabricDefaultAttributeRegistry.register(EYELESS_ZOMBIE, EyelessZombieEntity.createEyelessZombieAttributes());
        FabricDefaultAttributeRegistry.register(DISTORTED_ENDERMAN, DistortedEndermanEntity.createDistortedEndermanAttributes());
        FabricDefaultAttributeRegistry.register(FLESH, FleshEntity.createFleshAttributes());
        FabricDefaultAttributeRegistry.register(MIMIC, MimicEntity.createMimicAttributes());
        FabricDefaultAttributeRegistry.register(PREDATOR, PredatorEntity.createPredatorAttributes());
        FabricDefaultAttributeRegistry.register(MURDER, MurderEntity.createMurderAttributes());
        FabricDefaultAttributeRegistry.register(RIDAVOUMAX, RidavoumaxEntity.createRidavoumaxAttributes());
    }
}
