package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum MultiPartPowerBlockProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        BlockState state = accessor.getBlockState();
        if (!(state.getBlock() instanceof AbstractMultiPartBlock<?> multiPartBlock)) return;
        PowerGrid grid = findPowerGrid(multiPartBlock, accessor.getLevel(), accessor.getPosition(), state);
        if (grid == null) return;
        tag.putInt("generate", grid.getGenerate());
        tag.putInt("consume", grid.getConsume());
        tag.putBoolean("infinitePower", grid.isHasInfinitePower());
    }

    private static <P extends Enum<P>> @Nullable PowerGrid findPowerGrid(
        AbstractMultiPartBlock<P> block,
        Level level,
        BlockPos pos,
        BlockState state
    ) {
        BlockPos mainPos = block.getMainPartPos(pos, state);
        PowerGrid grid = getPowerGrid(level.getBlockEntity(mainPos));
        if (grid != null) return grid;

        for (P part : block.getParts()) {
            BlockPos partPos = pos.offset(block.offsetFrom(state, part));
            if (partPos.equals(mainPos) || !level.getBlockState(partPos).is(block)) continue;
            grid = getPowerGrid(level.getBlockEntity(partPos));
            if (grid != null) return grid;
        }
        return null;
    }

    private static @Nullable PowerGrid getPowerGrid(@Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof IPowerComponent powerComponent) {
            return powerComponent.getGrid();
        }
        return null;
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("multipart_power");
    }
}
