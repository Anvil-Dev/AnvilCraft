package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.IntUnaryOperator;

/** 根据经验修补效果消耗液态经验，容器可以按毫桶扣除，世界方块可以整桶扣除。 */
public final class ExperienceMending {
    private ExperienceMending() {
    }

    public static boolean repair(ItemStack stack, ServerLevel level, int availableFluid, IntUnaryOperator consumeFluid) {
        int experience = AnvilCraft.CONFIG.world.expFluidXpPerBlock;
        if (experience <= 0 || availableFluid <= 0 || !stack.isDamaged()
            || !EnchantmentHelper.has(stack, EnchantmentEffectComponents.REPAIR_WITH_XP)) {
            return false;
        }
        int durabilityPerBucket = EnchantmentHelper.modifyDurabilityToRepairFromXp(
            level, stack, (int) (experience * stack.getXpRepairRatio())
        );
        if (durabilityPerBucket <= 0) {
            return false;
        }
        long bucketVolume = (long) FluidType.BUCKET_VOLUME * stack.getCount();
        int repair = (int) Math.min(stack.getDamageValue(), (long) availableFluid * durabilityPerBucket / bucketVolume);
        if (repair <= 0) {
            return false;
        }
        int requiredFluid = (int) ((repair * bucketVolume + durabilityPerBucket - 1) / durabilityPerBucket);
        int consumed = consumeFluid.applyAsInt(requiredFluid);
        int repaired = (int) Math.min(stack.getDamageValue(), (long) consumed * durabilityPerBucket / bucketVolume);
        if (repaired <= 0) {
            return false;
        }
        stack.setDamageValue(stack.getDamageValue() - repaired);
        return true;
    }
}
