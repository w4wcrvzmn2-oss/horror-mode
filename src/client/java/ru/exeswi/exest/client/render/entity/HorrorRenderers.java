package ru.exeswi.exest.client.render.entity;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EndermanEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.SpiderEntityModel;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.util.Identifier;
import ru.exeswi.exest.Exest;
import ru.exeswi.exest.registry.ModEntities;

/**
 * Binds every horror creature to a {@link HorrorMobRenderer} configuration:
 * which vanilla model wears which vanilla skin, at what size, and whether the thing
 * renders as a pitch-black silhouette.
 */
public final class HorrorRenderers {

    private static final Identifier STEVE = Identifier.ofVanilla("textures/entity/player/wide/steve.png");
    private static final Identifier ZOMBIE = Identifier.ofVanilla("textures/entity/zombie/zombie.png");
    private static final Identifier VILLAGER = Identifier.ofVanilla("textures/entity/villager/villager.png");
    private static final Identifier SPIDER = Identifier.ofVanilla("textures/entity/spider/spider.png");
    private static final Identifier ENDERMAN = Identifier.ofVanilla("textures/entity/enderman/enderman.png");

    private static final Identifier BENTON = Exest.id("textures/entity/benton.png");
    private static final Identifier STALKER_EYES = Exest.id("textures/entity/stalker_eyes.png");
    private static final Identifier SMILER = Exest.id("textures/entity/smiler.png");
    private static final Identifier SMILER_EYES = Exest.id("textures/entity/smiler_eyes.png");
    private static final Identifier SHADOW_EYES = Exest.id("textures/entity/shadow_eyes.png");
    private static final Identifier MURDER = Exest.id("textures/entity/murder.png");
    private static final Identifier RIDAVOUMAX = Exest.id("textures/entity/ridavoumax.png");

    private HorrorRenderers() {
    }

    public static void register() {
        EntityRendererRegistry.register(ModEntities.STALKER, ctx -> new HorrorMobRenderer<>(ctx,
                new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false),
                BENTON, STALKER_EYES, 0.5f, 1.05f, false));
        EntityRendererRegistry.register(ModEntities.SHADOW, ctx -> new HorrorMobRenderer<>(ctx,
                new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), true),
                STEVE, SHADOW_EYES, 0.4f, 1.35f, true));
        EntityRendererRegistry.register(ModEntities.SMILER, ctx -> new HorrorMobRenderer<>(ctx,
                new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false),
                SMILER, SMILER_EYES, 0.5f, 1.0f, false));
        EntityRendererRegistry.register(ModEntities.BROKEN_VILLAGER, ctx -> new HorrorMobRenderer<>(ctx,
                new VillagerResemblingModel<>(ctx.getPart(EntityModelLayers.VILLAGER)), VILLAGER, 0.5f, 1.0f, false));
        EntityRendererRegistry.register(ModEntities.CRAWLER, ctx -> new HorrorMobRenderer<>(ctx,
                new SpiderEntityModel<>(ctx.getPart(EntityModelLayers.SPIDER)), SPIDER, 0.6f, 0.9f, true));
        EntityRendererRegistry.register(ModEntities.FACELESS, ctx -> new HorrorMobRenderer<>(ctx,
                new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), STEVE, 0.5f, 1.0f, true));
        EntityRendererRegistry.register(ModEntities.EYELESS_ZOMBIE, ctx -> new HorrorMobRenderer<>(ctx,
                new BipedEntityModel<>(ctx.getPart(EntityModelLayers.ZOMBIE)), ZOMBIE, 0.5f, 1.0f, true));
        EntityRendererRegistry.register(ModEntities.DISTORTED_ENDERMAN, ctx -> new HorrorMobRenderer<>(ctx,
                new EndermanEntityModel<>(ctx.getPart(EntityModelLayers.ENDERMAN)), ENDERMAN, 0.5f, 1.0f, false));
        EntityRendererRegistry.register(ModEntities.FLESH, ctx -> new HorrorMobRenderer<>(ctx,
                new BipedEntityModel<>(ctx.getPart(EntityModelLayers.ZOMBIE)), ZOMBIE, 0.7f, 1.3f, true));
        EntityRendererRegistry.register(ModEntities.MIMIC, ctx -> new HorrorMobRenderer<>(ctx,
                new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), STEVE, 0.5f, 1.0f, false));
        EntityRendererRegistry.register(ModEntities.PREDATOR, ctx -> new HorrorMobRenderer<>(ctx,
                new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), STEVE, 0.0f, 1.0f, true));
        EntityRendererRegistry.register(ModEntities.MURDER, ctx -> new HorrorMobRenderer<>(ctx,
                new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), MURDER, 0.5f, 1.0f, false));
        EntityRendererRegistry.register(ModEntities.RIDAVOUMAX, ctx -> new HorrorMobRenderer<>(ctx,
                new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false),
                RIDAVOUMAX, SHADOW_EYES, 0.0f, 1.15f, false));
    }
}
