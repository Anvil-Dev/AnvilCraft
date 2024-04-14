package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class CreativeDynamoBlockEntity extends BlockEntity implements IPowerProducer {
    @Getter
    private PowerGrid grid = null;
    private int power = 16;

    public static @NotNull CreativeDynamoBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new CreativeDynamoBlockEntity(type, pos, blockState);
    }

    public CreativeDynamoBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CREATIVE_DYNAMO.get(), pos, blockState);
    }

    private CreativeDynamoBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("power", power);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.power = tag.getInt("power");
    }

    @Override
    public int getOutputPower() {
        return this.power;
    }

    @Override
    public @NotNull BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public void setGrid(PowerGrid grid) {
        this.grid = grid;
    }
}
