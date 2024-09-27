package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import dev.dubhe.anvilcraft.api.world.load.LoadChuckData;
import dev.dubhe.anvilcraft.block.OverseerBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;

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

    public OverseerBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.OVERSEER.get(), pos, blockState);
    }

    public static @NotNull OverseerBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new OverseerBlockEntity(type, pos, blockState);
    }

    private OverseerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
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
                    LevelLoadManager.unregister(pos, (ServerLevel) level);
                }
                return;
            }
            BlockState updatedState = level.getBlockState(pos);
            if (!LevelLoadManager.checkRegistered(pos)) {
                LevelLoadManager.register(
                    pos,
                    LoadChuckData.createLoadChuckData(
                        updatedState.getValue(OverseerBlock.LEVEL),
                        pos,
                        (this.waterLoggedBlockCount >= 4),
                        serverLevel),
                    serverLevel);
            }
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
                if (currentState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    waterLogged++;
                }
            }
        }
        return waterLogged;
    }

    private int checkBaseSupportsLevel(Level level, BlockPos selfPos) {
        int supportLevel = 0;
        int waterLoggedBlockCount = 0;
        BlockPos.MutableBlockPos pos = selfPos.mutable();
        pos.move(Direction.DOWN);
        int tBase = checkBaseAt(level, pos);
        if (tBase == -1) {
            return 0;
        }
        waterLoggedBlockCount += tBase;
        supportLevel++;

        pos.move(Direction.DOWN);
        tBase = checkBaseAt(level, pos);
        if (tBase != -1) {
            waterLoggedBlockCount += tBase;
            supportLevel++;
        }

        pos.move(Direction.DOWN);
        tBase = checkBaseAt(level, pos);
        if (tBase != -1) {
            waterLoggedBlockCount += tBase;
            supportLevel++;
        }
        this.waterLoggedBlockCount = waterLoggedBlockCount;
        return supportLevel;
    }

    private boolean isBaseValid() {
        BlockPos thizPos = getBlockPos();
        if (!checkBlocks()) return false;
        int supportsLevel = checkBaseSupportsLevel(level, thizPos);
        for (int i = 0; i < 3; i++) {
            BlockPos pos = getBlockPos().relative(Direction.Axis.Y, i);
            BlockState state = level.getBlockState(pos);
            if (level.getBlockState(pos).is(ModBlocks.OVERSEER_BLOCK)) {
                level.setBlockAndUpdate(pos, state.setValue(OverseerBlock.LEVEL, supportsLevel));
            }
        }
        return supportsLevel > 0;
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
