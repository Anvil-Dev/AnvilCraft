package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.util.CompatUtil;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LargeCauldronTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity value) {
        return value instanceof LargeCauldronBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        if (CompatUtil.HAS_JADE.get() && AnvilCraftClient.CONFIG.doNotShowTooltipWhenJadePresent) return List.of();
        LargeCauldronBlockEntity cauldron = ((LargeCauldronBlockEntity) value).getMainPart();
        List<LargeCauldronBlockEntity.RecipePreview> previews = cauldron.getRecipePreviews();
        List<Component> lines = new ArrayList<>();

        lines.add(heading("tooltip.anvilcraft.large_cauldron.inputs"));
        for (int slot = 0; slot < cauldron.getInputHandler().getSlots(); slot++) {
            ItemStack stack = cauldron.getInputHandler().getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            lines.add(itemLine(stack, categoriesForSlot(previews, slot)));
        }

        lines.add(heading("tooltip.anvilcraft.large_cauldron.outputs"));
        for (ItemStack stack : aggregateOutputs(cauldron)) {
            lines.add(itemLine(stack, Set.of()));
        }

        lines.add(heading("tooltip.anvilcraft.large_cauldron.fluids"));
        boolean topLayer = true;
        for (int tank = cauldron.getFluids().getTanks() - 1; tank >= 0; tank--) {
            FluidStack fluid = cauldron.getFluids().getFluidInTank(tank);
            if (fluid.isEmpty()) continue;
            lines.add(fluidLine(fluid, categoriesForFluid(previews, fluid), topLayer && cauldron.isIgnited()));
            topLayer = false;
        }
        return lines;
    }

    private static Component heading(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.BLUE);
    }

    private static Component itemLine(ItemStack stack, Set<String> categories) {
        MutableComponent line = Component.empty()
            .append(stack.getHoverName())
            .append(Component.literal(" x" + stack.getCount()))
            .withStyle(ChatFormatting.GRAY);
        appendRecipeSuffix(line, categories);
        return ITooltipProvider.withIndentAndMerge(line);
    }

    private static Component fluidLine(FluidStack fluid, Set<String> categories, boolean burning) {
        MutableComponent line = Component.empty()
            .append(fluid.getHoverName())
            .append(Component.literal(" " + UnitUtil.fluidUnit(fluid.getAmount(), false)))
            .withStyle(ChatFormatting.GRAY);
        if (burning) {
            line.append(Component.translatable(
                "tooltip.anvilcraft.large_cauldron.burning"
            ).withStyle(ChatFormatting.RED));
        }
        appendRecipeSuffix(line, categories);
        return ITooltipProvider.withIndentAndMerge(line);
    }

    private static void appendRecipeSuffix(MutableComponent line, Set<String> categories) {
        if (categories.isEmpty()) return;
        MutableComponent names = Component.empty();
        boolean first = true;
        for (String category : categories) {
            if (!first) names.append(Component.literal(" / "));
            names.append(Component.translatable("gui.anvilcraft.category." + category));
            first = false;
        }
        line.append(Component.translatable(
            "tooltip.anvilcraft.large_cauldron.will_process",
            names
        ).withStyle(ChatFormatting.GOLD));
    }

    private static Set<String> categoriesForSlot(
        List<LargeCauldronBlockEntity.RecipePreview> previews,
        int slot
    ) {
        Set<String> result = new LinkedHashSet<>();
        for (LargeCauldronBlockEntity.RecipePreview preview : previews) {
            if (preview.inputSlot() == slot) result.add(preview.categoryPath());
        }
        return result;
    }

    private static Set<String> categoriesForFluid(
        List<LargeCauldronBlockEntity.RecipePreview> previews,
        FluidStack fluid
    ) {
        Set<String> result = new LinkedHashSet<>();
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        for (LargeCauldronBlockEntity.RecipePreview preview : previews) {
            for (HasCauldron predicate : preview.fluidPredicates()) {
                if (predicate.hasCheck() && predicate.matchesFluid(fluidId)) {
                    result.add(preview.categoryPath());
                    break;
                }
            }
        }
        return result;
    }

    private static List<ItemStack> aggregateOutputs(LargeCauldronBlockEntity cauldron) {
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < cauldron.getOutputHandler().getSlots(); slot++) {
            ItemStack stack = cauldron.getOutputHandler().getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            ItemStack existing = null;
            for (ItemStack candidate : result) {
                if (ItemStack.isSameItemSameComponents(candidate, stack)) {
                    existing = candidate;
                    break;
                }
            }
            if (existing == null) {
                result.add(stack.copy());
            } else {
                existing.grow(stack.getCount());
            }
        }
        return result;
    }

    @Override
    public int priority() {
        return -1;
    }
}
