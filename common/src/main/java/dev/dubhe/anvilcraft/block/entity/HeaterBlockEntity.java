package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class HeaterBlockEntity extends BlockEntity implements IPowerConsumer {
    private static final int POWER = 8;
    private PowerGrid grid = null;

    public HeaterBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.HEATER.get(), pos, blockState);
    }

    public static @NotNull HeaterBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new HeaterBlockEntity(type, pos, blockState);
    }

    private HeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public int getInputPower() {
        return HeaterBlockEntity.POWER;
    }

    @Override
    public @NotNull BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
    }

    /**
     * @param level 世界
     * @param pos   位置
     */
    public void tick(@NotNull Level level, @NotNull BlockPos pos) {
        this.flushState(level, pos);
    }

    @Override
    public Level getCurrentLevel() {
        return this.getLevel();
    }
}
