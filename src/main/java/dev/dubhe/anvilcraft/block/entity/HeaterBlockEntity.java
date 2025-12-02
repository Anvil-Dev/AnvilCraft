package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Getter
public class HeaterBlockEntity extends BlockEntity implements IPowerConsumer {
    private static final int POWER = 16;
    private PowerGrid grid = null;

    public HeaterBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.HEATER.get(), pos, blockState);
    }

    private HeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static HeaterBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new HeaterBlockEntity(type, pos, blockState);
    }

    @Override
    public int getInputPower() {
        return HeaterBlockEntity.POWER;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
    }

    public void tick(Level level, BlockPos pos) {
        this.flushState(level, pos);
        HeaterManager.addProducer(pos, level, ModHeaterInfos.HEATER);
    }

    @Override
    public Level getCurrentLevel() {
        return Objects.requireNonNull(this.getLevel());
    }
}
