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
import mezz.jei.api.neoforge.NeoForgeTypes;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

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

    /**
     * 同时容纳物品与流体的自绘槽位。
     *
     * <p>用于结果格：普通结果是方块物品、流体结果是流体本身（而不是它的桶），
     * 两者放同一格由 JEI 轮换，实际画面由配方类别自行渲染。</p>
     */
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

    /** 取指定槽位当前轮换到的成分，物品与流体皆可。 */
    public static Optional<ITypedIngredient<?>> getDisplayedIngredient(IRecipeSlotsView recipeSlotsView, String name) {
        return recipeSlotsView.findSlotByName(SLOT_PREFIX + name)
            .flatMap(IRecipeSlotView::getDisplayedIngredient);
    }

    /**
     * 方块状态对应的槽位物品；流体方块没有 {@code BlockItem}，因此会被跳过。
     */
    public static List<ItemStack> toItemStacks(List<BlockState> states) {
        return states.stream()
            .map(state -> new ItemStack(state.getBlock()))
            .filter(stack -> !stack.isEmpty())
            .distinct()
            .toList();
    }

    /**
     * 方块状态对应的槽位流体。
     *
     * <p>水、经验流体、熔岩等只注册了方块、没有 {@code BlockItem}，
     * 展示成流体本身而不是它的桶，提示里才是流体名。</p>
     */
    public static List<FluidStack> toFluidStacks(List<BlockState> states) {
        return states.stream()
            .map(BlockState::getFluidState)
            .filter(state -> !state.isEmpty())
            .map(state -> new FluidStack(state.getType(), FluidType.BUCKET_VOLUME))
            .distinct()
            .toList();
    }

    /**
     * 把槽位中当前轮换到的成分还原为对应的方块状态。
     *
     * <p>流体成分按流体匹配，物品成分按方块物品匹配。</p>
     */
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

    private record TransparentItemRenderer(int width, int height) implements IIngredientRenderer<ItemStack> {
        @Override
        public void render(GuiGraphics guiGraphics, ItemStack ingredient) {
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

        /**
         * 接口仍把两参重载声明为抽象方法，因此必须实现。
         *
         * <p>本类不用它取提示（JEI 会走带上下文的四参重载），这里仅提供等价实现：
         * 上下文取自当前客户端，玩家与高级提示标记沿用 JEI 传入的值。
         * 该重载已被标记为待移除，是因为必须实现才压制告警。</p>
         */
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

    /**
     * 不绘制内容、只提供流体名称提示的渲染器。
     *
     * <p>结果格的画面由配方类别自己按方块状态渲染，因此这里保持透明；
     * 但提示要给出流体名，而不是它的桶。</p>
     */
    private record TransparentFluidRenderer(int width, int height) implements IIngredientRenderer<FluidStack> {
        @Override
        public void render(GuiGraphics guiGraphics, FluidStack ingredient) {
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

        /**
         * 接口仍把两参重载声明为抽象方法，因此必须实现；等价于上面的四参重载。
         * 该重载已被标记为待移除，是因为必须实现才压制告警。
         */
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
