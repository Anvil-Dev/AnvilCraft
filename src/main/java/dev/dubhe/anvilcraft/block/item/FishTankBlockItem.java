package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class FishTankBlockItem extends BlockItem implements Equipable {
    public FishTankBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, LivingEntity entity) {
        return armorType == EquipmentSlot.HEAD;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;
        ItemStack headSlot = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!headSlot.getItem().equals(ModBlocks.FISH_TANK.asItem()) || player.isInWater()) return;
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 601, 0, false, false, true));
    }
}
