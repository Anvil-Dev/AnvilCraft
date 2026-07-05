package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.LargeLaserBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum LargeLaserBlockProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        BlockState state = accessor.getBlockState();
        if (!(state.getBlock() instanceof LargeLaserBlock largeLaserBlock)) return;
        if (largeLaserBlock.isMainPart(state)) return;
        BlockPos mainPos = largeLaserBlock.getMainPartPos(accessor.getPosition(), state);
        BlockEntity be = accessor.getLevel().getBlockEntity(mainPos);
        if (be instanceof IPowerComponent powerComponent) {
            PowerGrid grid = powerComponent.getGrid();
            if (grid != null) {
                tag.putInt("generate", grid.getGenerate());
                tag.putInt("consume", grid.getConsume());
                tag.putBoolean("infinitePower", grid.isHasInfinitePower());
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("large_laser");
    }
}
