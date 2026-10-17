package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.FluidRateSlider;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.renderer.item.FilterItemRenderer;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.ControlValveMenu;
import dev.dubhe.anvilcraft.network.ControlValveFilterItemPacket;
import dev.dubhe.anvilcraft.network.ControlValveFilterPacket;
import dev.dubhe.anvilcraft.network.ControlValveUpdatePacket;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static dev.dubhe.anvilcraft.util.FluidUtil.getFilterTooltip;

/**
 * 控制阀 GUI。上方过滤格设置允许通过的流体；下方滑条 + 按钮 + 数字框设置最大流速（0~2000 mB/tick）。
 */
public class ControlValveScreen extends AbstractContainerScreen<ControlValveMenu> implements IGhostIngredientScreen {
    public static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "control_valve");
    public static final ResourceLocation BUTTON_MAX = SharedTextures.textureGui("misc/slider_like/button_max");
    public static final ResourceLocation BUTTON_ADD = SharedTextures.textureGui("misc/slider_like/button_add");
    public static final ResourceLocation BUTTON_MINUS = SharedTextures.textureGui("misc/slider_like/button_minus");
    public static final ResourceLocation BUTTON_MIN = SharedTextures.textureGui("misc/slider_like/button_min");

    /** 过滤格（非物品槽）本地坐标 */
    private static final int FILTER_X = ControlValveMenu.FILTER_X;
    private static final int FILTER_Y = ControlValveMenu.FILTER_Y;
    /** JEI 幽灵目标用的虚拟槽索引（本菜单无过滤槽，用 0 占位） */
    private static final int FILTER_GHOST_ID = 0;

    /** 流体贴图的 blit 偏移：需大于 renderItem 使用的 z=150 才能盖在物品之上 */
    private static final int FLUID_SPRITE_BLIT_OFFSET = 200;

    /** 黑名单屏障：需高于流体贴图，故在 renderItem 的 z=150 之上再抬一段 */
    private static final int BARRIER_EXTRA_Z = 100;

    /** 黑名单屏障相对格子的缩放：0.5 = 8px，与流体图标同尺寸，缩放此值即可调整屏障大小 */
    private static final float BARRIER_SCALE = 0.5f;

    /** 黑名单屏障图标 */
    private static final ItemStack DENY_BARRIER = Items.BARRIER.getDefaultInstance();

    /** 客户端本地过滤流体缓存（服务端回传更新） */
    private FluidStack filterFluid = FluidStack.EMPTY;

    /** 客户端本地过滤器物品缓存（服务端回传更新） */
    private ItemStack filterItem = ItemStack.EMPTY;

    private FluidRateSlider slider;
    private EditBox valueBox;

    public ControlValveScreen(ControlValveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
        int ox = this.leftPos;
        int oy = this.topPos;

        // 滑条：W8H41 到 W167H48（宽 160，长度含滑块 16px）
        this.slider = new FluidRateSlider(ox + 8, oy + 41, 160, this::onSliderChange);

        // 数字输入框（居中，位于滑条与按钮之间的中部按钮区中央）
        this.valueBox = new EditBox(this.font, ox + 51, oy + 58, 28, 8, Component.literal("value"));
        this.valueBox.setCanLoseFocus(true);
        this.valueBox.setTextColor(-1);
        this.valueBox.setTextColorUneditable(-1);
        this.valueBox.setBordered(false);
        this.valueBox.setMaxLength(4);
        this.valueBox.setResponder(this::onValueInput);
        this.valueBox.setValue("");

        // 四按钮：外侧 min/max（-空/+满），内侧 minus/add（-档/+档）
        TexturedButton min = new TexturedButton(ox + 8, oy + 53, 16, 16, BUTTON_MIN, 16, 16, 32,
            b -> this.slider.setValueWithUpdate(0));
        TexturedButton minus = new TexturedButton(ox + 26, oy + 53, 16, 16, BUTTON_MINUS, 16, 16, 32,
            b -> this.slider.step(-1));
        TexturedButton add = new TexturedButton(ox + 134, oy + 53, 16, 16, BUTTON_ADD, 16, 16, 32,
            b -> this.slider.step(1));
        TexturedButton max = new TexturedButton(ox + 152, oy + 53, 16, 16, BUTTON_MAX, 16, 16, 32,
            b -> this.slider.setValueWithUpdate(FluidRateSlider.MAX));

        this.addRenderableWidget(min);
        this.addRenderableWidget(minus);
        this.addRenderableWidget(this.slider);
        this.addRenderableWidget(add);
        this.addRenderableWidget(max);
        this.addRenderableWidget(this.valueBox);
    }

    /** 服务端 init 包设置初值。 */
    public void setValue(int value) {
        this.slider.setValue(value);
        this.valueBox.setValue(Integer.toString(value));
    }

    /** 该阀门是否被红石锁定（实时读客户端 BE）。 */
    private boolean isLocked() {
        return this.getMenu().getBlockEntity() != null && this.getMenu().getBlockEntity().isLocked();
    }

    /** 过滤流体被服务端回传更新。 */
    public void setFilter(int index, FluidStack fluid) {
        this.filterFluid = fluid;
    }

    /** 过滤器物品被服务端回传更新。 */
    public void setFilterItem(ItemStack filterItem) {
        this.filterItem = filterItem;
    }

    /**
     * 当前过滤格应展示的流体。
     *
     * <p>放入过滤器物品时轮播其内容解析出的多种流体，否则直接用单流体白名单。</p>
     */
    private FluidStack displayedFilterFluid() {
        if (this.filterItem.isEmpty()) return this.filterFluid;
        List<FluidStack> fluids = ControlValveBlockEntity.resolveFluids(this.filterItem);
        if (fluids.isEmpty()) return FluidStack.EMPTY;
        int interval = Math.max(1, AnvilCraft.CLIENT_CONFIG.ui.filterItemDisplayIntervalMillis);
        int index = (int) Math.floorMod(Util.getMillis() / interval, fluids.size());
        return fluids.get(index);
    }

    private void onSliderChange(int value) {
        if (this.isLocked()) {
            return;
        }
        this.valueBox.setValue(Integer.toString(value));
        PacketDistributor.sendToServer(new ControlValveUpdatePacket(value));
    }

    private void onValueInput(String text) {
        if (this.isLocked()) {
            return;
        }
        int v;
        if (text.matches("^[0-9]+$")) {
            v = Integer.parseInt(text);
        } else if (text.isEmpty()) {
            return;
        } else {
            this.valueBox.setValue(Integer.toString(this.slider.getValue()));
            return;
        }
        v = Math.max(0, Math.min(FluidRateSlider.MAX, v));
        this.slider.setValue(v);
        PacketDistributor.sendToServer(new ControlValveUpdatePacket(v));
    }

    // ---- JEI 幽灵流体拖入 ----

    @Override
    public Collection<Integer> getGhostSlots() {
        return List.of(FILTER_GHOST_ID);
    }

    @Override
    public int[] getGhostSlotArea(int slotIndex) {
        return new int[]{FILTER_X, FILTER_Y, 16, 16};
    }

    @Override
    public void acceptGhost(Slot slot, ItemStack ingredient) {
        // 物品拖入 → 过滤器直接放入过滤格，其余尝试解析其流体（有桶物品的流体）
        if (ingredient.is(ModItems.FILTER)) {
            this.setFilterItem(ingredient.copyWithCount(1));
            PacketDistributor.sendToServer(new ControlValveFilterItemPacket(ingredient.copyWithCount(1)));
            return;
        }
        FluidStack fluid = ControlValveBlockEntity.fluidOf(ingredient);
        this.setFilter(0, fluid);
        PacketDistributor.sendToServer(new ControlValveFilterPacket(0, fluid));
    }

    @Override
    public void acceptFluidGhost(int slotIndex, FluidStack fluid) {
        // 拖入流体视为手动设置白名单，同时取出过滤器，避免两者判定叠加
        if (!this.filterItem.isEmpty()) {
            this.setFilterItem(ItemStack.EMPTY);
            PacketDistributor.sendToServer(new ControlValveFilterItemPacket(ItemStack.EMPTY));
        }
        this.setFilter(0, fluid);
        PacketDistributor.sendToServer(new ControlValveFilterPacket(0, fluid));
    }

    /**
     * 左键点击过滤格：
     * <ul>
     *   <li>手持过滤器 → 放入过滤器，按过滤器内容过滤多种流体；</li>
     *   <li>手持其他能解析出流体的物品（桶等）→ 设为单流体白名单；</li>
     *   <li>空手 → 取出过滤器并清空单流体白名单。</li>
     * </ul>
     * 同时把左键转发给滑条以支持拖动。
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isHovering(FILTER_X, FILTER_Y, 16, 16, mouseX, mouseY)) {
            ItemStack carried = this.getMenu().getCarried();
            if (carried.is(ModItems.FILTER)) {
                ItemStack placed = carried.copyWithCount(1);
                this.setFilterItem(placed);
                PacketDistributor.sendToServer(new ControlValveFilterItemPacket(placed));
                return true;
            }
            if (!this.filterItem.isEmpty()) {
                this.setFilterItem(ItemStack.EMPTY);
                PacketDistributor.sendToServer(new ControlValveFilterItemPacket(ItemStack.EMPTY));
            }
            // 空手或非桶 → EMPTY（清空单流体白名单）
            FluidStack fluid = ControlValveBlockEntity.fluidOf(carried);
            this.setFilter(0, fluid);
            PacketDistributor.sendToServer(new ControlValveFilterPacket(0, fluid));
            return true;
        }
        if (button == 0 && !this.isLocked()) {
            this.slider.onClick(mouseX, mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.isLocked()) {
            this.slider.onDrag(mouseX, mouseY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.slider.onReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    /**
     * 在当前过滤格上叠加黑名单屏障。
     *
     * <p>屏障必须画在流体图标之上，而它原本由 {@code FilterItemRenderer} 在渲染本体时一并绘制、
     * 无法保证高于流体图标，因此这里由屏幕在流体之后单独补画。</p>
     *
     * @param guiGraphics 画布
     * @param fx          过滤格 X 坐标
     * @param fy          过滤格 Y 坐标
     */
    private void renderDenyBarrier(GuiGraphics guiGraphics, int fx, int fy) {
        guiGraphics.pose().pushPose();
        // 内层 renderItem 自带 z=150，这里再抬 Z 使其高于流体图标的 blitOffset
        guiGraphics.pose().translate(fx + 8, fy + 8, BARRIER_EXTRA_Z);
        guiGraphics.pose().scale(BARRIER_SCALE, BARRIER_SCALE, 1.0f);
        // 以格心为基准缩放：renderItem 自 (x+8, y+8) 起画 16×16
        guiGraphics.renderItem(DENY_BARRIER, -8, -8);
        guiGraphics.pose().popPose();
    }

    /** 在 (x,y) 处画 size×size 的流体静态贴图（取自方块图集，带色调）。 */
    private void renderFluidSprite(GuiGraphics guiGraphics, FluidStack fluid, int x, int y, int size) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(ext.getStillTexture(fluid));
        int tint = ext.getTintColor(fluid);
        // 物品经 renderItem 画在 z=150，这里取更大的 blitOffset 才能盖在其上
        guiGraphics.blit(x, y, FLUID_SPRITE_BLIT_OFFSET, size, size, sprite, FastColor.ARGB32.red(tint) / 255f,
            FastColor.ARGB32.green(tint) / 255f, FastColor.ARGB32.blue(tint) / 255f, 1.0f);
    }

    /**
     * 渲染过滤格中流体的提示框
     *
     * <p>过滤格不是物品槽，流体只是画上去的贴图，原版不会为它显示任何提示框，
     * 因此这里按流体方块的样式补上。</p>
     *
     * @param guiGraphics 画布
     * @param mouseX      鼠标 X 坐标
     * @param mouseY      鼠标 Y 坐标
     */
    private void renderFilterFluidTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.isHovering(FILTER_X, FILTER_Y, 16, 16, mouseX, mouseY)) {
            return;
        }
        TooltipFlag flag = Minecraft.getInstance().options.advancedItemTooltips
            ? TooltipFlag.ADVANCED.asCreative()
            : TooltipFlag.NORMAL.asCreative();
        // 过滤器内可含多种流体，一次列出全部，而不是只显示轮播到的那一个
        List<FluidStack> fluids = this.filterItem.isEmpty()
            ? (this.filterFluid.isEmpty() ? List.of() : List.of(this.filterFluid))
            : ControlValveBlockEntity.resolveFluids(this.filterItem);
        if (fluids.isEmpty()) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        for (FluidStack fluid : fluids) {
            if (!lines.isEmpty()) lines.add(Component.empty());
            lines.addAll(getFilterTooltip(fluid, flag));
        }
        guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 红石锁定：流速为 0 且不可编辑
        boolean locked = this.isLocked();
        this.valueBox.setEditable(!locked);
        if (locked) {
            this.slider.setValue(0);
            this.valueBox.setValue("0");
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int fx = this.leftPos + FILTER_X;
        int fy = this.topPos + FILTER_Y;
        // 悬停时叠加与物品槽一致的白色半透明高亮，让它看起来像可放入的格子。
        // 高亮的 z 为 0，必须画在物品 / 流体图标之前，否则会被它们盖住。
        boolean hovered = this.isHovering(FILTER_X, FILTER_Y, 16, 16, mouseX, mouseY);
        if (hovered) {
            guiGraphics.fillGradient(fx, fy, fx + 16, fy + 16, 0x80FFFFFF, 0x80FFFFFF);
        }
        // 过滤格（非物品槽）：放入过滤器时只画过滤器本体，其中的流体改以流体图标呈现，否则画白名单流体贴图
        FluidStack displayed = this.displayedFilterFluid();
        if (this.filterItem.isEmpty()) {
            if (!displayed.isEmpty()) {
                this.renderFluidSprite(guiGraphics, displayed, fx, fy, 16);
            }
        } else {
            // 顺序：过滤器本体 → 流体图标 → 黑名单屏障，屏障须盖在流体之上
            FilterItemRenderer.withBodyOnly(() -> guiGraphics.renderItem(this.filterItem, fx, fy));
            if (!displayed.isEmpty()) {
                this.renderFluidSprite(guiGraphics, displayed, fx + 4, fy + 4, 8);
            }
            if (FilterItemRenderer.isDenyList(this.filterItem)) {
                this.renderDenyBarrier(guiGraphics, fx, fy);
            }
        }
        // 红石锁定提示文字（滑动条内）
        if (locked) {
            Component text = Component.translatable("screen.anvilcraft.control_valve.redstone_locked");
            int tx = this.leftPos + (this.imageWidth - this.font.width(text)) / 2;
            guiGraphics.drawString(this.font, text, tx, this.topPos + 41, 0xFFFF5555, false);
        }
        // 数字输入框右边始终渲染单位 mB（原版样式）
        guiGraphics.drawString(this.font, "mB", this.leftPos + 115, this.topPos + 59, 0x3F3F3F, false);
        guiGraphics.drawString(this.font, "mB", this.leftPos + 114, this.topPos + 58, 0xFFFFFF, false);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.renderFilterFluidTooltip(guiGraphics, mouseX, mouseY);
    }
}
