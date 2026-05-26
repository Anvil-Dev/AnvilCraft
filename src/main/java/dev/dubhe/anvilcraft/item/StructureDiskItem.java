package dev.dubhe.anvilcraft.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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
    public void appendHoverText(
        ItemStack stack,
        Item.TooltipContext context,
        List<Component> tooltipComponents,
        TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        // 从 NBT 中读取结构信息并显示在 tooltip 中
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("StructureName")) {
                String structureName = tag.getString("StructureName");
                tooltipComponents.add(Component.translatable("item.anvilcraft.structure_disk.structure", structureName));
            }
            
            // 显示结构大小
            if (tag.contains("SizeX") && tag.contains("SizeY") && tag.contains("SizeZ")) {
                int sizeX = tag.getInt("SizeX");
                int sizeY = tag.getInt("SizeY");
                int sizeZ = tag.getInt("SizeZ");
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
}
