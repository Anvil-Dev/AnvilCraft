package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.api.tooltip.StoragePortItemTooltip;
import dev.dubhe.anvilcraft.client.renderer.item.CustomRenderItemClientExtension;
import dev.dubhe.anvilcraft.client.renderer.item.StoragePortItemRenderer;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.Optional;
import java.util.function.Consumer;

public class StoragePortBlockItem extends BlockItem {
    public StoragePortBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return StoragePortItemTooltip.storagePortTooltipImage(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(CustomRenderItemClientExtension.of(StoragePortItemRenderer.getInstance()));
    }
}
