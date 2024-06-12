package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import lombok.Getter;

@Getter
public abstract class BaseMachineBlockEntity extends BlockEntity implements MenuProvider {

    protected BaseMachineBlockEntity(
            BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public abstract Direction getDirection();

    public abstract void setDirection(Direction direction);
}
