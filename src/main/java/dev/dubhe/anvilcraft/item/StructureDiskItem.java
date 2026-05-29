package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 结构磁盘物品
 * 在tooltip中显示保存的结构名称
 */
public class StructureDiskItem extends Item {

    public StructureDiskItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        // 从 NBT 中读取结构信息并显示在 tooltip 中
        StructureDiskData structureDiskData = stack.get(ModComponents.STRUCTURE_DISK_DATA);
        if (structureDiskData != null) {
            tooltipComponents.add(Component.translatable("item.anvilcraft.structure_disk.structure", structureDiskData.name()));
            int sizeX = structureDiskData.sizeX();
            int sizeY = structureDiskData.sizeY();
            int sizeZ = structureDiskData.sizeZ();
            String sizeText = sizeX + " x " + sizeY + " x " + sizeZ;
            tooltipComponents.add(Component.translatable("item.anvilcraft.structure_disk.size", sizeText));

            // 检查结构是否超过5x5x5
            if (sizeX <= 5 && sizeY <= 5 && sizeZ <= 5) {
                tooltipComponents.add(Component.translatable("item.anvilcraft.structure_disk.fit_placer")
                    .withStyle(net.minecraft.ChatFormatting.GREEN));
            } else {
                tooltipComponents.add(Component.translatable("item.anvilcraft.structure_disk.too_large_for_placer")
                    .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
    }
}
