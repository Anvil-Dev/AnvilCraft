package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.util.ColorUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class AnvilRailgunItem extends Item {
    private static final int FULL_BAR_COLOR = 0xFF5454FF;
    private static final int BAR_COLOR = 0x7087FFFF;
    public static final int MAX_ENERGY = 640000000;

    public AnvilRailgunItem(Properties properties) {
        super(properties.component(ModComponents.STORED_ENERGY, AnvilRailgunItem.MAX_ENERGY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int energy = stack.getOrDefault(ModComponents.STORED_ENERGY, 0);
        return Math.round(Math.clamp((float) energy / AnvilRailgunItem.MAX_ENERGY, 0, 1) * 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float energy = stack.getOrDefault(ModComponents.STORED_ENERGY, 0);
        return ColorUtil.lerpColor(energy / AnvilRailgunItem.MAX_ENERGY, BAR_COLOR, FULL_BAR_COLOR);
    }
}
