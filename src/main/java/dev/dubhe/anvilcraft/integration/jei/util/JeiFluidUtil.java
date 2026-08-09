package dev.dubhe.anvilcraft.integration.jei.util;

import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import dev.dubhe.anvilcraft.util.DataComponentPredicate;
import dev.dubhe.anvilcraft.util.FluidStackPredicate;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPatch;
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
            getDisplayFluids(cauldron.fluid(), cauldron.consume()), false, 1.0f
        );
    }

    /**
     * 存在物品时流体位置向下偏移
     */
    public static void addFluidOutputSlots(IRecipeLayoutBuilder builder, String name, int width, int height, HasCauldronSimple cauldron) {
        addOutputSlots(builder, name, JeiSlotUtil.OUTPUT_X, JeiSlotUtil.FLUID_Y, width, height, cauldron);
    }

    /**
     * 默认的居中位置
     */
    public static void addDefaultOutputSlots(IRecipeLayoutBuilder builder, String name, int width, int height, HasCauldronSimple cauldron) {
        addOutputSlots(builder, name, JeiSlotUtil.OUTPUT_X, JeiSlotUtil.DEFAULT_Y, width, height, cauldron);
    }

    public static void addOutputSlots(
        IRecipeLayoutBuilder builder,
        String name,
        int x,
        int y,
        int width,
        int height,
        HasCauldronSimple cauldron
    ) {
        List<FluidStack> transforms = cauldron.transforms();
        if (transforms.isEmpty()) return;
        int cols = (int) Math.ceil(Math.sqrt(transforms.size()));
        int rows = Math.ceilDiv(transforms.size(), cols);
        int startX = x - (cols - 1) * JeiSlotUtil.OFFSET / 2;
        int startY = y - (rows - 1) * JeiSlotUtil.OFFSET / 2;
        for (int index = 0; index < transforms.size(); index++) {
            FluidStack transform = transforms.get(index);
            addSlot(
                builder,
                RecipeIngredientRole.OUTPUT,
                transforms.size() == 1 ? name : name + "/" + index,
                startX + index % cols * JeiSlotUtil.OFFSET,
                startY + index / cols * JeiSlotUtil.OFFSET,
                width,
                height,
                getDisplayFluids(transform, transform.getAmount()),
                true,
                cauldron.chance()
            );
        }
    }

    private static void addSlot(
        IRecipeLayoutBuilder builder,
        RecipeIngredientRole role,
        String name,
        int x,
        int y,
        int width,
        int height,
        List<FluidStack> fluids,
        boolean showCapacity,
        float chance
    ) {
        if (fluids.isEmpty()) return;
        IRecipeSlotBuilder slot = addFluidSlot(
            builder, role, x, y, width, height, fluids.getFirst().getAmount(), showCapacity, fluids
        )
            .setSlotName(SLOT_PREFIX + name);
        if (chance < 1.0f) {
            slot.addRichTooltipCallback((slotView, tooltip) ->
                                            tooltip.addAll(JeiRecipeUtil.getTooltips(ConstantValue.exactly(chance))));
        }
    }

    public static IRecipeSlotBuilder addFluidSlot(
        IRecipeLayoutBuilder builder,
        RecipeIngredientRole role,
        int x,
        int y,
        int width,
        int height,
        long capacity,
        boolean showCapacity,
        List<FluidStack> fluids
    ) {
        IRecipeSlotBuilder slot = builder.addSlot(role, x, y)
            .setFluidRenderer(capacity, showCapacity, width, height);
        fluids.forEach(fluid -> slot.addFluidStack(
            fluid.getFluid(),
            fluid.getAmount(),
            fluid.getComponentsPatch()
        ));
        List<ItemStack> buckets = fluids.stream()
            .map(FluidStack::getFluid)
            .map(Fluid::getBucket)
            .filter(bucket -> bucket != Items.AIR)
            .distinct()
            .map(ItemStack::new)
            .toList();
        if (!buckets.isEmpty()) {
            builder.addInvisibleIngredients(role).addItemStacks(buckets);
        }
        return slot;
    }

    public static List<FluidStack> getDisplayFluids(FluidStackPredicate predicate, int amount) {
        int displayAmount = amount > 0 ? amount : FluidType.BUCKET_VOLUME;
        DataComponentPatch components = predicate.isNegate()
                                        ? DataComponentPatch.EMPTY
                                        : predicate.component()
                                            .filter(component -> !component.isNegate())
                                            .map(DataComponentPredicate::patch)
                                            .orElse(DataComponentPatch.EMPTY);
        return predicate.fluids().stream()
            .flatMap(HolderSet::stream)
            .filter(holder -> holder.value().defaultFluidState().isSource())
            .distinct()
            .map(holder -> new FluidStack(holder, displayAmount, components))
            .toList();
    }

    public static List<FluidStack> getDisplayFluids(FluidStack fluid, int amount) {
        if (!fluid.getFluid().defaultFluidState().isSource()) return List.of();
        int displayAmount = amount > 0 ? amount : FluidType.BUCKET_VOLUME;
        return List.of(fluid.copyWithAmount(displayAmount));
    }

    public static void suppressHoverOverlays(IRecipeExtrasBuilder builder) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

}
