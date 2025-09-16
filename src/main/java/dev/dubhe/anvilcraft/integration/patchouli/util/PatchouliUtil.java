package dev.dubhe.anvilcraft.integration.patchouli.util;

import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import vazkii.patchouli.api.PatchouliAPI;

public class PatchouliUtil {
    public static void openBook(ServerPlayer player) {
        PatchouliAPI.get().openBookGUI(player, AnvilCraft.of("guide"));
    }

    public static ItemStack getStack(ChanceItemStack stack) {
        ItemStack itemStack = stack.stack().copy();
        if (stack.count() instanceof ConstantValue) {
            itemStack.setCount(stack.getMaxCount());
        } else if (stack.count() instanceof BinomialDistributionGenerator count) {
            if (count.p() instanceof ConstantValue(float value) && value == 1) itemStack.setCount(stack.getMaxCount());
        }
        return itemStack;
    }
}
