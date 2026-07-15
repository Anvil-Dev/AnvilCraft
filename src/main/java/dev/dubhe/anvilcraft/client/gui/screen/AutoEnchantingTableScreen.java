package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.block.workstation.AutoEnchantingTableBlock;
import dev.dubhe.anvilcraft.client.gui.component.AutoEnchantingTableButton;
import dev.dubhe.anvilcraft.client.gui.component.FluidDisplayWidget;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.AutoEnchantingTableMenu;
import dev.dubhe.anvilcraft.network.AutoEnchantingTableSyncPacket;
import dev.dubhe.anvilcraft.util.TickDebouncer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class AutoEnchantingTableScreen extends AbstractContainerScreen<AutoEnchantingTableMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "auto_enchanting_table");

    @Nullable
    private EditBox editBox;
    @Nullable
    private TickDebouncer editBoxTickDebouncer;
    @Nullable
    private final AutoEnchantingTableButton[] buttons = new AutoEnchantingTableButton[10];
    private List<Holder<Enchantment>> enchantmentList = new ObjectArrayList<>();
    private int currentIndex = 0;
    private int scrollOffset = 0;
    private boolean draggedArea = false;
    private int errorCooldown = 0;
    private final Set<Holder<Enchantment>> selectedEnchantments = new ObjectOpenHashSet<>();
    private ItemStack finishItem = ItemStack.EMPTY;

    public AutoEnchantingTableScreen(AutoEnchantingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        menu.getBlockEntity().registerUpdateListener(() -> {
            this.addWidget(0);
            this.selectedEnchantments.clear();
            this.scrollOffset = 0;
        });
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            IdMap<Holder<Enchantment>> idMap = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
            this.selectedEnchantments.clear();
            for (int id : this.menu.getBlockEntity().getSelectedEnchantmentSet()) {
                Holder<Enchantment> enchantmentHolder = idMap.byId(id);
                if (enchantmentHolder != null) {
                    this.selectedEnchantments.add(enchantmentHolder);
                }
            }
        }
        ItemStack itemStack = this.menu.getBlockEntity().getItems().getFirst().copyWithCount(1);
        if (!itemStack.isEmpty()) {
            ItemStack enchantedBook = Items.ENCHANTED_BOOK.getDefaultInstance();
            for (Holder<Enchantment> selectedEnchantment : this.selectedEnchantments) {
                enchantedBook.enchant(selectedEnchantment, selectedEnchantment.value().getMaxLevel());
            }
            AutoEnchantingTableBlockEntity.applyEnchantment(itemStack, enchantedBook);
            this.finishItem = itemStack.copyWithCount(1);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.addRenderableWidget(
            new FluidDisplayWidget(
                this.leftPos + 151, this.topPos + 16,
                18, 56,
                this.menu.getBlockEntity().getFluidHandler(),
                (fluidHandler) -> Component.translatable(
                    "screen.anvilcraft.auto_enchanting_table.fluid_display",
                    fluidHandler.getAmountAsInt(0) + "/" + fluidHandler.getCapacityAsInt(0, FluidResource.EMPTY)
                )
            )
        );
        this.editBox = this.addRenderableWidget(
            new EditBox(this.font, this.leftPos + 46, this.topPos + 17, 99, 12, Component.empty())
        );
        this.editBoxTickDebouncer = new TickDebouncer(20, () -> this.addWidget(0));
        this.editBox.setResponder((_) -> this.editBoxTickDebouncer.trigger());

        this.addWidget(0);
    }

    private void addWidget(int startIndex) {
        if (startIndex < 0) {
            return;
        }
        for (int i = 0; i < this.buttons.length; i++) {
            AutoEnchantingTableButton button = this.buttons[i];
            if (button != null) {
                this.removeWidget(button);
            }
            this.buttons[i] = null;
        }
        this.currentIndex = startIndex;
        this.enchantmentList = this.menu.getEnchantmentList().stream().filter((holder) -> {
            if (this.editBox != null) {
                String enchantmentName = this.editBox.getValue();
                if (enchantmentName.isBlank()) {
                    return true;
                }
                return holder.value().description().getString().contains(enchantmentName);
            }
            return true;
        }).toList();
        if (startIndex < this.enchantmentList.size()) {
            int index = 0;
            for (int i = startIndex; i < this.enchantmentList.size() && i < startIndex + 10; i++) {
                Holder<Enchantment> holder = this.enchantmentList.get(i);
                AutoEnchantingTableButton button = this.getAutoEnchantingTableButton(index, holder);
                this.buttons[index] = button;
                this.addRenderableWidget(button);
                index++;
            }
        }
    }

    private AutoEnchantingTableButton getAutoEnchantingTableButton(int index, Holder<Enchantment> enchantment) {
        AutoEnchantingTableButton button = new AutoEnchantingTableButton(
            this.leftPos + 47 + 18 * (index % 5), this.topPos + 32 + 18 * (index / 5),
            18, 18,
            SharedTextures.SWITCH_TABLE_BUTTON,
            enchantment,
            18, 54,
            new int[]{0, 18, 36},
            (btn) -> {
                if (btn.isSelected()) {
                    this.selectedEnchantments.remove(btn.getHolder());
                    ItemStack itemStack = this.menu.getBlockEntity().getItems().getFirst().copyWithCount(1);
                    if (!itemStack.isEmpty()) {
                        ItemStack enchantedBook = Items.ENCHANTED_BOOK.getDefaultInstance().copyWithCount(1);
                        for (Holder<Enchantment> selectedEnchantment : this.selectedEnchantments) {
                            enchantedBook.enchant(selectedEnchantment, selectedEnchantment.value().getMaxLevel());
                        }
                        AutoEnchantingTableBlockEntity.applyEnchantment(itemStack, enchantedBook);
                        this.finishItem = itemStack.copyWithCount(1);
                    }
                    btn.setSelected(!btn.isSelected());
                    return;
                }
                int totalLevel = this.selectedEnchantments.stream()
                    .mapToInt((holder) -> holder.value().getMaxLevel())
                    .reduce(Integer::sum)
                    .orElse(0) + btn.getHolder().value().getMaxLevel();
                ClientLevel level = Minecraft.getInstance().level;
                if (level != null) {
                    if (totalLevel <= this.getBookShelf(level, this.menu.getBlockEntity().getBlockPos())
                        && totalLevel * 400 <= this.menu.getBlockEntity().getFluidHandler().getCapacityAsInt(0, FluidResource.EMPTY)) {
                        if (!btn.isSelected()) {
                            ItemStack itemStack = this.menu.getBlockEntity().getItems().getFirst().copyWithCount(1);
                            if (!itemStack.isEmpty()) {
                                ItemStack enchantedBook = Items.ENCHANTED_BOOK.getDefaultInstance().copyWithCount(1);
                                for (Holder<Enchantment> selectedEnchantment : this.selectedEnchantments) {
                                    enchantedBook.enchant(selectedEnchantment, selectedEnchantment.value().getMaxLevel());
                                }
                                enchantedBook.enchant(btn.getHolder(), btn.getHolder().value().getMaxLevel());
                                AutoEnchantingTableBlockEntity.applyEnchantment(itemStack, enchantedBook);
                                this.finishItem = itemStack.copyWithCount(1);
                            }
                            this.selectedEnchantments.add(btn.getHolder());
                        }
                        btn.setSelected(!btn.isSelected());
                    } else {
                        this.errorCooldown = 80;
                    }
                }
            },
            List.of()
        );
        if (Minecraft.getInstance().level != null) {
            if (this.selectedEnchantments.contains(button.getHolder())) {
                button.setSelected(true);
            }
        }
        return button;
    }

    private int getBookShelf(Level level, BlockPos pos) {
        float bookcases = 0;
        for (BlockPos offset : AutoEnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                bookcases += level.getBlockState(pos.offset(offset)).getEnchantPowerBonus(level, pos.offset(offset));
            }
        }
        return (int) bookcases;
    }

    private boolean mouseInListArea(double mouseX, double mouseY) {
        return mouseX >= this.leftPos + 47
            && mouseX <= this.leftPos + 144
            && mouseY >= this.topPos + 32
            && mouseY <= this.topPos + 68;
    }

    private boolean mouseInScrollBarArea(double mouseX, double mouseY) {
        return mouseX >= this.leftPos + 140
            && mouseX <= this.leftPos + 144
            && mouseY >= this.topPos + 32
            && mouseY <= this.topPos + 68;
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {

        return super.getTooltipFromContainerItem(itemStack);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.mouseInListArea(x, y)) {
            if (this.errorCooldown > 0) {
                return true;
            }
            if (this.currentIndex + 10 >= this.enchantmentList.size() && scrollY < 0) {
                return true;
            }
            if (this.enchantmentList.size() > 10) {
                int newIndex = Mth.clamp(this.currentIndex + (int) -scrollY * 10, 0, this.enchantmentList.size() - 1);
                this.addWidget(newIndex);
                this.scrollOffset = newIndex;
                return true;
            }
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        this.draggedArea = false;
        if (event.button() == 0) {
            if (this.mouseInScrollBarArea(event.x(), event.y())) {
                if (this.errorCooldown <= 0) {
                    this.draggedArea = true;
                }
            }
        } else if (event.button() == 1) {
            if (this.editBox != null && this.editBox.isMouseOver(event.x(), event.y())) {
                this.editBox.setValue("");
                this.addWidget(0);
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        int trackY = this.topPos + 32;
        int trackHeight = 36 - 12;
        if (this.draggedArea) {
            int size = this.enchantmentList.size();
            int maxIndex = size > 10 ? (size - 1) / 10 * 10 : 0;
            if (maxIndex > 0) {
                int continuousIndex = Mth.clamp((int) ((event.y() - trackY) * (double) maxIndex / trackHeight), 0, maxIndex);
                this.scrollOffset = continuousIndex;
                int newIndex = Mth.clamp((int) (Math.round(continuousIndex / 10.0) * 10), 0, maxIndex);
                this.addWidget(newIndex);
                return true;
            }
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.draggedArea = false;
        return super.mouseReleased(event);
    }

    @Override
    protected void containerTick() {
        if (this.editBoxTickDebouncer != null) {
            this.editBoxTickDebouncer.tick();
        }
        if (this.errorCooldown > 0) {
            this.errorCooldown--;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        AutoEnchantingTableBlockEntity blockEntity = this.menu.getBlockEntity();
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (Minecraft.getInstance().level != null) {
            IdMap<Holder<Enchantment>> idMap = Minecraft.getInstance().level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .asHolderIdMap();
            if (connection != null) {
                connection.send(new AutoEnchantingTableSyncPacket(
                    blockEntity.getBlockPos(),
                    this.selectedEnchantments.stream().map(idMap::getId).toList()
                ));
            }
        }
        return true;
    }

    @Override
    public void onClose() {
        super.onClose();
        AutoEnchantingTableBlockEntity blockEntity = this.menu.getBlockEntity();
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (Minecraft.getInstance().level != null) {
            IdMap<Holder<Enchantment>> idMap = Minecraft.getInstance().level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .asHolderIdMap();
            if (connection != null) {
                connection.send(new AutoEnchantingTableSyncPacket(
                    blockEntity.getBlockPos(),
                    this.selectedEnchantments.stream().map(idMap::getId).toList()
                ));
            }
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        if (this.errorCooldown >  0) {
            graphics.fill(this.leftPos + 47, this.topPos + 32, this.leftPos + 137, this.topPos + 68, ARGB.color(128, 255, 0, 0));
        }
        int size = this.enchantmentList.size();
        if (size > 10) {
            int maxY = this.topPos + 32 + 36 - 12;
            int trackHeight = 36 - 12;
            int maxIndex = (size - 1) / 10 * 10;
            int scrollY = Mth.clamp(this.topPos + 32 + (int) ((float) this.scrollOffset * trackHeight / maxIndex), this.topPos + 32, maxY);
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                SharedTextures.SWITCH_TABLE_SLIDER,
                this.leftPos + 140,
                scrollY,
                0, 0,
                4, 12,
                8, 12
            );
        }
        if (this.menu.getBlockEntity().getItems().get(1).isEmpty() && !this.finishItem.isEmpty()) {
            graphics.item(this.finishItem, this.leftPos + 7, this.topPos + 52);
            graphics.fill(this.leftPos + 7, this.topPos + 52, this.leftPos + 23, this.topPos + 68, 0x99777777);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND,
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
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        int x = (this.imageWidth - this.font.width(this.title)) / 2;
        graphics.text(this.font, this.title, x, 2, -12566464, false);
        if (this.errorCooldown > 0) {
            MutableComponent text = Component.translatable("screen.anvilcraft.auto_enchanting_table.out_of_limit");
            x = (this.imageWidth - this.font.width(text)) / 2;
            graphics.text(this.font, text, x, -15, ARGB.color(255, 0, 0), false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack item = this.hoveredSlot.getItem();
            if (
                this.menu.getCarried().isEmpty()
                    || item.getTooltipImage()
                    .map(ClientTooltipComponent::create)
                    .map(ClientTooltipComponent::showTooltipWithItemInHand)
                    .orElse(false)
            ) {
                if (this.hoveredSlot.index != 37) {
                    graphics.setTooltipForNextFrame(
                        this.font,
                        this.getTooltipFromContainerItem(item),
                        item.getTooltipImage(),
                        item,
                        mouseX,
                        mouseY,
                        item.get(DataComponents.TOOLTIP_STYLE)
                    );
                }
            }
        }
        if (this.isHovering(7, 52, 16, 16, mouseX, mouseY)) {
            if (this.menu.getBlockEntity().getItems().get(1).isEmpty()) {
                if (!this.finishItem.isEmpty()) {
                    graphics.setTooltipForNextFrame(
                        this.font,
                        this.getTooltipFromContainerItem(this.finishItem),
                        this.finishItem.getTooltipImage(),
                        this.finishItem,
                        mouseX,
                        mouseY,
                        this.finishItem.get(DataComponents.TOOLTIP_STYLE)
                    );
                }
            }
        }
        if (this.isHovering(151, 16, 18, 56, mouseX, mouseY)) {
            graphics.tooltip(
                Minecraft.getInstance().font,
                List.of(
                    ClientTooltipComponent.create(
                        Component.translatable(
                            "screen.anvilcraft.auto_enchanting_table.fluid_display",
                            this.menu.getBlockEntity().getFluidHandler().getAmountAsInt(0)
                                + "/"
                                + this.menu.getBlockEntity().getFluidHandler().getCapacityAsInt(0, FluidResource.EMPTY)
                        ).getVisualOrderText()
                    )
                ),
                mouseX,
                mouseY,
                DefaultTooltipPositioner.INSTANCE,
                null
            );
        }
    }
}
