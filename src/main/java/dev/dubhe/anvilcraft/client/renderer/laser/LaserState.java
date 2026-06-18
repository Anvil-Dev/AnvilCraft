package dev.dubhe.anvilcraft.client.renderer.laser;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public record LaserState(
    BaseLaserBlockEntity blockEntity,
    BlockPos pos,
    float length,
    float offset,
    int laserLevel,
    boolean gamma,
    PoseStack.Pose pose,
    TextureAtlasSprite laserAtlasSprite,
    TextureAtlasSprite concreteAtlasSprite
) {
    @SuppressWarnings("deprecation")
    public static @Nullable LaserState create(BaseLaserBlockEntity blockEntity, PoseStack poseStack) {
        if (blockEntity.getIrradiateBlockPos() == null) return null;
        Function<ResourceLocation, TextureAtlasSprite> spriteGetter = Minecraft.getInstance()
            .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5);
        float length = (float) (blockEntity
            .getIrradiateBlockPos()
            .getCenter()
            .distanceTo(blockEntity.getBlockPos().getCenter()) - 0.5);
        poseStack.mulPose(blockEntity.getFacing().getRotation());
        boolean gamma = blockEntity instanceof CelestialForgingAnvilLaserInterfaceBlockEntity cfaLaser
            && cfaLaser.isEmittingGamma();
        LaserState laserState = new LaserState(
            blockEntity,
            blockEntity.getBlockPos(),
            length,
            blockEntity.getLaserOffset(),
            blockEntity.getLaserLevel(),
            gamma,
            poseStack.last(),
            spriteGetter
                .apply(AnvilCraft.of("block/laser")),
            spriteGetter
                .apply(ResourceLocation.withDefaultNamespace("block/white_concrete"))
        );
        poseStack.popPose();
        return laserState;
    }
}
