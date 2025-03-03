package dev.dubhe.anvilcraft.item.amulet;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import static dev.dubhe.anvilcraft.init.ModDataAttachments.IMMUNE_TO_LIGHTNING;

public class TopazAmuletItem extends AbstractAmuletItem {
    public TopazAmuletItem(Properties properties) {
        super(properties);
    }

    @Override
    void UpdateAccessory(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        //这个可能主要是从另一边（实体被闪电击中）的事件监听效果那边写的，同样用的是data attachment
        if (entity instanceof LivingEntity) {
            entity.setData(IMMUNE_TO_LIGHTNING, true);
        }
    }
}
