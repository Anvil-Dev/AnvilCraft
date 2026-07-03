package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CrabTrapBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum CrabTrapBlockStateProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig pluginConfig) {
        CompoundTag serverData = blockAccessor.getServerData();
        if (serverData.contains("finishing")) {
            int finishing = serverData.getInt("finishing");
            tooltip.add(Component.translatable("tooltip.anvilcraft.crab_trap.jade.finishing", finishing));
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        BlockState blockState = blockAccessor.getBlockState();
        if (blockState.is(ModBlocks.CRAB_TRAP)) {
            int count = blockState.getValue(CrabTrapBlock.FINISHING);
            compoundTag.putInt("finishing", count);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("crab_trap");
    }
}