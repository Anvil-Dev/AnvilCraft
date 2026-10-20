package dev.dubhe.anvilcraft.integration.jei.util;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilAmplifierBlock;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder;
import dev.dubhe.anvilcraft.block.workstation.GiantAnvilBlock;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.ISlottedRecipeWidget;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class JeiBlockIngredientUtil {
    static final String PREVIEW_SLOT_PREFIX = "anvilcraft:preview/";
    private static final String SLOT_PREFIX = "anvilcraft:block_preview/";

    private JeiBlockIngredientUtil() {
    }

    @SuppressWarnings("UnusedReturnValue")
    public static IRecipeSlotBuilder addInputSlot(
        IRecipeLayoutBuilder builder,
        String name,
        int x,
        int y,
        int width,
        int height,
        BlockStatePredicate input
    ) {
        List<ItemStack> stacks = input.getBlocks().stream()
            .map(holder -> new ItemStack(holder.value()))
            .toList();
        return addSlot(builder, RecipeIngredientRole.INPUT, name, x, y, width, height, stacks);
    }

    public static IRecipeSlotBuilder addSlot(
        IRecipeLayoutBuilder builder,
        RecipeIngredientRole role,
        String name,
        int x,
        int y,
        int width,
        int height,
        Block block
    ) {
        return addSlot(builder, role, name, x, y, width, height, List.of(new ItemStack(block)));
    }

    public static IRecipeSlotBuilder addSlot(
        IRecipeLayoutBuilder builder,
        RecipeIngredientRole role,
        String name,
        int x,
        int y,
        int width,
        int height,
        List<ItemStack> stacks
    ) {
        return builder.addSlot(role, x, y)
            .setSlotName(SLOT_PREFIX + name)
            .setCustomRenderer(VanillaTypes.ITEM_STACK, new TransparentItemRenderer(width, height))
            .addItemStacks(stacks);
    }

    public static IRecipeSlotBuilder addSlot(
        IRecipeLayoutBuilder builder,
        RecipeIngredientRole role,
        String name,
        int x,
        int y,
        int width,
        int height,
        List<ItemStack> stacks,
        List<FluidStack> fluids
    ) {
        IRecipeSlotBuilder slot = builder.addSlot(role, x, y)
            .setSlotName(SLOT_PREFIX + name)
            .setCustomRenderer(VanillaTypes.ITEM_STACK, new TransparentItemRenderer(width, height));
        if (!fluids.isEmpty()) {
            slot.setCustomRenderer(NeoForgeTypes.FLUID_STACK, new TransparentFluidRenderer(width, height));
        }
        if (!stacks.isEmpty()) {
            slot.addItemStacks(stacks);
        }
        fluids.forEach(fluid -> slot.addFluidStack(
            fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch()
        ));
        return slot;
    }

    public static Optional<ITypedIngredient<?>> getDisplayedIngredient(IRecipeSlotsView recipeSlotsView, String name) {
        return recipeSlotsView.findSlotByName(SLOT_PREFIX + name)
            .flatMap(IRecipeSlotView::getDisplayedIngredient);
    }

    public static List<ItemStack> toItemStacks(List<BlockState> states) {
        return states.stream()
            .map(state -> new ItemStack(state.getBlock()))
            .filter(stack -> !stack.isEmpty())
            .distinct()
            .toList();
    }

    public static List<FluidStack> toFluidStacks(List<BlockState> states) {
        return states.stream()
            .map(BlockState::getFluidState)
            .filter(state -> !state.isEmpty())
            .map(state -> new FluidStack(state.getType(), FluidType.BUCKET_VOLUME))
            .distinct()
            .toList();
    }

    @Nullable
    public static BlockState toBlockState(@Nullable ITypedIngredient<?> ingredient, List<BlockState> states) {
        if (ingredient == null) return null;
        FluidStack fluid = ingredient.getIngredient(NeoForgeTypes.FLUID_STACK).orElse(null);
        if (fluid != null) {
            for (BlockState state : states) {
                if (state.getFluidState().getType().isSame(fluid.getFluid())) return state;
            }
        }
        ItemStack stack = ingredient.getItemStack().orElse(null);
        if (stack != null && !stack.isEmpty()) {
            for (BlockState state : states) {
                if (new ItemStack(state.getBlock()).is(stack.getItem())) return state;
            }
        }
        return null;
    }

    public static void suppressHoverOverlays(IRecipeExtrasBuilder builder) {
        List<IRecipeSlotDrawable> slots = builder.getRecipeSlots().getSlots().stream()
            .filter(slot -> slot.getSlotName()
                .filter(name -> name.startsWith(SLOT_PREFIX) || name.startsWith(PREVIEW_SLOT_PREFIX))
                .isPresent())
            .toList();
        if (!slots.isEmpty()) {
            builder.addWidget(new NoHoverSlotsWidget(slots));
        }
    }

    public static Optional<ItemStack> getDisplayedItemStack(IRecipeSlotsView recipeSlotsView, String slotName) {
        return recipeSlotsView.findSlotByName(SLOT_PREFIX + slotName)
            .flatMap(IRecipeSlotView::getDisplayedItemStack);
    }

    public static Optional<BlockState> getDisplayedState(
        IRecipeSlotsView recipeSlotsView,
        String slotName,
        List<BlockState> states
    ) {
        if (states.isEmpty()) return Optional.empty();
        Optional<Block> displayedBlock = getDisplayedItemStack(recipeSlotsView, slotName)
            .map(ItemStack::getItem)
            .filter(BlockItem.class::isInstance)
            .map(BlockItem.class::cast)
            .map(BlockItem::getBlock);
        return displayedBlock
            .flatMap(block -> states.stream().filter(state -> state.is(block)).findFirst())
            .or(() -> Optional.of(states.getFirst()))
            .map(JeiBlockIngredientUtil::getRenderablePreviewState);
    }

    public static BlockState getRenderablePreviewState(BlockState state) {
        return IMultiPartBlockModelHolder.modelHolderState(state);
    }

    public static int getRenderablePreviewScale(BlockState state, int defaultScale) {
        if (state.getBlock() instanceof GiantAnvilBlock
            || state.getBlock() instanceof CelestialForgingAnvilAmplifierBlock) {
            return Math.min(defaultScale, 5);
        }
        return defaultScale;
    }

    private record TransparentItemRenderer(int width, int height) implements IIngredientRenderer<ItemStack> {
        @Override
        public void render(GuiGraphicsExtractor guiGraphics, ItemStack ingredient) {
        }

        @Override
        public List<Component> getTooltip(
            ItemStack ingredient,
            Item.TooltipContext context,
            @Nullable Player player,
            TooltipFlag tooltipFlag
        ) {
            return ingredient.getTooltipLines(context, player, tooltipFlag);
        }

        @Override
        @SuppressWarnings("removal")
        public List<Component> getTooltip(ItemStack ingredient, TooltipFlag tooltipFlag) {
            Minecraft minecraft = Minecraft.getInstance();
            return this.getTooltip(
                ingredient, Item.TooltipContext.of(minecraft.level), minecraft.player, tooltipFlag
            );
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

    private record TransparentFluidRenderer(int width, int height) implements IIngredientRenderer<FluidStack> {
        @Override
        public void render(GuiGraphicsExtractor guiGraphics, FluidStack ingredient) {
        }

        @Override
        public List<Component> getTooltip(
            FluidStack ingredient,
            Item.TooltipContext context,
            @Nullable Player player,
            TooltipFlag tooltipFlag
        ) {
            return List.of(ingredient.getHoverName());
        }

        @Override
        @SuppressWarnings("removal")
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

    private record NoHoverSlotsWidget(List<IRecipeSlotDrawable> slots) implements ISlottedRecipeWidget {
        @Override
        public ScreenPosition getPosition() {
            return new ScreenPosition(0, 0);
        }

        @Override
        public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
            return this.slots.stream()
                .filter(slot -> slot.isMouseOver(mouseX, mouseY))
                .findFirst()
                .map(NoHoverRecipeSlot::new)
                .map(slot -> new RecipeSlotUnderMouse(slot, this.getPosition()));
        }
    }

    @SuppressWarnings({"removal", "NonExtendableApiUsage"})
    private record NoHoverRecipeSlot(IRecipeSlotDrawable delegate) implements IRecipeSlotDrawable {
        @Override
        public Stream<ITypedIngredient<?>> getAllIngredients() {
            return this.delegate.getAllIngredients();
        }

        @Override
        public List<@Nullable ITypedIngredient<?>> getAllIngredientsList() {
            return this.delegate.getAllIngredientsList();
        }

        @Override
        public Optional<ITypedIngredient<?>> getDisplayedIngredient() {
            return this.delegate.getDisplayedIngredient();
        }

        @Override
        public Stream<ITypedIngredient<?>> getDisplayedIngredients() {
            return this.delegate.getDisplayedIngredients();
        }

        @Override
        public RecipeIngredientRole getRole() {
            return this.delegate.getRole();
        }

        @Override
        public void drawHighlight(GuiGraphicsExtractor graphics, int color) {
            this.delegate.drawHighlight(graphics, color);
        }

        @Override
        public Optional<String> getSlotName() {
            return this.delegate.getSlotName();
        }

        @Override
        public Optional<TagKey<?>> getTagKey() {
            return this.delegate.getTagKey();
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics) {
            this.draw(graphics, false);
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, boolean hovered) {
            this.delegate.draw(graphics, false);
        }

        @Override
        public void drawHoverOverlays(GuiGraphicsExtractor graphics) {
        }

        @Override
        public List<Component> getTooltip() {
            return this.delegate.getTooltip();
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltipBuilder) {
            this.delegate.getTooltip(tooltipBuilder);
        }

        @Override
        public void drawTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            this.delegate.drawTooltip(graphics, mouseX, mouseY);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.delegate.isMouseOver(mouseX, mouseY);
        }

        @Override
        public void setPosition(int x, int y) {
            this.delegate.setPosition(x, y);
        }

        @Override
        public IIngredientAcceptor<?> createDisplayOverrides() {
            return this.delegate.createDisplayOverrides();
        }

        @Override
        public void clearDisplayOverrides() {
            this.delegate.clearDisplayOverrides();
        }

        @Override
        public Rect2i getAreaIncludingBackground() {
            return this.delegate.getAreaIncludingBackground();
        }
    }
}
