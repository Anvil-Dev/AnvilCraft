package dev.dubhe.anvilcraft.block.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import static dev.dubhe.anvilcraft.api.sliding.SlidingBlockStructureResolver.MAX_PUSH_DEPTH;

public class DetectorSlidingRailBlockEntity extends BlockEntity {
    @Getter
    private int power = 0;

    public DetectorSlidingRailBlockEntity(
        BlockEntityType<?> type, BlockPos pos,
        BlockState blockState
    ) {
        super(type, pos, blockState);
    }

    public void cleanPower() {
        this.updatePower(0);
    }

    public void updatePower(int blockCount) {
        if (MAX_PUSH_DEPTH <= 15) {
            this.power = blockCount;
            return;
        }
        this.power = blockCount / MAX_PUSH_DEPTH;
        if (this.power < 1 && blockCount > 0) {
            this.power = 1;
        }
    }
}
