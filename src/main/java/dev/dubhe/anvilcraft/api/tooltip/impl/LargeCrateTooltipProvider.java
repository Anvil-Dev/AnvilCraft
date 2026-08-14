package dev.dubhe.anvilcraft.api.tooltip.impl;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.storage.LargeCrateBlockEntity;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class LargeCrateTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity value) {
        if (
            Minecraft.getInstance().player == null
            || !Minecraft.getInstance().player.getMainHandItem().is(ModItemTags.ANVIL_HAMMER)
               && !Minecraft.getInstance().player.getOffhandItem().is(ModItemTags.ANVIL_HAMMER)
        ) {
            return false;
        }
        return value instanceof LargeCrateBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        if (
            Minecraft.getInstance().player == null
            || !Minecraft.getInstance().player.getMainHandItem().is(ModItemTags.ANVIL_HAMMER)
            && !Minecraft.getInstance().player.getOffhandItem().is(ModItemTags.ANVIL_HAMMER)
        ) {
            return ImmutableList.of();
        }
        return ImmutableList.of(
            Component.translatable("tooltip.anvilcraft.large_crate.0"),
            Component.translatable("tooltip.anvilcraft.large_crate.1"),
            Component.translatable("tooltip.anvilcraft.large_crate.2"),
            Component.translatable("tooltip.anvilcraft.large_crate.3")
        );
    }

    @Override
    public int priority() {
        return 0;
    }
}
