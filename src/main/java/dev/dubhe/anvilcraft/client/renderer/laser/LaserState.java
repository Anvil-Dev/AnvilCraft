package dev.dubhe.anvilcraft.client.renderer.laser;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.LensBlock;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CreativeLaserBlockEntity;
import dev.dubhe.anvilcraft.block.state.LensType;
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
    LensType lensType,
    PoseStack.Pose pose,
    TextureAtlasSprite laserAtlasSprite,
    TextureAtlasSprite concreteAtlasSprite
) {
    @SuppressWarnings(
        {"deprecation",
         "checkstyle:VariableDeclarationUsageDistance"
        }
    )
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
        boolean gamma = blockEntity.isEmittingGamma();
        LensType lensType = blockEntity.getBlockState().getBlock() instanceof LensBlock
            ? blockEntity.getBlockState().getValue(LensBlock.TYPE)
            : LensType.NONE;
        if (blockEntity instanceof CreativeLaserBlockEntity creativeLaser) {
            lensType = creativeLaser.getLensType();
        }
        LaserState laserState = new LaserState(
            blockEntity,
            blockEntity.getBlockPos(),
            length,
            blockEntity.getLaserOffset(),
            blockEntity.getLaserLevel(),
            gamma,
            lensType,
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
