package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.tooltip.providers.IItemTooltipProvider;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * 结构磁盘物品
 * 在tooltip中显示保存的结构名称
 */
public class StructureDiskItem extends Item implements IItemTooltipProvider {

    public StructureDiskItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendItemTooltip(
        ItemStack stack,
        TooltipContext context,
        TooltipDisplay display,
        Consumer<Component> builder,
        TooltipFlag tooltipFlag
    ) {
        // 从 NBT 中读取结构信息并显示在 tooltip 中
        StructureDiskData structureDiskData = stack.get(ModComponents.STRUCTURE_DISK_DATA);
        if (structureDiskData != null) {
            builder.accept(Component.translatable("item.anvilcraft.structure_disk.structure", structureDiskData.name()));
            int sizeX = structureDiskData.sizeX();
            int sizeY = structureDiskData.sizeY();
            int sizeZ = structureDiskData.sizeZ();
            String sizeText = sizeX + " x " + sizeY + " x " + sizeZ;
            builder.accept(Component.translatable("item.anvilcraft.structure_disk.size", sizeText));

            // 检查结构是否超过5x5x5
            if (sizeX <= 5 && sizeY <= 5 && sizeZ <= 5) {
                builder.accept(Component.translatable("item.anvilcraft.structure_disk.fit_placer")
                    .withStyle(ChatFormatting.GREEN));
            } else {
                builder.accept(Component.translatable("item.anvilcraft.structure_disk.too_large_for_placer")
                    .withStyle(ChatFormatting.RED));
            }
        }
    }
}
