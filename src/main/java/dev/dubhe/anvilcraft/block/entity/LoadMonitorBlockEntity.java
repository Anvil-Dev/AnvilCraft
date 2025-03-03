package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.LoadMonitorBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

public class LoadMonitorBlockEntity extends BlockEntity implements IPowerConsumer {
    @Getter
    @Setter
    private PowerGrid grid;

    private int cooldown = 0;

    public LoadMonitorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        super.loadAdditional(tag, provider);
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        super.saveAdditional(tag, provider);
        cooldown = tag.getInt("Cooldown");
    }

    @Override
    public Level getCurrentLevel() {
        return getLevel();
    }

    @Override
    public @NotNull BlockPos getPos() {
        return getBlockPos();
    }

    /**
     *
     */
    public int getRedstoneSignal() {
        if (getGrid() == null) return 0;
        // 空载
        if (getGrid().getConsume() == 0) return 0;
        // 满载
        if (getGrid().getConsume() > getGrid().getGenerate()) return 0;
        return (int) Math.ceil(((double) getGrid().getConsume() / getGrid().getGenerate()) * 15);
    }

    /**
     *
     */
    public void tick() {
        if (cooldown > 0) {
            cooldown--;
        } else {
            if (getGrid() == null) return;
            flushState(getLevel(), getBlockPos());
            // 满载
            if (getGrid().getConsume() > getGrid().getGenerate()) return;
            int load = getGrid().getConsume() != 0
                ? (int) Math.ceil(
                (double) getGrid().getConsume() / getGrid().getGenerate() * 10)
                : 0;
            BlockState state = getBlockState().setValue(LoadMonitorBlock.LOAD, load);
            getLevel().setBlockAndUpdate(getBlockPos(), state);
            cooldown = AnvilCraft.config.loadMonitor;
            getLevel().updateNeighbourForOutputSignal(getBlockPos(), state.getBlock());
        }
    }
}
