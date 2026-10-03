package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class FireReforgingUtil {

    private FireReforgingUtil() {
    }

    public static boolean repair(ItemStack stack, int amount, Level level, @Nullable BlockPos pos) {
        if (stack.isEmpty() || !stack.has(ModComponents.FIRE_REFORGING) || !stack.isDamaged()) return false;
        stack.setDamageValue(stack.getDamageValue() - amount);
        if (pos != null) TriggerUtil.fireReforge(level, pos);
        return true;
    }
}
