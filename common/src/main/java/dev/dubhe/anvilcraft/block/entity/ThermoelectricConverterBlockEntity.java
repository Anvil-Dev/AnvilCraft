package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.chargecollector.ThermoManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import static dev.dubhe.anvilcraft.block.ThermoelectricConverterBlock.DIRECTIONS;

public class ThermoelectricConverterBlockEntity extends BlockEntity {

    private boolean created = false;

    public ThermoelectricConverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * tick
     */
    public void tick() {
        if (!created && level != null) {
            for (Direction direction : DIRECTIONS) {
                BlockPos neighborPos = getBlockPos().relative(direction);
                ThermoManager.getInstance(level).addThermoBlock(neighborPos, level.getBlockState(neighborPos));
            }
            created = true;
        }
    }
}
