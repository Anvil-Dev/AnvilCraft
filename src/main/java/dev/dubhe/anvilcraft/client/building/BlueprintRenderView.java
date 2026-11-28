package dev.dubhe.anvilcraft.client.building;

import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.model.data.ModelData;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 为模型面剔除和流体连接提供投影内的方块状态。蓝图使用结构局部坐标，
 * 材料预览使用世界坐标，以便模型取得目标位置的颜色与随机变体。
 */
public final class BlueprintRenderView implements BlockAndTintGetter {
    private static final int AXIS = StructureSnapshotCodec.MAX_AXIS;

    private final ClientLevel level;
    private final boolean placed;
    private final BlockPos tintPos;
    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    private final Long2ObjectOpenHashMap<BlockEntity> blockEntities = new Long2ObjectOpenHashMap<>();

    private final Map<BlockPos, BoundingBox> units;
    @Nullable private BoundingBox visibleUnit;
    private boolean separateFluids;

    BlueprintRenderView(ClientLevel level, BlockPos tintPos, Map<BlockPos, BoundingBox> units) {
        this(level, tintPos, units, false);
    }

    BlueprintRenderView(ClientLevel level, BlockPos tintPos, Map<BlockPos, BoundingBox> units, boolean placed) {
        this.placed = placed;
        this.units = units;
        this.level = level;
        this.tintPos = tintPos.immutable();
    }

    void put(BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        this.blocks.put(pos, state);
        if (blockEntity != null) {
            this.blockEntities.put(pos.asLong(), blockEntity);
        }
    }

    Iterable<BlockPos> positions() {
        return this.blocks.keySet();
    }

    void selectUnit(BlockPos pos) {
        this.visibleUnit = this.units.get(pos);
    }

    int coveredFaces(BlockPos pos, BlockState state) {
        int covered = 0;
        // 查询地形本来的面剔除结果时，暂时关闭缩放单元之间的补面。
        var unit = this.visibleUnit;
        this.visibleUnit = null;
        try {
            for (var face : Direction.values()) {
                var neighbor = pos.relative(face);
                if (this.units.get(neighbor) != unit
                    && !Block.shouldRenderFace(this, pos, state, this.level.getBlockState(neighbor), face)) {
                    covered |= 1 << face.ordinal();
                }
            }
        } finally {
            this.visibleUnit = unit;
        }
        return covered;
    }

    void separateFluids(boolean separate) {
        this.separateFluids = separate;
    }

    public boolean exposesFace(BlockPos pos, Direction face) {
        return this.placed && this.visibleUnit != null && this.units.get(pos.relative(face)) != this.visibleUnit;
    }

    // 分开缩放的单元之间留有空隙，只有同一单元的部件可以参与面剔除和流体连接。
    private boolean isVisible(BlockPos pos) {
        return this.visibleUnit == null || this.units.get(pos) == this.visibleUnit;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (this.placed) {
            return this.separateFluids && !this.isVisible(pos) ? Blocks.AIR.defaultBlockState() : this.level.getBlockState(pos);
        }
        BlockState state = this.isVisible(pos) ? this.blocks.get(pos) : null;
        return state != null ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public ModelData getModelData(BlockPos pos) {
        if (this.placed) {
            return this.level.getModelData(pos);
        }
        var entity = this.getBlockEntity(pos);
        return entity == null ? ModelData.EMPTY : entity.getModelData();
    }

    @Override
    public @Nullable AuxiliaryLightManager getAuxLightManager(ChunkPos pos) {
        return this.placed ? this.level.getAuxLightManager(pos) : null;
    }

    /** 把查询原点挪到 {@code origin},供 {@code renderLiquid} 在 (0,0,0) 写 0-1 顶点。 */
    BlockAndTintGetter shifted(BlockPos origin) {
        return new Shifted(this, origin.immutable());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos) {
        if (this.placed) {
            return this.level.getBlockEntity(pos);
        }
        return this.isVisible(pos) ? this.blockEntities.get(pos.asLong()) : null;
    }

    @Override
    public int getHeight() {
        return this.units.isEmpty() ? AXIS * 2 : this.level.getHeight();
    }

    @Override
    public int getMinY() {
        return this.units.isEmpty() ? -AXIS : this.level.getMinY();
    }

    @Override
    public boolean isOutsideBuildHeight(int y) {
        return !this.units.isEmpty() && this.level.isOutsideBuildHeight(y);
    }

    @Override
    public CardinalLighting cardinalLighting() {
        return this.level.cardinalLighting();
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
        if (!this.units.isEmpty()) {
            return this.level.getBlockTint(pos, colorResolver);
        }
        Holder<Biome> plains = this.level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        return colorResolver.getColor(plains.value(), this.tintPos.getX(), this.tintPos.getZ());
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos) {
        return this.placed ? this.level.getBrightness(type, pos) : 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return this.placed ? this.level.getRawBrightness(pos, amount) : 15;
    }

    private record Shifted(BlueprintRenderView view, BlockPos origin) implements BlockAndTintGetter {
        private BlockPos map(BlockPos pos) {
            return pos.offset(this.origin);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return this.view.getBlockState(this.map(pos));
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
        public int getMinY() {
            return this.view.units.isEmpty() ? this.view.getMinY() : this.view.getMinY() - this.origin.getY();
        }

        @Override
        public boolean isOutsideBuildHeight(int y) {
            return this.view.isOutsideBuildHeight(this.origin.getY() + y);
        }

        @Override
        public CardinalLighting cardinalLighting() {
            return this.view.cardinalLighting();
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

    }
}
