package dev.dubhe.anvilcraft.recipe;

import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.LiquidEnchantmentUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class LiquidEnchantmentCauldronRecipe {
    private LiquidEnchantmentCauldronRecipe() {
    }

    /** 匹配并模拟一次液态魔咒特殊反应，调用方确认输出空间后再提交结果。 */
    public static Optional<Result> match(List<FluidStack> fluids, ItemStack item, boolean heated) {
        if (item.is(Items.LAPIS_LAZULI)) {
            Optional<Result> assimilation = heated ? assimilate(fluids) : Optional.empty();
            return assimilation.isPresent() ? assimilation : createBlank(fluids, item);
        }
        if (item.is(ModItemTags.SILVER_NUGGETS)) return cleanse(fluids);
        if (item.is(Items.GOLD_INGOT) && item.getCount() >= 16) {
            return curseGold(fluids, 1, new ItemStack(ModItems.CURSED_GOLD_INGOT.get(), 16));
        }
        if (item.is(Items.GOLD_BLOCK) && item.getCount() >= 16) {
            return curseGold(fluids, 9, new ItemStack(ModBlocks.CURSED_GOLD_BLOCK, 16));
        }
        return Optional.empty();
    }

    private static Optional<Result> createBlank(List<FluidStack> fluids, ItemStack item) {
        if (item.getCount() < 3) return Optional.empty();
        FluidStack experience = find(fluids, fluid -> fluid.is(ModFluids.EXP_FLUID.get()), 2000);
        if (experience.isEmpty()) return Optional.empty();

        LargeCauldronFluidHandler simulated = copyHandler(fluids);
        if (!drain(simulated, experience, 2000)) return Optional.empty();
        FluidStack blank = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 1);
        if (!fill(simulated, blank)) return Optional.empty();
        return Optional.of(new Result(simulated.copyFluids(), 3, ItemStack.EMPTY, false));
    }

    private static Optional<Result> assimilate(List<FluidStack> fluids) {
        FluidStack blank = find(fluids, LiquidEnchantmentUtil::isBlank, 1);
        FluidStack enchanted = find(fluids, LiquidEnchantmentUtil::isEnchanted, 8);
        if (blank.isEmpty() || enchanted.isEmpty()) return Optional.empty();

        LargeCauldronFluidHandler simulated = copyHandler(fluids);
        if (!drain(simulated, blank, 1) || !drain(simulated, enchanted, 8)) return Optional.empty();
        FluidStack result = enchanted.copyWithAmount(9);
        if (!fill(simulated, result)) return Optional.empty();
        return Optional.of(new Result(simulated.copyFluids(), 1, ItemStack.EMPTY, true));
    }

    private static Optional<Result> cleanse(List<FluidStack> fluids) {
        FluidStack enchanted = find(fluids, LiquidEnchantmentUtil::isEnchanted, 8);
        if (enchanted.isEmpty()) return Optional.empty();

        LargeCauldronFluidHandler simulated = copyHandler(fluids);
        if (!drain(simulated, enchanted, 8)) return Optional.empty();
        FluidStack blank = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 8);
        if (!fill(simulated, blank)) return Optional.empty();
        return Optional.of(new Result(simulated.copyFluids(), 1, ItemStack.EMPTY, false));
    }

    private static Optional<Result> curseGold(List<FluidStack> fluids, int amount, ItemStack result) {
        FluidStack cursed = find(fluids, LiquidEnchantmentUtil::isCursed, amount);
        if (cursed.isEmpty()) return Optional.empty();

        LargeCauldronFluidHandler simulated = copyHandler(fluids);
        if (!drain(simulated, cursed, amount)) return Optional.empty();
        return Optional.of(new Result(simulated.copyFluids(), 16, result, false));
    }

    private static FluidStack find(List<FluidStack> fluids, Predicate<FluidStack> predicate, int amount) {
        for (FluidStack fluid : fluids) {
            if (fluid.getAmount() >= amount && predicate.test(fluid)) return fluid;
        }
        return FluidStack.EMPTY;
    }

    private static LargeCauldronFluidHandler copyHandler(List<FluidStack> fluids) {
        LargeCauldronFluidHandler handler = new LargeCauldronFluidHandler(() -> {});
        handler.setFluids(fluids);
        return handler;
    }

    private static boolean drain(LargeCauldronFluidHandler handler, FluidStack fluid, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(FluidResource.of(fluid), amount, transaction);
            if (extracted != amount) return false;
            transaction.commit();
            return true;
        }
    }

    private static boolean fill(LargeCauldronFluidHandler handler, FluidStack fluid) {
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(FluidResource.of(fluid), fluid.getAmount(), transaction);
            if (inserted != fluid.getAmount()) return false;
            transaction.commit();
            return true;
        }
    }

    public record Result(List<FluidStack> fluids, int itemCost, ItemStack itemResult, boolean consumesHeat) {
        public Result {
            fluids = fluids.stream().map(FluidStack::copy).toList();
            itemResult = itemResult.copy();
        }
    }
}
