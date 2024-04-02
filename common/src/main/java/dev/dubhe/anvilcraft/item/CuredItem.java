package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CuredItem extends Item {
    public CuredItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (entity instanceof Player player) {
            MobEffectInstance weakness = new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, true);
            MobEffectInstance slowness = new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1, false, true);
            MobEffectInstance hungry = new MobEffectInstance(MobEffects.HUNGER, 200, 1, false, true);
            player.addEffect((weakness));
            int curedNumber = player.getInventory().countItem(ModItems.CURSED_GOLD_INGOT.get()) + player.getInventory().countItem(ModItems.CURSED_GOLD_NUGGET.get()) + player.getInventory().countItem(ModBlocks.CURSED_GOLD_BLOCK.asItem());
            if (curedNumber>8){
                player.addEffect((slowness));
            }
            if (curedNumber>64){
                player.addEffect((hungry));
            }
        }
    }
}
