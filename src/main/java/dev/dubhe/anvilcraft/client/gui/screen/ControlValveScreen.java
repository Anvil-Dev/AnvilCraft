package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.FluidRateSlider;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.ControlValveMenu;
import dev.dubhe.anvilcraft.network.ControlValveFilterPacket;
import dev.dubhe.anvilcraft.network.ControlValveUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class ControlValveScreen extends AbstractContainerScreen<ControlValveMenu> implements IGhostIngredientScreen {
    public static final Identifier BACKGROUND = SharedTextures.bg("machine", "control_valve");
    public static final Identifier BUTTON_MAX = SharedTextures.textureGui("misc/slider_like/button_max");
    public static final Identifier BUTTON_ADD = SharedTextures.textureGui("misc/slider_like/button_add");
    public static final Identifier BUTTON_MINUS = SharedTextures.textureGui("misc/slider_like/button_minus");
    public static final Identifier BUTTON_MIN = SharedTextures.textureGui("misc/slider_like/button_min");

    private static final int FILTER_X = ControlValveMenu.FILTER_X;
    private static final int FILTER_Y = ControlValveMenu.FILTER_Y;
    private static final int FILTER_GHOST_ID = 0;

    private int value = ControlValveBlockEntity.MAX_RATE;
    private FluidStack filter = FluidStack.EMPTY;
    private @Nullable FluidRateSlider slider;
    private @Nullable EditBox valueBox;
    private boolean updatingValueBox;

    public ControlValveScreen(ControlValveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;

        int offsetX = this.leftPos;
        int offsetY = this.topPos;
        this.slider = new FluidRateSlider(offsetX + 8, offsetY + 41, 160, this::onSliderChange);
        this.valueBox = new EditBox(this.font, offsetX + 51, offsetY + 58, 28, 8, Component.literal("value"));
        this.valueBox.setCanLoseFocus(true);
        this.valueBox.setTextColor(-1);
        this.valueBox.setTextColorUneditable(-1);
        this.valueBox.setBordered(false);
        this.valueBox.setMaxLength(4);
        this.valueBox.setResponder(this::onValueInput);

        this.addRenderableWidget(new TexturedButton(
            offsetX + 8,
            offsetY + 53,
            16,
            16,
            ControlValveScreen.BUTTON_MIN,
            16,
            16,
            32,
            _ -> this.updateSlider(0)
        ));
        this.addRenderableWidget(new TexturedButton(
            offsetX + 26,
            offsetY + 53,
            16,
            16,
            ControlValveScreen.BUTTON_MINUS,
            16,
            16,
            32,
            _ -> this.stepSlider(-1)
        ));
        this.addRenderableWidget(this.slider);
        this.addRenderableWidget(new TexturedButton(
            offsetX + 134,
            offsetY + 53,
            16,
            16,
            ControlValveScreen.BUTTON_ADD,
            16,
            16,
            32,
            _ -> this.stepSlider(1)
        ));
        this.addRenderableWidget(new TexturedButton(
            offsetX + 152,
            offsetY + 53,
            16,
            16,
            ControlValveScreen.BUTTON_MAX,
            16,
            16,
            32,
            _ -> this.updateSlider(FluidRateSlider.MAX)
        ));
        this.addRenderableWidget(this.valueBox);
        this.setValue(this.value);
    }

    public void setValue(int value) {
        this.value = Math.clamp(value, 0, FluidRateSlider.MAX);
        if (this.slider != null) {
            this.slider.setExactValue(this.value);
            this.value = this.slider.getValue();
        }
        this.setValueBoxText(this.value);
    }

    public void setFilter(int index, FluidStack fluid) {
        if (index != 0) return;
        this.filter = fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(1);
    }

    private void setValueBoxText(int value) {
        if (this.valueBox == null) return;
        this.updatingValueBox = true;
        this.valueBox.setValue(Integer.toString(value));
        this.updatingValueBox = false;
    }

    private void updateSlider(int value) {
        if (this.isLocked() || this.slider == null) return;
        this.slider.setValueWithUpdate(value);
    }

    private void stepSlider(int direction) {
        if (this.isLocked() || this.slider == null) return;
        this.slider.step(direction);
    }

    private boolean isLocked() {
        ControlValveBlockEntity blockEntity = this.getMenu().getBlockEntity();
        return blockEntity != null && blockEntity.isLocked();
    }

    private void onSliderChange(int value) {
        if (this.isLocked()) return;
        this.value = value;
        this.setValueBoxText(value);
        ClientPacketDistributor.sendToServer(new ControlValveUpdatePacket(value));
    }

    private void onValueInput(String text) {
        if (this.updatingValueBox || this.isLocked() || this.slider == null) return;
        if (text.isEmpty()) return;
        if (!text.matches("^[0-9]+$")) {
            this.setValueBoxText(this.value);
            return;
        }
        int parsed = Integer.parseInt(text);
        this.slider.setExactValue(parsed);
        this.value = this.slider.getValue();
        if (parsed != this.value) {
            this.setValueBoxText(this.value);
        }
        ClientPacketDistributor.sendToServer(new ControlValveUpdatePacket(this.value));
    }

    private static FluidStack fluidOf(ItemStack stack) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
        ItemStack single = stack.copyWithCount(1);
        ResourceHandler<FluidResource> handler = single.getCapability(
            Capabilities.Fluid.ITEM,
            ItemAccess.forStack(single)
        );
        if (handler == null) return FluidStack.EMPTY;
        for (int i = 0; i < handler.size(); i++) {
            FluidResource resource = handler.getResource(i);
            if (resource.isEmpty() || handler.getAmountAsInt(i) <= 0) continue;
            return resource.toStack(1);
        }
        return FluidStack.EMPTY;
    }

    private void sendFilter(FluidStack fluid) {
        ClientPacketDistributor.sendToServer(new ControlValveFilterPacket(0, fluid));
    }

    @Override
    public Collection<Integer> getGhostSlots() {
        return List.of(ControlValveScreen.FILTER_GHOST_ID);
    }

    @Override
    public @Nullable Rect2i getGhostSlotArea(int slotIndex) {
        return slotIndex == ControlValveScreen.FILTER_GHOST_ID ? new Rect2i(
            ControlValveScreen.FILTER_X, ControlValveScreen.FILTER_Y, 16, 16) : null;
    }

    @Override
    public void acceptGhost(Slot slot, ItemStack ingredient) {
        this.sendFilter(ControlValveScreen.fluidOf(ingredient));
    }

    @Override
    public void acceptFluidGhost(int slotIndex, FluidStack fluid) {
        if (slotIndex != ControlValveScreen.FILTER_GHOST_ID) return;
        this.sendFilter(fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(1));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (event.button() == 0 && this.isHovering(
            ControlValveScreen.FILTER_X, ControlValveScreen.FILTER_Y, 16, 16, event.x(), event.y())) {
            this.sendFilter(ControlValveScreen.fluidOf(this.getMenu().getCarried()));
            return true;
        }
        if (event.button() == 0 && !this.isLocked() && this.slider != null) {
            this.slider.onClick(event, handled);
        }
        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!this.isLocked() && this.slider != null) {
            this.slider.onDrag(event, dragX, dragY);
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.slider != null) this.slider.onReleased();
        return super.mouseReleased(event);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            ControlValveScreen.BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            this.getImageHeight(),
            256,
            256
        );
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        boolean locked = this.isLocked();
        if (this.slider != null) this.slider.active = !locked;
        if (this.valueBox != null) this.valueBox.setEditable(!locked);

        super.extractContents(graphics, mouseX, mouseY, a);
        int fx = this.leftPos + ControlValveScreen.FILTER_X;
        int fy = this.topPos + ControlValveScreen.FILTER_Y;
        if (!this.filter.isEmpty()) {
            this.extractFluidSwatch(graphics, this.filter, fx, fy);
        }
        if (this.isHovering(ControlValveScreen.FILTER_X, ControlValveScreen.FILTER_Y, 16, 16, mouseX, mouseY)) {
            graphics.fill(fx, fy, fx + 16, fy + 16, 0x80FFFFFF);
        }
        if (locked) {
            Component text = Component.translatable("screen.anvilcraft.control_valve.redstone_locked");
            graphics.text(
                this.font,
                text,
                this.leftPos + (this.getImageWidth() - this.font.width(text)) / 2,
                this.topPos + 41,
                0xFFFF5555,
                false
            );
        }
        graphics.text(this.font, "mB", this.leftPos + 115, this.topPos + 59, 0xFF3F3F3F, false);
        graphics.text(this.font, "mB", this.leftPos + 114, this.topPos + 58, 0xFFFFFFFF, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!this.filter.isEmpty() && this.getMenu().getCarried().isEmpty()
            && this.isHovering(ControlValveScreen.FILTER_X, ControlValveScreen.FILTER_Y, 16, 16, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, this.filter.getHoverName(), mouseX, mouseY);
        }
    }

    private void extractFluidSwatch(GuiGraphicsExtractor graphics, FluidStack fluid, int x, int y) {
        FluidResource resource = FluidResource.of(fluid);
        FluidModel model = FluidRenderHelper.getModel(
            Minecraft.getInstance().getModelManager().getFluidStateModelSet(),
            resource.getFluid()
        );
        var tintSource = model.fluidTintSource();
        if (tintSource == null) return;
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        int tint = tintSource.colorAsStack(resource.toStack(1));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 16, 16, tint);
    }
}
