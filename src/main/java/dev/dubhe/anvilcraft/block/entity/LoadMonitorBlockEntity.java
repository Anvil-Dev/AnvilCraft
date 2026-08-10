package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.power.LoadMonitorBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class LoadMonitorBlockEntity extends BlockEntity implements IPowerConsumer {
    @Getter
    @Setter
    @Nullable
    private PowerGrid grid;

    private int cooldown = 0;

    public LoadMonitorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.cooldown = input.getIntOr("Cooldown", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Cooldown", this.cooldown);
    }

    @Override
    public Level getCurrentLevel() {
        return Objects.requireNonNull(this.getLevel());
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    public int getRedstoneSignal() {
        if (this.getGrid() == null) return 0;
        // 空载
        if (this.getGrid().getConsume() == 0) return 0;
        // 满载
        if (this.getGrid().getConsume() > this.getGrid().getGenerate()) return 0;
        return (int) Math.ceil(((double) this.getGrid().getConsume() / this.getGrid().getGenerate()) * 15);
    }

    public void tick() {
        if (this.cooldown > 0) {
            this.cooldown--;
        } else {
            PowerGrid grid = this.getGrid();
            Level level = this.getLevel();
            if (grid == null || level == null) return;
            this.flushState(level, this.getBlockPos());
            // 满载
            if (grid.getConsume() > grid.getGenerate()) return;
            int load = grid.getConsume() != 0
                ? (int) Math.ceil(
                (double) grid.getConsume() / grid.getGenerate() * 10)
                : 0;
            BlockState state = this.getBlockState().setValue(LoadMonitorBlock.LOAD, load);
            level.setBlockAndUpdate(this.getBlockPos(), state);
            this.cooldown = AnvilCraft.CONFIG.loadMonitor;
            level.updateNeighbourForOutputSignal(this.getBlockPos(), state.getBlock());
        }
    }
}
