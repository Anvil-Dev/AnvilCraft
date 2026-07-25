package dev.dubhe.anvilcraft.integration.jei.util;

import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// 以炼药锅方块形式渲染的流体内容所共用的 JEI 原料槽
public final class JeiFluidIngredientUtil {
    private static final String SLOT_PREFIX = JeiBlockIngredientUtil.PREVIEW_SLOT_PREFIX + "fluid/";

    private JeiFluidIngredientUtil() {
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
        addSlot(builder, RecipeIngredientRole.INPUT, name, x, y, width, height,
            getInputFluids(cauldron), cauldron.consume());
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
        addSlot(builder, RecipeIngredientRole.OUTPUT, name, x, y, width, height,
            getFluids(cauldron.transform(), null), cauldron.produce());
    }

    /// 只登记流体及其桶物品，不创建可见的预览槽
    public static void addOutputIngredients(IRecipeLayoutBuilder builder, HasCauldronSimple cauldron) {
        addInvisibleIngredients(builder, RecipeIngredientRole.OUTPUT,
            getFluids(cauldron.transform(), null), cauldron.produce());
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
            .setCustomRenderer(NeoForgeTypes.FLUID_STACK, new TransparentFluidRenderer(width, height));
        fluids.forEach(fluid -> slot.add(fluid, displayAmount));
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
        fluids.forEach(fluid -> fluidIngredients.add(fluid, ingredientAmount));
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

    private record TransparentFluidRenderer(int width, int height) implements IIngredientRenderer<FluidStack> {
        @Override
        public void render(GuiGraphicsExtractor guiGraphics, FluidStack ingredient) {
        }

        @Override
        public List<Component> getTooltip(FluidStack ingredient, TooltipFlag tooltipFlag) {
            return List.of(ingredient.getHoverName());
        }

        @Override
        public int getWidth() {
            return this.width;
        }

        @Override
        public int getHeight() {
            return this.height;
        }
    }
}
