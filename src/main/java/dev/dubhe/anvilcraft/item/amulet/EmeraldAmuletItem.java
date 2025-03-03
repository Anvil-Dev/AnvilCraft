package dev.dubhe.anvilcraft.item.amulet;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import static dev.dubhe.anvilcraft.init.ModDataAttachments.DISCOUNT_RATE;

public class EmeraldAmuletItem extends AbstractAmuletItem {
    public EmeraldAmuletItem(Properties properties) {
        super(properties);
    }

    @Override
    void UpdateAccessory(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player) {
            //如果要直接加村庄英雄：player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 1, 2, false, false));
            player.setData(DISCOUNT_RATE, 0.3f);
        }
    }
}
