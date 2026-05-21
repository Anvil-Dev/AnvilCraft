package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public class FishTankBlockItem extends BlockItem {
    public FishTankBlockItem(Block block, Properties properties) {
        super(block, properties.equippable(EquipmentSlot.HEAD));
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, LivingEntity entity) {
        return armorType == EquipmentSlot.HEAD;
    }

    @Override
    public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(itemStack, level, owner, slot);
        if (level.isClientSide()) return;
        if (!(owner instanceof Player player)) return;
        ItemStack headSlot = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!headSlot.getItem().equals(ModBlocks.FISH_TANK.asItem()) || player.isInWater()) return;
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 601, 0, false, false));
    }
}
