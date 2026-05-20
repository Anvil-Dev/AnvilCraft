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
        
        // 从 NBT 中读取结构名称并显示在 tooltip 中
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("StructureName")) {
                String structureName = tag.getString("StructureName");
                tooltipComponents.add(Component.literal("Structure: ").append(Component.literal(structureName)));
            }
        }
    }
}
