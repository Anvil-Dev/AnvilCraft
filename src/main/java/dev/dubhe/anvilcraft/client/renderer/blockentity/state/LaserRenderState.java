package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderState;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.client.init.ModAtlasIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public class LaserRenderState extends CachedBlockEntityRenderState {
    public static final Identifier LASER_TEXTURE = AnvilCraft.of("laser/beam");
    public static final Identifier SOLID_TEXTURE = AnvilCraft.of("laser/solid");

    public @Nullable BaseLaserBlockEntity blockEntity;
    public float length;
    public float offset;
    public int color;
    public int laserLevel;
    public Quaternionf rotation;
    public TextureAtlasSprite laserAtlasSprite;
    public TextureAtlasSprite solidAtlasSprite;

    public LaserRenderState() {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(ModAtlasIds.LASER);
        this.rotation = new Quaternionf();
        this.laserAtlasSprite = atlas.getSprite(LaserRenderState.LASER_TEXTURE);
        this.solidAtlasSprite = atlas.getSprite(LaserRenderState.SOLID_TEXTURE);
    }

    public void extract(BaseLaserBlockEntity blockEntity) {
        this.blockEntity = null;
        if (blockEntity.getIrradiateBlockPos() == null) return;
        float length = (float) (blockEntity
            .getIrradiateBlockPos()
            .getCenter()
            .distanceTo(blockEntity.getBlockPos().getCenter()) - 0.5);

        final TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(ModAtlasIds.LASER);

        this.blockEntity = blockEntity;
        this.length = length;
        this.offset = blockEntity.getLaserOffset();
        this.color = blockEntity.getLaserColor();
        this.laserLevel = blockEntity.getLaserLevel();
        this.rotation = blockEntity.getFacing().getRotation();
        this.laserAtlasSprite = atlas.getSprite(LaserRenderState.LASER_TEXTURE);
        this.solidAtlasSprite = atlas.getSprite(LaserRenderState.SOLID_TEXTURE);
    }
}
