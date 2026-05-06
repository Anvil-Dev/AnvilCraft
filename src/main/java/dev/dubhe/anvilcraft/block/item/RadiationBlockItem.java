package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.item.abnormal.IRadiation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public class RadiationBlockItem extends BlockItem implements IRadiation {
    public RadiationBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(itemStack, level, owner, slot);
        IRadiation.super.inventoryTick(itemStack, level, owner, slot);
    }
}
