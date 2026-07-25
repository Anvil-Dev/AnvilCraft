package dev.dubhe.anvilcraft.integration.jei.util;

import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 共享的 JEI 流体成分工具，将流体内容渲染为格子而非大锅方块。
 */
public final class JeiFluidUtil {
    private static final String SLOT_PREFIX = JeiBlockIngredientUtil.PREVIEW_SLOT_PREFIX + "fluid/";

    private JeiFluidUtil() {
    }

    public static void drawFluidInputSlots(GuiGraphics guiGraphics, IDrawable slot, int inputSize) {
        JeiSlotUtil.drawSlots(guiGraphics, slot, inputSize, JeiSlotUtil.INPUT_X - 1, JeiSlotUtil.FLUID_Y - 1);
    }

    public static void drawFluidOutputSlots(GuiGraphics guiGraphics, IDrawable slot, int inputSize) {
        JeiSlotUtil.drawSlots(guiGraphics, slot, inputSize, JeiSlotUtil.OUTPUT_X - 1, JeiSlotUtil.FLUID_Y - 1);
    }

    public static void addFluidInputSlot(IRecipeLayoutBuilder builder, String name, int width, int height, HasCauldronSimple cauldron) {
        addInputSlot(builder, name, JeiSlotUtil.INPUT_X, JeiSlotUtil.FLUID_Y, width, height, cauldron);
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
            getInputFluids(cauldron), cauldron.consume()
        );
    }

    public static void addFluidOutputSlot(IRecipeLayoutBuilder builder, String name, int width, int height, HasCauldronSimple cauldron) {
        addOutputSlot(builder, name, JeiSlotUtil.OUTPUT_X, JeiSlotUtil.FLUID_Y, width, height, cauldron);
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
            getFluids(cauldron.transform(), null), cauldron.produce()
        );
    }

    /**
     * 注册流体及其桶，不创建可见的预览格子。
     */
    public static void addOutputIngredients(IRecipeLayoutBuilder builder, HasCauldronSimple cauldron) {
        addInvisibleIngredients(
            builder, RecipeIngredientRole.OUTPUT,
            getFluids(cauldron.transform(), null), cauldron.produce()
        );
    }

    public static void suppressHoverOverlays(IRecipeExtrasBuilder builder) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

    public static Optional<Fluid> getDisplayedFluid(IRecipeSlotsView recipeSlotsView, String name) {
        return recipeSlotsView.findSlotByName(SLOT_PREFIX + name)
            .flatMap(IRecipeSlotView::getDisplayedIngredient)
            .flatMap(ingredient -> ingredient.getIngredient(NeoForgeTypes.FLUID_STACK))
            .map(FluidStack::getFluid);
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
        int amount
    ) {
        if (fluids.isEmpty()) return;
        long displayAmount = amount > 0 ? amount : FluidType.BUCKET_VOLUME;
        IRecipeSlotBuilder slot = builder.addSlot(role, x, y)
            .setSlotName(SLOT_PREFIX + name)
            .setFluidRenderer(displayAmount, false, width, height);
        fluids.forEach(fluid -> slot.addFluidStack(fluid, displayAmount));
        addBucketIngredients(builder, role, fluids);
    }

    private static void addInvisibleIngredients(
        IRecipeLayoutBuilder builder,
        RecipeIngredientRole role,
        List<Fluid> fluids,
        int amount
    ) {
        if (fluids.isEmpty()) return;
        long ingredientAmount = amount > 0 ? amount : FluidType.BUCKET_VOLUME;
        var fluidIngredients = builder.addInvisibleIngredients(role);
        fluids.forEach(fluid -> fluidIngredients.addFluidStack(fluid, ingredientAmount));
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

    private static List<Fluid> getInputFluids(HasCauldronSimple cauldron) {
        return getFluids(cauldron.fluid(), cauldron.fluidTag());
    }

    private static List<Fluid> getFluids(ResourceLocation fluidId, @Nullable ResourceLocation fluidTag) {
        if (fluidTag != null) {
            TagKey<Fluid> tag = TagKey.create(Registries.FLUID, fluidTag);
            return BuiltInRegistries.FLUID.getTag(tag)
                .stream()
                .flatMap(HolderSet.ListBacked::stream)
                .map(Holder::value)
                .filter(fluid -> fluid.defaultFluidState().isSource())
                .distinct()
                .toList();
        }
        if (!HasCauldron.isNotEmpty(fluidId)) return List.of();
        return BuiltInRegistries.FLUID.getHolder(fluidId)
            .stream()
            .map(Holder::value)
            .filter(fluid -> fluid.defaultFluidState().isSource())
            .toList();
    }

}
