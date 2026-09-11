package dev.dubhe.anvilcraft.integration.jei.util;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilAmplifierBlock;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IIngredientConsumer;
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
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/** JEI slots that make custom-rendered previews interactive without drawing an ingredient over them. */
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

    public static Optional<BlockState> getDisplayedState(
        IRecipeSlotsView recipeSlotsView,
        String slotName,
        List<BlockState> states
    ) {
        if (states.isEmpty()) return Optional.empty();
        Optional<Block> displayedBlock = recipeSlotsView.findSlotByName(SLOT_PREFIX + slotName)
            .flatMap(IRecipeSlotView::getDisplayedItemStack)
            .map(ItemStack::getItem)
            .filter(BlockItem.class::isInstance)
            .map(BlockItem.class::cast)
            .map(BlockItem::getBlock);
        return displayedBlock
            .flatMap(block -> states.stream().filter(state -> state.is(block)).findFirst())
            .or(() -> Optional.of(states.getFirst()))
            .map(JeiBlockIngredientUtil::getRenderablePreviewState);
    }

    /**
     * 把任意部件状态映射到承载渲染模型的状态。
     *
     * <p>具体映射由方块自己通过 {@link IMultiPartBlockModelHolder#getModelHolderState} 声明
     * （巨型铁砧映射到 MID_CENTER、锻星砧增幅器映射到朝向前方的中部），本工具只做统一查询，
     * 不再逐方块特判。</p>
     */
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

    @SuppressWarnings("removal")
    private record TransparentItemRenderer(int width, int height) implements IIngredientRenderer<ItemStack> {
        @Override
        public void render(GuiGraphics guiGraphics, ItemStack ingredient) {
        }

        @Override
        @SuppressWarnings("removal")
        public List<Component> getTooltip(ItemStack ingredient, TooltipFlag tooltipFlag) {
            Minecraft minecraft = Minecraft.getInstance();
            Item.TooltipContext context = Item.TooltipContext.of(minecraft.level);
            return ingredient.getTooltipLines(context, minecraft.player, tooltipFlag);
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
        public List<ITypedIngredient<?>> getAllIngredientsList() {
            return this.delegate.getAllIngredientsList();
        }

        @Override
        public Optional<ITypedIngredient<?>> getDisplayedIngredient() {
            return this.delegate.getDisplayedIngredient();
        }

        @Override
        public Stream<ITypedIngredient<?>> getDisplayedIngredients() {
            return this.delegate.getDisplayedIngredient().stream();
        }

        @Override
        public Optional<TagKey<?>> getTagKey() {
            return Optional.empty();
        }

        @Override
        public RecipeIngredientRole getRole() {
            return this.delegate.getRole();
        }

        @Override
        public void drawHighlight(GuiGraphics guiGraphics, int color) {
            this.delegate.drawHighlight(guiGraphics, color);
        }

        @Override
        public Optional<String> getSlotName() {
            return this.delegate.getSlotName();
        }

        @Override
        public void draw(GuiGraphics guiGraphics) {
            this.delegate.draw(guiGraphics);
        }

        @Override
        public void drawHoverOverlays(GuiGraphics guiGraphics) {
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
        public void drawTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            this.delegate.drawTooltip(guiGraphics, mouseX, mouseY);
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
        public IIngredientConsumer createDisplayOverrides() {
            return this.delegate.createDisplayOverrides();
        }

        @Override
        public void clearDisplayOverrides() {
            this.delegate.clearDisplayOverrides();
        }

        @Override
        public Rect2i getRect() {
            return this.delegate.getRect();
        }

        @Override
        public Rect2i getAreaIncludingBackground() {
            return this.delegate.getAreaIncludingBackground();
        }
    }
}
