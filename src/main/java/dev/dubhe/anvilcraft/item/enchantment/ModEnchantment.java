package dev.dubhe.anvilcraft.item.enchantment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import java.util.ArrayList;
import java.util.List;

public abstract class ModEnchantment extends Enchantment {
    protected static final List<BlockPos> OFFSET = new ArrayList<>() {
        {
            this.add(new BlockPos(-1, -1, -1));
            this.add(new BlockPos(-1, -1, 1));
            this.add(new BlockPos(-1, 1, -1));
            this.add(new BlockPos(-1, 1, 1));
            this.add(new BlockPos(1, -1, -1));
            this.add(new BlockPos(1, -1, 1));
            this.add(new BlockPos(1, 1, -1));
            this.add(new BlockPos(1, 1, 1));
            this.add(new BlockPos(0, 0, -1));
            this.add(new BlockPos(0, 0, 1));
            this.add(new BlockPos(0, -1, 0));
            this.add(new BlockPos(0, 1, 0));
            this.add(new BlockPos(-1, 0, 0));
            this.add(new BlockPos(1, 0, 0));
            this.add(new BlockPos(-1, -1, 0));
            this.add(new BlockPos(-1, 1, 0));
            this.add(new BlockPos(-1, 0, -1));
            this.add(new BlockPos(-1, 0, 1));
            this.add(new BlockPos(0, -1, -1));
            this.add(new BlockPos(0, -1, 1));
            this.add(new BlockPos(0, 1, -1));
            this.add(new BlockPos(0, 1, 1));
            this.add(new BlockPos(1, -1, 0));
            this.add(new BlockPos(1, 1, 0));
            this.add(new BlockPos(1, 0, -1));
            this.add(new BlockPos(1, 0, 1));
        }
    };

    protected ModEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] applicableSlots) {
        super(rarity, category, applicableSlots);
    }
}
