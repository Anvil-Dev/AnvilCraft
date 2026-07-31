package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import dev.dubhe.anvilcraft.api.world.load.LoadChuckData;
import dev.dubhe.anvilcraft.block.utility.OverseerBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class OverseerBlockEntity extends BlockEntity {
    private int waterLoggedBlockCount = 0;
    private int oldlevel = -1;
    private boolean oldRandomTick = false;

    public OverseerBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.OVERSEER.get(), pos, blockState);
    }

    private OverseerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static OverseerBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new OverseerBlockEntity(type, pos, blockState);
    }

    /// tick 逻辑
    ///
    /// @param level 世界
    /// @param pos   坐标
    /// @param state 方块状态
    @SuppressWarnings("unused")
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            // 如果底座上方不是监督者，直接破坏底座，结束方法
            if (!this.checkBlocks()) {
                if (LevelLoadManager.checkRegistered(pos)) {
                    LevelLoadManager.unregister(pos, level);
                }
                return;
            }
            int newlevel = this.checkBaseSupportsLevel(level, pos);
            boolean newRandomTick = this.waterLoggedBlockCount >= 4;
            boolean levelChanged = newlevel != this.oldlevel;
            if (!levelChanged && newRandomTick == this.oldRandomTick) {
                return;
            }
            if (levelChanged) this.updateDisplayedLevel(newlevel);
            if (this.oldlevel > -1 || LevelLoadManager.checkRegistered(pos)) {
                LevelLoadManager.unregister(pos, level);
                this.oldlevel = -1;
                this.oldRandomTick = false;
            }
            if (newlevel >= 0) {
                LevelLoadManager.register(
                    pos,
                    LoadChuckData.createLoadChuckData(
                        newlevel,
                        pos,
                        (this.waterLoggedBlockCount >= 4),
                        serverLevel),
                    serverLevel);
            }
            this.oldlevel = newlevel;
            this.oldRandomTick = newRandomTick;
        }
    }

    private int checkBaseAt(Level level, BlockPos pos) {
        int waterLogged = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos current = pos.mutable().move(dx, 0, dz);
                BlockState currentState = level.getBlockState(current);
                if (!currentState.is(ModBlockTags.OVERSEER_BASE)) {
                    return -1;
                }
                if (currentState.hasProperty(BlockStateProperties.WATERLOGGED)
                    && currentState.getValue(BlockStateProperties.WATERLOGGED)) {
                    waterLogged++;
                }
            }
        }
        return waterLogged;
    }

    private int checkBaseSupportsLevel(Level level, BlockPos selfPos) {
        int supportLevel = 0;
        int waterLoggedBlockCount = 0;
        BlockPos.MutableBlockPos pos = selfPos.mutable().move(Direction.DOWN);
        for (int i = 0; i < 3; i++) {
            int baseT = this.checkBaseAt(level, pos);
            if (baseT == -1) break;
            waterLoggedBlockCount += baseT;
            supportLevel++;
            pos.move(Direction.DOWN);
        }
        this.waterLoggedBlockCount = waterLoggedBlockCount;
        return supportLevel;
    }

    private void updateDisplayedLevel(int levelValue) {
        Level level = this.level;
        if (level == null) return;
        for (int i = 0; i < 3; i++) {
            BlockPos pos = this.getBlockPos().relative(Direction.Axis.Y, i);
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.OVERSEER)
                && state.getValue(OverseerBlock.LEVEL) != levelValue) {
                level.setBlock(pos, state.setValue(OverseerBlock.LEVEL, levelValue), 2);
            }
        }
    }

    private boolean checkBlocks() {
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
}
