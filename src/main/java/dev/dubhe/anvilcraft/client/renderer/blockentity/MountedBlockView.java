package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.math.OctahedralGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.model.data.ModelData;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/** 模型以底座所在位置为原点查询完整尺寸的部件，在缩放后的世界位置采样光照和群系。 */
final class MountedBlockView implements BlockAndTintGetter {
    private final Sources sources;
    private final CardinalLighting lighting;
    private final boolean sampleSurroundings;
    private final MountedBlockView shaded;
    private final MountedBlockView unshaded;

    MountedBlockView(BlockAndTintGetter parts, Map<BlockPos, BlockState> states, @Nullable BlockAndTintGetter host,
                     BlockPos origin, Direction facing, int packedLight, Matrix4fc modelToButton) {
        this.sources = new Sources(parts, states, host, origin.immutable(), packedLight, modelToButton);
        var rotation = rotation(facing);
        var worldLighting = host == null ? CardinalLighting.DEFAULT : host.cardinalLighting();
        this.lighting = rotation == OctahedralGroup.IDENTITY ? worldLighting : new CardinalLighting(
            worldLighting.byFace(rotation.rotate(Direction.DOWN)), worldLighting.byFace(rotation.rotate(Direction.UP)),
            worldLighting.byFace(rotation.rotate(Direction.NORTH)), worldLighting.byFace(rotation.rotate(Direction.SOUTH)),
            worldLighting.byFace(rotation.rotate(Direction.WEST)), worldLighting.byFace(rotation.rotate(Direction.EAST))
        );
        this.sampleSurroundings = false;
        this.shaded = host == null ? this : new MountedBlockView(this.sources, this.lighting);
        // 原版用维度的上表面亮度表示无方向阴影，安装在墙上时也要保留这个值。
        this.unshaded = this.lighting.up() == worldLighting.up() ? this.shaded : new MountedBlockView(this.sources, worldLighting);
    }

    private MountedBlockView(Sources sources, CardinalLighting lighting) {
        this.sources = sources;
        this.lighting = lighting;
        this.sampleSurroundings = true;
        this.shaded = this;
        this.unshaded = this;
    }

    static OctahedralGroup rotation(Direction facing) {
        return switch (facing) {
            case UP -> OctahedralGroup.IDENTITY;
            case DOWN -> OctahedralGroup.BLOCK_ROT_X_180;
            case NORTH -> OctahedralGroup.BLOCK_ROT_X_90;
            case SOUTH -> OctahedralGroup.BLOCK_ROT_Y_180.compose(OctahedralGroup.BLOCK_ROT_X_90);
            case EAST -> OctahedralGroup.BLOCK_ROT_Y_90.compose(OctahedralGroup.BLOCK_ROT_X_90);
            case WEST -> OctahedralGroup.BLOCK_ROT_Y_270.compose(OctahedralGroup.BLOCK_ROT_X_90);
        };
    }

    BlockAndTintGetter forLighting(boolean shade) {
        return shade ? this.shaded : this.unshaded;
    }

    @Nullable Holder<Biome> biomeAt(BlockPos pos) {
        return this.sources.host instanceof LevelReader world ? world.getBiome(this.sources.worldPos(pos)) : null;
    }

    boolean hasHost() {
        return this.sources.host != null;
    }

    @Override
    public CardinalLighting cardinalLighting() {
        return this.lighting;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        var state = this.sources.states.get(pos);
        if (state != null) {
            return state;
        }
        // 四边形的剔面只取决于相连的部件，邻墙仍可在缩小模型上形成环境遮蔽。
        if (this.sampleSurroundings && this.sources.host != null) {
            var sample = this.sources.worldPos(pos);
            if (!sample.equals(this.sources.origin)) {
                return this.sources.host.getBlockState(sample);
            }
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return this.sources.parts.getBlockEntity(pos);
    }

    @Override
    public ModelData getModelData(BlockPos pos) {
        return this.sources.parts.getModelData(pos);
    }

    @Override
    public @Nullable AuxiliaryLightManager getAuxLightManager(ChunkPos pos) {
        return this.sources.parts.getAuxLightManager(pos);
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        var host = this.sources.host;
        if (host != null) {
            return host.getBlockTint(this.sources.worldPos(pos), resolver);
        }
        if (this.sources.parts == BlockAndTintGetter.EMPTY) {
            var client = Minecraft.getInstance();
            if (client != null && client.level != null) {
                var plains = client.level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
                return resolver.getColor(plains.value(), pos.getX(), pos.getZ());
            }
        }
        return this.sources.parts.getBlockTint(pos, resolver);
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        var host = this.sources.host;
        if (host != null) {
            return host.getBrightness(layer, this.sources.worldPos(pos));
        }
        return layer == LightLayer.SKY ? LightCoordsUtil.sky(this.sources.packedLight) : LightCoordsUtil.block(this.sources.packedLight);
    }

    @Override
    public int getRawBrightness(BlockPos pos, int darkening) {
        var host = this.sources.host;
        if (host != null) {
            return host.getRawBrightness(this.sources.worldPos(pos), darkening);
        }
        return Math.max(LightCoordsUtil.block(this.sources.packedLight), LightCoordsUtil.sky(this.sources.packedLight) - darkening);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.sources.parts.getLightEngine();
    }

    @Override
    public int getMinY() {
        var host = this.sources.host;
        return host == null ? DimensionType.MIN_Y : host.getMinY();
    }

    @Override
    public int getHeight() {
        var host = this.sources.host;
        return host == null ? DimensionType.Y_SIZE : host.getHeight();
    }

    private record Sources(BlockAndTintGetter parts, Map<BlockPos, BlockState> states, @Nullable BlockAndTintGetter host,
                           BlockPos origin, int packedLight, Matrix4fc modelToButton) {
        BlockPos worldPos(BlockPos pos) {
            double x = pos.getX() - this.origin.getX() + 0.5;
            double y = pos.getY() - this.origin.getY() + 0.5;
            double z = pos.getZ() - this.origin.getZ() + 0.5;
            var m = this.modelToButton;
            return BlockPos.containing(
                this.origin.getX() + (m.m00() * x + m.m10() * y + m.m20() * z + m.m30()),
                this.origin.getY() + (m.m01() * x + m.m11() * y + m.m21() * z + m.m31()),
                this.origin.getZ() + (m.m02() * x + m.m12() * y + m.m22() * z + m.m32())
            );
        }
    }
}
