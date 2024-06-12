package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import dev.dubhe.anvilcraft.api.world.load.LoadChuckData;
import dev.dubhe.anvilcraft.block.OverseerBlock;
import dev.dubhe.anvilcraft.block.state.Vertical3PartHalf;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class OverseerBlockEntity extends BlockEntity {
    private int waterLoggerBlockNum = 0;
    private int lastWaterLoggerBlockNum = 0;

    public OverseerBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.OVERSEER.get(), pos, blockState);
    }

    public static @NotNull OverseerBlockEntity createBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
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
            if (checkBase(level, pos, state)) {
                return;
            }
            BlockState updatedState = level.getBlockState(pos);
            if (state == updatedState && this.waterLoggerBlockNum == this.lastWaterLoggerBlockNum) {
                return;
            }
            this.lastWaterLoggerBlockNum = this.waterLoggerBlockNum;
            LevelLoadManager.register(
                    pos,
                    LoadChuckData.creatLoadChuckData(
                            updatedState.getValue(OverseerBlock.LEVEL),
                            pos,
                            (this.waterLoggerBlockNum >= 4),
                            serverLevel),
                    serverLevel);
        }
    }

    /**
     * @return 是否破坏了底座
     */
    private boolean checkBase(Level level, @NotNull BlockPos pos, BlockState state) {
        int waterLoggerBlockNum = 0;
        for (int laminar = 0; laminar < 4; laminar++) {
            level.setBlockAndUpdate(pos, state.setValue(OverseerBlock.LEVEL, laminar));
            // 如果第二格第三格不是监督者，破坏底座
            for (BlockPos blockPos : Arrays.asList(pos.above(), pos.above(2))) {
                if (level.getBlockState(blockPos).is(ModBlocks.OVERSEER_BLOCK.get())) {
                    continue;
                }
                // 破坏监督者方块
                level.destroyBlock(pos, false);
                return true;
            }
            level.setBlockAndUpdate(
                    pos.above(),
                    state
                            .setValue(OverseerBlock.LEVEL, laminar)
                            .setValue(OverseerBlock.HALF, Vertical3PartHalf.MID));
            level.setBlockAndUpdate(
                    pos.above(2),
                    state
                            .setValue(OverseerBlock.LEVEL, laminar)
                            .setValue(OverseerBlock.HALF, Vertical3PartHalf.TOP));
            for (int x = pos.getX() - 1; x <= pos.getX() + 1; x++) {
                for (int z = pos.getZ() - 1; z <= pos.getZ() + 1; z++) {
                    BlockPos basePos = new BlockPos(x, pos.getY() - laminar - 1, z);
                    if (!level.getBlockState(basePos).is(ModBlockTags.OVERSEER_BASE)) {
                        this.waterLoggerBlockNum = waterLoggerBlockNum;
                        return false;
                    }
                    if (level.getBlockState(basePos).hasProperty(BlockStateProperties.WATERLOGGED)
                            && level.getBlockState(basePos).getValue(BlockStateProperties.WATERLOGGED))
                        waterLoggerBlockNum++;
                }
            }
        }
        this.waterLoggerBlockNum = waterLoggerBlockNum;
        return false;
    }
}
