package ru.exeswi.exest.client.render.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * One renderer for the whole bestiary: it reuses vanilla models and textures but can
 * scale the mob and force it to render at light level zero, which turns any familiar
 * silhouette into a pitch-black figure. Cheap, zero custom assets, very effective.
 */
public class HorrorMobRenderer<T extends MobEntity> extends MobEntityRenderer<T, EntityModel<T>> {

    private final Identifier texture;
    private final float scale;
    private final boolean pitchBlack;

    public HorrorMobRenderer(EntityRendererFactory.Context context, EntityModel<T> model,
                             Identifier texture, float shadowRadius, float scale, boolean pitchBlack) {
        super(context, model, shadowRadius);
        this.texture = texture;
        this.scale = scale;
        this.pitchBlack = pitchBlack;
    }

    /** Same, plus a fullbright eyes overlay that stays visible in total darkness. */
    public HorrorMobRenderer(EntityRendererFactory.Context context, EntityModel<T> model,
                             Identifier texture, Identifier eyesTexture,
                             float shadowRadius, float scale, boolean pitchBlack) {
        this(context, model, texture, shadowRadius, scale, pitchBlack);
        addFeature(new HorrorEyesFeatureRenderer<>(this, eyesTexture));
    }

    @Override
    public Identifier getTexture(T entity) {
        return texture;
    }

    @Override
    protected void scale(T entity, MatrixStack matrices, float amount) {
        if (scale != 1.0f) {
            matrices.scale(scale, scale, scale);
        }
    }

    @Override
    protected int getBlockLight(T entity, BlockPos pos) {
        return pitchBlack ? 0 : super.getBlockLight(entity, pos);
    }

    @Override
    protected int getSkyLight(T entity, BlockPos pos) {
        return pitchBlack ? 0 : super.getSkyLight(entity, pos);
    }
}
