package dev.dubhe.anvilcraft.integration.jei.util;

import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import dev.dubhe.anvilcraft.util.FluidStackPredicate;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.List;

/**
 * 共享的 JEI 流体成分工具，将流体内容渲染为格子而非大锅方块。
 */
public final class JeiFluidUtil {
    private static final String SLOT_PREFIX = JeiBlockIngredientUtil.PREVIEW_SLOT_PREFIX + "fluid/";

    private JeiFluidUtil() {
    }

    /**
     * 存在物品时流体位置向下偏移
     */
    public static void addFluidInputSlot(IRecipeLayoutBuilder builder, String name, int width, int height, HasCauldronSimple cauldron) {
        addInputSlot(builder, name, JeiSlotUtil.INPUT_X, JeiSlotUtil.FLUID_Y, width, height, cauldron);
    }

    /**
     * 默认的居中位置
     */
    public static void addDefaultInputSlot(IRecipeLayoutBuilder builder, String name, int width, int height, HasCauldronSimple cauldron) {
        addInputSlot(builder, name, JeiSlotUtil.INPUT_X, JeiSlotUtil.DEFAULT_Y, width, height, cauldron);
    }

    public static void addInputSlot(
        IRecipeLayoutBuilder builder,
        String name,
        int x,
        int y,
        int width,
        int height,
        HasCauldronSimple cauldron
    ) {
        addSlot(
            builder, RecipeIngredientRole.INPUT, name, x, y, width, height,
            getFluids(cauldron.fluid()), cauldron.consume(), 1.0f
        );
    }

    /**
     * 存在物品时流体位置向下偏移
     */
    public static void addFluidOutputSlot(IRecipeLayoutBuilder builder, String name, int width, int height, HasCauldronSimple cauldron) {
        addOutputSlot(builder, name, JeiSlotUtil.OUTPUT_X, JeiSlotUtil.FLUID_Y, width, height, cauldron);
    }

    /**
     * 默认的居中位置
     */
    public static void addDefaultOutputSlot(IRecipeLayoutBuilder builder, String name, int width, int height, HasCauldronSimple cauldron) {
        addOutputSlot(builder, name, JeiSlotUtil.OUTPUT_X, JeiSlotUtil.DEFAULT_Y, width, height, cauldron);
    }

    public static void addOutputSlot(
        IRecipeLayoutBuilder builder,
        String name,
        int x,
        int y,
        int width,
        int height,
        HasCauldronSimple cauldron
    ) {
        addSlot(
            builder, RecipeIngredientRole.OUTPUT, name, x, y, width, height,
            cauldron.transform().map(JeiFluidUtil::getFluids).orElseGet(List::of),
            cauldron.produce(),
            cauldron.chance()
        );
    }

    private static void addSlot(
        IRecipeLayoutBuilder builder,
        RecipeIngredientRole role,
        String name,
        int x,
        int y,
        int width,
        int height,
        List<Fluid> fluids,
        int amount,
        float chance
    ) {
        if (fluids.isEmpty()) return;
        long displayAmount = amount > 0 ? amount : FluidType.BUCKET_VOLUME;
        IRecipeSlotBuilder slot = builder.addSlot(role, x, y)
            .setSlotName(SLOT_PREFIX + name)
            .setFluidRenderer(displayAmount, false, width, height);
        fluids.forEach(fluid -> slot.addFluidStack(fluid, displayAmount));
        if (chance < 1.0f) {
            slot.addRichTooltipCallback((slotView, tooltip) ->
                tooltip.addAll(JeiRecipeUtil.getTooltips(ConstantValue.exactly(chance))));
        }
        addBucketIngredients(builder, role, fluids);
    }

    private static void addBucketIngredients(
        IRecipeLayoutBuilder builder,
        RecipeIngredientRole role,
        List<Fluid> fluids
    ) {
        List<ItemStack> buckets = fluids.stream()
            .map(Fluid::getBucket)
            .filter(bucket -> bucket != Items.AIR)
            .distinct()
            .map(ItemStack::new)
            .toList();
        if (!buckets.isEmpty()) {
            builder.addInvisibleIngredients(role).addItemStacks(buckets);
        }
    }

    private static List<Fluid> getFluids(FluidStackPredicate predicate) {
        return predicate.fluids().stream()
            .flatMap(HolderSet::stream)
            .map(Holder::value)
            .filter(value -> value.defaultFluidState().isSource())
            .distinct()
            .toList();
    }

    private static List<Fluid> getFluids(FluidStack fluid) {
        if (!fluid.getFluid().defaultFluidState().isSource()) return List.of();
        return List.of(fluid.getFluid());
    }

    public static void suppressHoverOverlays(IRecipeExtrasBuilder builder) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

}
