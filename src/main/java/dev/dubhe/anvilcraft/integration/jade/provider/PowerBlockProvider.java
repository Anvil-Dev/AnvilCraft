package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum PowerBlockProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final Identifier UID = AnvilCraft.of("power_provider");

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        Level level = blockAccessor.getLevel();
        BlockPos pos = blockAccessor.getHitResult().getBlockPos();
        BlockState state = blockAccessor.getBlockState();
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> multiblock) {
            pos = multiblock.getMainPartPos(pos, state);
        }
        if (level.getBlockEntity(pos) instanceof IPowerComponent blockEntity) {
            PowerGrid powerGrid = blockEntity.getGrid();
            if (powerGrid == null) {
                return;
            }
            compoundTag.putInt("generate", powerGrid.getGenerate());
            compoundTag.putInt("consume", powerGrid.getConsume());
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
