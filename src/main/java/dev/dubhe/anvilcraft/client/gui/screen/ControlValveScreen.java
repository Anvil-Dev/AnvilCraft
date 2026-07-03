package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.gui.component.FluidRateSlider;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.ControlValveMenu;
import dev.dubhe.anvilcraft.network.ControlValveFilterPacket;
import dev.dubhe.anvilcraft.network.ControlValveUpdatePacket;
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
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.List;

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

    /** 客户端本地过滤流体缓存（服务端回传更新） */
    private FluidStack filterFluid = FluidStack.EMPTY;

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

    private void onSliderChange(int value) {
        if (isLocked()) {
            return;
        }
        this.valueBox.setValue(Integer.toString(value));
        PacketDistributor.sendToServer(new ControlValveUpdatePacket(value));
    }

    private void onValueInput(String text) {
        if (isLocked()) {
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
        // 物品拖入 → 尝试解析其流体（有桶物品的流体）
        FluidStack fluid = fluidOf(ingredient);
        PacketDistributor.sendToServer(new ControlValveFilterPacket(0, fluid));
    }

    @Override
    public void acceptFluidGhost(int slotIndex, FluidStack fluid) {
        PacketDistributor.sendToServer(new ControlValveFilterPacket(0, fluid));
    }

    /** 左键点击过滤格：手持桶→设为其流体；空手→清空。同时把左键转发给滑条以支持拖动。 */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isHovering(FILTER_X, FILTER_Y, 16, 16, mouseX, mouseY)) {
            ItemStack carried = this.getMenu().getCarried();
            FluidStack fluid = fluidOf(carried); // 空手或非桶 → EMPTY（清空）
            PacketDistributor.sendToServer(new ControlValveFilterPacket(0, fluid));
            return true;
        }
        if (button == 0 && !isLocked()) {
            this.slider.onClick(mouseX, mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!isLocked()) {
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

    /** 从桶类物品解析其所含流体。 */
    private static FluidStack fluidOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(stack.copyWithCount(1)).orElse(null);
        return handler == null ? FluidStack.EMPTY : handler.getFluidInTank(0);
    }

    /** 在 (x,y) 处画 16×16 的流体静态贴图（取自方块图集，带色调）。 */
    private void renderFluidSprite(GuiGraphics guiGraphics, FluidStack fluid, int x, int y) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(ext.getStillTexture(fluid));
        int tint = ext.getTintColor(fluid);
        guiGraphics.blit(x, y, 0, 16, 16, sprite, FastColor.ARGB32.red(tint) / 255f,
            FastColor.ARGB32.green(tint) / 255f, FastColor.ARGB32.blue(tint) / 255f, 1.0f);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 红石锁定：流速为 0 且不可编辑
        boolean locked = isLocked();
        this.valueBox.setEditable(!locked);
        if (locked) {
            this.slider.setValue(0);
            this.valueBox.setValue("0");
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int fx = this.leftPos + FILTER_X;
        int fy = this.topPos + FILTER_Y;
        // 过滤格（非物品槽）：绘制白名单流体贴图
        if (!this.filterFluid.isEmpty()) {
            renderFluidSprite(guiGraphics, this.filterFluid, fx, fy);
        }
        // 悬停时叠加与物品槽一致的白色半透明高亮，让它看起来像可放入的格子
        if (this.isHovering(FILTER_X, FILTER_Y, 16, 16, mouseX, mouseY)) {
            guiGraphics.fillGradient(fx, fy, fx + 16, fy + 16, 0x80FFFFFF, 0x80FFFFFF);
        }
        // 红石锁定提示文字（滑动条内）
        if (locked) {
            Component text = Component.translatable("screen.anvilcraft.control_valve.redstone_locked");
            int tx = this.leftPos + (this.imageWidth - this.font.width(text)) / 2;
            guiGraphics.drawString(this.font, text, tx, this.topPos + 41, 0xFFFF5555, false);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
