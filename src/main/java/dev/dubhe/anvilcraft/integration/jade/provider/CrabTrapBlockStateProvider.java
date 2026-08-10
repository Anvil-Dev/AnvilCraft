package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.production.CrabTrapBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum CrabTrapBlockStateProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final Identifier UID = AnvilCraft.of("crab_trap");

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        BlockState blockState = blockAccessor.getBlockState();
        if (blockState.is(ModBlocks.CRAB_TRAP)) {
            int count = blockState.getValue(CrabTrapBlock.FISHING);
            compoundTag.putInt("fishing", count);
        }
    }

    @Override
    public Identifier getUid() {
        return CrabTrapBlockStateProvider.UID;
    }
}
