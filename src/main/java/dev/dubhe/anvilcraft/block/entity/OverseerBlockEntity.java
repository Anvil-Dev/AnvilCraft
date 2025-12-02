package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import dev.dubhe.anvilcraft.api.world.load.LoadChuckData;
import dev.dubhe.anvilcraft.block.OverseerBlock;
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
import org.jetbrains.annotations.NotNull;

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

    public static @NotNull OverseerBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new OverseerBlockEntity(type, pos, blockState);
    }

    /**
     * tick 逻辑
     *
     * @param level 世界
     * @param pos   坐标
     * @param state 方块状态
     */
    @SuppressWarnings("unused")
    public void tick(Level level, @NotNull BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            // 如果底座上方不是监督者，直接破坏底座，结束方法
            if (!isBaseValid()) {
                if (LevelLoadManager.checkRegistered(pos)) {
                    LevelLoadManager.unregister(pos, level);
                }
                return;
            }
            int newlevel = checkBaseSupportsLevel(level, pos);
            boolean newRandomTick = this.waterLoggedBlockCount >= 4;
            if (newlevel == oldlevel && newRandomTick == oldRandomTick) {
                return;
            }
            if (oldlevel > -1 || LevelLoadManager.checkRegistered(pos)) {
                LevelLoadManager.unregister(pos, level);
                oldlevel = -1;
                oldRandomTick = false;
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
            oldlevel = newlevel;
            oldRandomTick = newRandomTick;
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
            int baseT = checkBaseAt(level, pos);
            if (baseT == -1) break;
            waterLoggedBlockCount += baseT;
            supportLevel++;
            pos.move(Direction.DOWN);
        }
        this.waterLoggedBlockCount = waterLoggedBlockCount;
        return supportLevel;
    }

    private boolean isBaseValid() {
        BlockPos thizPos = getBlockPos();
        if (!checkBlocks()) return false;
        int supportsLevel = checkBaseSupportsLevel(this.level, thizPos);
        for (int i = 0; i < 3; i++) {
            BlockPos pos = getBlockPos().relative(Direction.Axis.Y, i);
            BlockState state = level.getBlockState(pos);
            if (level.getBlockState(pos).is(ModBlocks.OVERSEER_BLOCK)) {
                level.setBlock(pos, state.setValue(OverseerBlock.LEVEL, supportsLevel), 2);
            }
        }
        return supportsLevel >= 0;
    }

    private boolean checkBlocks() {
        for (int i = 0; i < 3; i++) {
            BlockPos pos = getBlockPos().relative(Direction.Axis.Y, i);
            if (!level.getBlockState(pos).is(ModBlocks.OVERSEER_BLOCK)) {
                return false;
            }
        }
        return true;
    }
}
