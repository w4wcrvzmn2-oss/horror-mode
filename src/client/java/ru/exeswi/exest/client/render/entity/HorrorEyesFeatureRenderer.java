package ru.exeswi.exest.client.render.entity;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

/**
 * Emissive overlay: the eyes (and the Smiler's grin) glow at full brightness no matter
 * how dark the world is. Two pale points floating in the night is all you get to see —
 * and all you need to.
 */
public class HorrorEyesFeatureRenderer<T extends MobEntity, M extends EntityModel<T>>
        extends EyesFeatureRenderer<T, M> {

    private final RenderLayer layer;

    public HorrorEyesFeatureRenderer(FeatureRendererContext<T, M> context, Identifier texture) {
        super(context);
        this.layer = RenderLayer.getEyes(texture);
    }

    @Override
    public RenderLayer getEyesTexture() {
        return layer;
    }
}
