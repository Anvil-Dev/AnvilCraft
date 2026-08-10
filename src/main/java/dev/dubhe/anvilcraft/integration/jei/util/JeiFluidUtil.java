package dev.dubhe.anvilcraft.integration.jei.util;

import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 共享的 JEI 流体成分工具，将流体内容渲染为格子而非大锅方块。
 */
public final class JeiFluidUtil {
    private static final String SLOT_PREFIX = JeiBlockIngredientUtil.PREVIEW_SLOT_PREFIX + "fluid/";

    private JeiFluidUtil() {
    }

    /**
     * 存在物品时流体位置向下偏移。
     */
    public static void addFluidInputSlot(
        IRecipeLayoutBuilder builder,
        String name,
        int width,
        int height,
        HasCauldronSimple cauldron
    ) {
        JeiFluidUtil.addInputSlot(builder, name, JeiSlotUtil.INPUT_X, JeiSlotUtil.FLUID_Y, width, height, cauldron);
    }

    /**
     * 使用默认的居中位置。
     */
    public static void addDefaultInputSlot(
        IRecipeLayoutBuilder builder,
        String name,
        int width,
        int height,
        HasCauldronSimple cauldron
    ) {
        JeiFluidUtil.addInputSlot(builder, name, JeiSlotUtil.INPUT_X, JeiSlotUtil.DEFAULT_Y, width, height, cauldron);
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
        JeiFluidUtil.addSlot(
            builder,
            RecipeIngredientRole.INPUT,
            name,
            x,
            y,
            width,
            height,
            JeiFluidUtil.getFluids(cauldron.fluid(), cauldron.fluidTag()),
            cauldron.consume()
        );
    }

    /**
     * 存在物品时流体位置向下偏移。
     */
    public static void addFluidOutputSlot(
        IRecipeLayoutBuilder builder,
        String name,
        int width,
        int height,
        HasCauldronSimple cauldron
    ) {
        JeiFluidUtil.addOutputSlot(builder, name, JeiSlotUtil.OUTPUT_X, JeiSlotUtil.FLUID_Y, width, height, cauldron);
    }

    /**
     * 使用默认的居中位置。
     */
    public static void addDefaultOutputSlot(
        IRecipeLayoutBuilder builder,
        String name,
        int width,
        int height,
        HasCauldronSimple cauldron
    ) {
        JeiFluidUtil.addOutputSlot(builder, name, JeiSlotUtil.OUTPUT_X, JeiSlotUtil.DEFAULT_Y, width, height, cauldron);
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
        JeiFluidUtil.addSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            name,
            x,
            y,
            width,
            height,
            JeiFluidUtil.getFluids(cauldron.transform(), null),
            cauldron.produce()
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
        int amount
    ) {
        if (fluids.isEmpty()) return;
        long displayAmount = amount > 0 ? amount : FluidType.BUCKET_VOLUME;
        IRecipeSlotBuilder slot = builder.addSlot(role, x, y)
            .setSlotName(JeiFluidUtil.SLOT_PREFIX + name)
            .setFluidRenderer(displayAmount, false, width, height);
        fluids.forEach(fluid -> slot.add(fluid, displayAmount));
        JeiFluidUtil.addBucketIngredients(builder, role, fluids);
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

    private static List<Fluid> getFluids(Identifier fluidId, @Nullable Identifier fluidTag) {
        if (fluidTag != null) {
            TagKey<Fluid> tag = TagKey.create(Registries.FLUID, fluidTag);
            List<Fluid> tagged = new ArrayList<>();
            for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(tag)) {
                Fluid fluid = holder.value();
                if (fluid.defaultFluidState().isSource() && !tagged.contains(fluid)) tagged.add(fluid);
            }
            return List.copyOf(tagged);
        }
        if (!HasCauldron.isNotEmpty(fluidId)) return List.of();
        return BuiltInRegistries.FLUID.get(fluidId)
            .stream()
            .map(Holder::value)
            .filter(fluid -> fluid.defaultFluidState().isSource())
            .toList();
    }

    public static void suppressHoverOverlays(IRecipeExtrasBuilder builder) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }
}
