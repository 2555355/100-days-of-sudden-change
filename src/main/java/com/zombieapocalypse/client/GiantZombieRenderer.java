package com.zombieapocalypse.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;

/**
 * 巨型僵尸渲染器
 * 使用原版僵尸模型和纹理，通过矩阵变换应用2倍缩放
 */
public class GiantZombieRenderer extends ZombieEntityRenderer {

    private final float scale;

    public GiantZombieRenderer(EntityRendererFactory.Context context, float scale) {
        super(context,
                EntityModelLayers.ZOMBIE,
                EntityModelLayers.ZOMBIE_INNER_ARMOR,
                EntityModelLayers.ZOMBIE_OUTER_ARMOR);
        this.scale = scale;
        this.shadowRadius = 0.6f * scale;
    }

    @Override
    protected void scale(ZombieEntity zombieEntity, MatrixStack matrixStack, float f) {
        matrixStack.scale(this.scale, this.scale, this.scale);
    }

    @Override
    public Identifier getTexture(ZombieEntity zombieEntity) {
        return new Identifier("textures/entity/zombie/zombie.png");
    }
}