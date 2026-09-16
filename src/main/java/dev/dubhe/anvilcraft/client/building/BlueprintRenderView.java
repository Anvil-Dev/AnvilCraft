package dev.dubhe.anvilcraft.client.building;

import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nullable;

/**
 * 投影用的邻接查询视图:只保存变换后局部坐标上的方块与方块实体,
 * 供面剔除和流体连接使用。不加入真实世界,也不跑方块刻。
 * 坐标空间与 {@link net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate}
 * 变换后的局部坐标一致,可能为负,因此不沿用真实世界的建筑高度裁剪。
 */
final class BlueprintRenderView implements BlockAndTintGetter {
    private static final int AXIS = StructureSnapshotCodec.MAX_AXIS;

    private final ClientLevel level;
    private final BlockPos tintPos;
    private final Long2ObjectOpenHashMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<BlockEntity> blockEntities = new Long2ObjectOpenHashMap<>();
    private boolean hideNonOccludingNeighbors = true;

    BlueprintRenderView(ClientLevel level, BlockPos tintPos) {
        this.level = level;
        this.tintPos = tintPos.immutable();
    }

    void put(BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        long key = pos.asLong();
        this.blocks.put(key, state);
        if (blockEntity != null) {
            this.blockEntities.put(key, blockEntity);
        }
    }

    void hideNonOccludingNeighbors(boolean hide) {
        this.hideNonOccludingNeighbors = hide;
    }

    BlockState realState(BlockPos pos) {
        BlockState state = this.blocks.get(pos.asLong());
        return state != null ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        BlockState state = this.realState(pos);
        // 流体连接高度读邻格 getBlockState().getFluidState();把熔岩/含水方块藏成空气会画出阶梯和内部水面。
        // 红石粉 canOcclude 为真但不是实心立方体,会切掉下方顶面;邻接查询只把 isSolidRender 立方体当真。
        if (this.hideNonOccludingNeighbors
            && state.getFluidState().isEmpty()
            && !state.isSolidRender(EmptyBlockGetter.INSTANCE, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    /** 把查询原点挪到 {@code origin},供 {@code renderLiquid} 在 (0,0,0) 写 0-1 顶点。 */
    BlockAndTintGetter shifted(BlockPos origin) {
        return new Shifted(this, origin.immutable());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.realState(pos).getFluidState();
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.blockEntities.get(pos.asLong());
    }

    @Override
    public int getHeight() {
        return AXIS * 2;
    }

    @Override
    public int getMinBuildHeight() {
        return -AXIS;
    }

    @Override
    public boolean isOutsideBuildHeight(int y) {
        return false;
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return this.level.getShade(direction, shade);
    }

    @Override
    public float getShade(float normalX, float normalY, float normalZ, boolean shade) {
        return this.level.getShade(normalX, normalY, normalZ, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
        Holder<Biome> plains = this.level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
        return colorResolver.getColor(plains.value(), this.tintPos.getX(), this.tintPos.getZ());
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return 15;
    }

    @Override
    public ModelData getModelData(BlockPos pos) {
        BlockEntity blockEntity = this.blockEntities.get(pos.asLong());
        return blockEntity != null ? blockEntity.getModelData() : ModelData.EMPTY;
    }

    private record Shifted(BlueprintRenderView view, BlockPos origin) implements BlockAndTintGetter {
        private BlockPos map(BlockPos pos) {
            return pos.offset(this.origin);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return this.view.realState(this.map(pos));
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return this.view.getFluidState(this.map(pos));
        }

        @Override
        @Nullable
        public BlockEntity getBlockEntity(BlockPos pos) {
            return this.view.getBlockEntity(this.map(pos));
        }

        @Override
        public int getHeight() {
            return this.view.getHeight();
        }

        @Override
        public int getMinBuildHeight() {
            return this.view.getMinBuildHeight();
        }

        @Override
        public boolean isOutsideBuildHeight(int y) {
            return false;
        }

        @Override
        public float getShade(Direction direction, boolean shade) {
            return this.view.getShade(direction, shade);
        }

        @Override
        public float getShade(float normalX, float normalY, float normalZ, boolean shade) {
            return this.view.getShade(normalX, normalY, normalZ, shade);
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return this.view.getLightEngine();
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
            return this.view.getBlockTint(this.map(pos), colorResolver);
        }

        @Override
        public int getBrightness(LightLayer type, BlockPos pos) {
            return 15;
        }

        @Override
        public int getRawBrightness(BlockPos pos, int amount) {
            return 15;
        }

        @Override
        public ModelData getModelData(BlockPos pos) {
            return this.view.getModelData(this.map(pos));
        }
    }
}
