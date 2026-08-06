package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import dev.dubhe.anvilcraft.api.world.load.LoadChunkData;
import dev.dubhe.anvilcraft.block.utility.OverseerBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class OverseerBlockEntity extends BlockEntity {
    private int oldLevel = -1;
    private int oldTierHash = 0;

    private static final int[] TIER_RADIUS = {1, 2, 3, 4};

    @SuppressWarnings("unchecked")
    private static final TagKey<Block>[] VALID_BASE_TAGS = new TagKey[]{
        ModBlockTags.OVERSEER_BASE_TIER_0,
        ModBlockTags.OVERSEER_BASE_TIER_1,
        ModBlockTags.OVERSEER_BASE_TIER_2,
        ModBlockTags.OVERSEER_BASE_TIER_3
    };

    public OverseerBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.OVERSEER.get(), pos, blockState);
    }

    private OverseerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static OverseerBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new OverseerBlockEntity(type, pos, blockState);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (!this.checkOverseerBlocks()) {
            if (LevelLoadManager.checkRegistered(pos)) {
                LevelLoadManager.unregister(pos, level);
            }
            return;
        }

        BaseScanResult result = this.scanPyramidBase(level, pos);
        int newLevel = result.completeTiers;
        int newTierHash = hashTierMappings(result.offsetMappings);
        boolean levelChanged = newLevel != this.oldLevel;
        boolean tiersChanged = newTierHash != this.oldTierHash;

        if (!levelChanged && !tiersChanged) return;

        if (levelChanged) {
            this.updateDisplayedLevel(Math.max(newLevel, 0));
        }

        if (this.oldLevel > -1 || LevelLoadManager.checkRegistered(pos)) {
            LevelLoadManager.unregister(pos, level);
            this.oldLevel = -1;
            this.oldTierHash = 0;
        }

        LevelLoadManager.register(
            pos,
            LoadChunkData.createLoadChunkData(
                Math.max(newLevel, 0),
                pos,
                serverLevel,
                result.offsetMappings),
            serverLevel);

        this.oldLevel = newLevel;
        this.oldTierHash = newTierHash;
    }

    private boolean isTierComplete(Level level, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos current = center.mutable().move(dx, 0, dz);
                if (!isValidBaseBlock(level.getBlockState(current))) {
                    return false;
                }
            }
        }
        return true;
    }

    private BaseScanResult scanPyramidBase(Level level, BlockPos selfPos) {
        int completeTiers = 0;
        List<LoadChunkData.BlockOffsetMapping> offsetMappings = new ArrayList<>();
        BlockPos.MutableBlockPos tierCenter = selfPos.mutable().move(Direction.DOWN);

        for (int tier = 0; tier < TIER_RADIUS.length; tier++) {
            int radius = TIER_RADIUS[tier];
            if (!isTierComplete(level, tierCenter, radius)) break;

            completeTiers++;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos blockPos = tierCenter.mutable().move(dx, 0, dz);
                    BlockState state = level.getBlockState(blockPos);
                    int blockTier = getBlockTier(state);

                    int sourceFlags = getBlockSourceFlags(state);

                    if (blockTier >= 1) {
                        offsetMappings.add(new LoadChunkData.BlockOffsetMapping(dx, dz, blockTier, sourceFlags));
                    }
                }
            }
            tierCenter.move(Direction.DOWN);
        }

        return new BaseScanResult(completeTiers, offsetMappings);
    }

    private int getBlockSourceFlags(BlockState state) {
        if (state.is(ModBlocks.TRANSCENDIUM_BLOCK)) {
            return LoadChunkData.SourceFlags.TRANSCENDIUM;
        }
        if (state.is(ModBlocks.MULTIPHASE_MATTER_BLOCK)) {
            return LoadChunkData.SourceFlags.MULTIPHASE;
        }
        if (state.is(Blocks.NETHERITE_BLOCK) || state.is(ModBlockTags.EMBER_SERIES)) {
            return LoadChunkData.SourceFlags.FIRE;
        }
        if (state.is(ModBlockTags.FROST_SERIES)) {
            return LoadChunkData.SourceFlags.FROST;
        }

        return LoadChunkData.SourceFlags.DEFAULT;
    }

    private boolean isValidBaseBlock(BlockState state) {
        for (TagKey<Block> tag : VALID_BASE_TAGS) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    private int getBlockTier(BlockState state) {
        for (int i = 0; i < VALID_BASE_TAGS.length; i++) {
            if (state.is(VALID_BASE_TAGS[i])) return i;
        }
        return -1;
    }

    private int hashTierMappings(List<LoadChunkData.BlockOffsetMapping> mappings) {
        int hash = 0;
        for (LoadChunkData.BlockOffsetMapping m : mappings) {
            hash = hash * 31 + m.chunkOffsetX();
            hash = hash * 31 + m.chunkOffsetZ();
            hash = hash * 31 + m.tier();
            hash = hash * 31 + m.sourceFlags();
        }
        return hash;
    }

    private boolean checkOverseerBlocks() {
        Level level = this.level;
        if (level == null) return false;
        for (int i = 0; i < 3; i++) {
            BlockPos pos = this.getBlockPos().relative(Direction.Axis.Y, i);
            if (!level.getBlockState(pos).is(ModBlocks.OVERSEER)) {
                return false;
            }
        }
        return true;
    }

    private void updateDisplayedLevel(int levelValue) {
        Level level = this.level;
        if (level == null) return;
        for (int i = 0; i < 3; i++) {
            BlockPos pos = this.getBlockPos().relative(Direction.Axis.Y, i);
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.OVERSEER) && state.getValue(OverseerBlock.LEVEL) != levelValue) {
                level.setBlock(pos, state.setValue(OverseerBlock.LEVEL, levelValue), 2);
            }
        }
    }

    public int getLoadLevel() {
        return this.oldLevel;
    }

    private record BaseScanResult(int completeTiers, List<LoadChunkData.BlockOffsetMapping> offsetMappings) {}
}