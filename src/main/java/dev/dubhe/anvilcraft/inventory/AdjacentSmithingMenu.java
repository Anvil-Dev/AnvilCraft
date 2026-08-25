package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.api.itemhandler.ReadOnlyItemHandlerWrapper;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.network.multiple.SmithingTemplatePackets;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import javax.annotation.Nullable;

/**
 * 支持从相邻容器临时借用锻造模板的菜单基类。
 */
public abstract class AdjacentSmithingMenu extends ItemCombinerMenu {
    private static final int TEMPLATE_SLOT = 0;
    private static final long REFRESH_INTERVAL = 5;

    private final Level templateLevel;
    private final Player menuPlayer;
    @Getter
    private List<ItemStack> adjacentTemplates = List.of();
    @Getter
    private List<ResourceLocation> favoriteTemplates = List.of();
    private ItemStack borrowedTemplateStack = ItemStack.EMPTY;
    private long nextRefreshTime;
    private boolean templateDataDirty = true;

    @Nullable
    private BlockPos tablePos;

    @Nullable
    private BorrowedTemplate borrowedTemplate;

    protected AdjacentSmithingMenu(
        MenuType<?> type,
        int containerId,
        Inventory playerInventory,
        ContainerLevelAccess access
    ) {
        super(type, containerId, playerInventory, access);
        this.templateLevel = playerInventory.player.level();
        this.menuPlayer = playerInventory.player;
        access.execute((level, pos) -> this.tablePos = pos.immutable());
    }

    /** 判断物品能否作为当前锻造台的模板。 */
    protected abstract boolean isUsableTemplate(ItemStack stack);

    public boolean isBorrowedTemplate(ItemStack stack) {
        return !this.borrowedTemplateStack.isEmpty() && stack.is(this.borrowedTemplateStack.getItem());
    }

    /** 接收服务端发来的模板面板数据。 */
    public void handleTemplateSync(
        List<ItemStack> templates,
        List<ResourceLocation> favorites,
        ItemStack borrowedTemplate
    ) {
        this.adjacentTemplates = templates.stream().map(ItemStack::copy).toList();
        this.favoriteTemplates = List.copyOf(favorites);
        this.borrowedTemplateStack = borrowedTemplate.copy();
    }

    /** 处理模板面板点击；所有容器状态都在服务端重新校验。 */
    public void handleTemplateAction(Player player, ResourceLocation template, boolean toggleFavorite) {
        if (!(player instanceof ServerPlayer serverPlayer) || player != this.menuPlayer) return;
        this.refreshTemplateCatalog();
        if (!this.containsTemplate(template)) return;
        if (toggleFavorite) {
            SmithingTemplateFavorites favorites = player.getData(ModDataAttachments.SMITHING_TEMPLATE_FAVORITES);
            player.setData(ModDataAttachments.SMITHING_TEMPLATE_FAVORITES, favorites.toggle(template));
            this.refreshTemplateCatalog();
            this.syncTemplateData(serverPlayer);
            return;
        }
        if (this.isBorrowedTemplateId(template)) {
            this.returnBorrowedTemplate(true);
            this.refreshTemplateCatalog();
            this.syncTemplateData(serverPlayer);
            return;
        }
        this.borrowTemplate(serverPlayer, template);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(this.menuPlayer instanceof ServerPlayer serverPlayer) || this.tablePos == null) return;
        long gameTime = this.templateLevel.getGameTime();
        if (gameTime < this.nextRefreshTime) return;
        this.nextRefreshTime = gameTime + REFRESH_INTERVAL;
        this.refreshTemplateCatalog();
        this.syncTemplateData(serverPlayer);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!this.borrowedTemplateStack.isEmpty() && slotId == TEMPLATE_SLOT) return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!this.borrowedTemplateStack.isEmpty()
            && index > this.getResultSlot()
            && this.isUsableTemplate(this.getSlot(index).getItem())) {
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, index);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (!this.borrowedTemplateStack.isEmpty() && slot == this.getSlot(TEMPLATE_SLOT)) return false;
        return super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void removed(Player player) {
        if (!this.templateLevel.isClientSide && this.borrowedTemplate != null) {
            this.returnBorrowedTemplate(false);
        }
        super.removed(player);
    }

    private void refreshTemplateCatalog() {
        if (this.tablePos == null || this.templateLevel.isClientSide) return;
        List<ResourceLocation> favorites = this.menuPlayer
            .getData(ModDataAttachments.SMITHING_TEMPLATE_FAVORITES)
            .templates();
        List<ItemStack> templates = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            IItemHandler handler = this.getItemHandler(this.tablePos.relative(direction));
            this.collectTemplates(handler, templates);
        }
        if (!this.borrowedTemplateStack.isEmpty()) {
            addUniqueTemplate(templates, this.borrowedTemplateStack);
        }
        templates.sort(Comparator
            .comparingInt((ItemStack stack) -> favoriteIndex(favorites, itemId(stack)))
            .thenComparingInt(stack -> templateIndex(this.adjacentTemplates, itemId(stack))));
        if (!sameTemplates(this.adjacentTemplates, templates) || !this.favoriteTemplates.equals(favorites)) {
            this.adjacentTemplates = templates;
            this.favoriteTemplates = List.copyOf(favorites);
            this.templateDataDirty = true;
        }
    }

    private void collectTemplates(@Nullable IItemHandler handler, List<ItemStack> templates) {
        if (handler == null) return;
        IntFunction<ItemStack> extractor;
        if (handler instanceof ReadOnlyItemHandlerWrapper wrapper) {
            extractor = slot -> wrapper.extractItemBypass(slot, 1, true);
        } else {
            extractor = slot -> handler.extractItem(slot, 1, true);
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty() || !this.isUsableTemplate(stack)) continue;
            ItemStack simulated = extractor.apply(slot);
            if (!ItemStack.isSameItemSameComponents(stack.copyWithCount(1), simulated)) continue;
            addUniqueTemplate(templates, stack);
        }
    }

    private void borrowTemplate(ServerPlayer player, ResourceLocation template) {
        if (this.tablePos == null) return;
        if (this.isBorrowedTemplateId(template)) return;
        ItemStack currentTemplate = this.inputSlots.getItem(TEMPLATE_SLOT);
        if (this.borrowedTemplate == null && !currentTemplate.isEmpty()) return;
        if (this.borrowedTemplate != null
            && !matchesTemplate(currentTemplate, this.borrowedTemplate.template())) {
            this.borrowedTemplate = null;
            this.borrowedTemplateStack = ItemStack.EMPTY;
            this.templateDataDirty = true;
            if (!currentTemplate.isEmpty()) {
                this.syncTemplateData(player);
                return;
            }
        }
        ExtractedTemplate extracted = this.extractTemplate(template);
        if (extracted == null) {
            this.refreshTemplateCatalog();
            this.syncTemplateData(player);
            return;
        }
        if (this.borrowedTemplate != null) {
            this.returnBorrowedTemplate(true);
        }
        this.borrowedTemplate = new BorrowedTemplate(
            extracted.sourcePos(),
            extracted.sourceSlot(),
            template,
            extracted.sourceBlockEntity()
        );
        this.borrowedTemplateStack = extracted.stack().copy();
        this.inputSlots.setItem(TEMPLATE_SLOT, extracted.stack());
        this.templateDataDirty = true;
        this.refreshTemplateCatalog();
        this.syncTemplateData(player);
    }

    @Nullable
    private ExtractedTemplate extractTemplate(ResourceLocation template) {
        if (this.tablePos == null) return null;
        for (Direction direction : Direction.values()) {
            BlockPos sourcePos = this.tablePos.relative(direction);
            IItemHandler handler = this.getItemHandler(sourcePos);
            if (handler == null) continue;
            BiFunction<Integer, Boolean, ItemStack> extractor;
            if (handler instanceof ReadOnlyItemHandlerWrapper wrapper) {
                extractor = (slot, simulate) -> wrapper.extractItemBypass(slot, 1, simulate);
            } else {
                extractor = (slot, simulate) -> handler.extractItem(slot, 1, simulate);
            }
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!matchesTemplate(stack, template) || !this.isUsableTemplate(stack)) continue;
                ItemStack simulated = extractor.apply(slot, true);
                if (!matchesTemplate(simulated, template) || !this.isUsableTemplate(simulated)) continue;
                ItemStack extracted = extractor.apply(slot, false);
                if (matchesTemplate(extracted, template) && this.isUsableTemplate(extracted)) {
                    return new ExtractedTemplate(
                        sourcePos.immutable(),
                        slot,
                        extracted,
                        this.templateLevel.getBlockEntity(sourcePos)
                    );
                }
                this.returnToHandlerOrDrop(handler, slot, extracted);
            }
        }
        return null;
    }

    private void returnBorrowedTemplate(boolean notifyMenu) {
        BorrowedTemplate origin = this.borrowedTemplate;
        if (origin == null) return;
        final ItemStack stack = this.inputSlots.getItem(TEMPLATE_SLOT);
        this.borrowedTemplate = null;
        this.borrowedTemplateStack = ItemStack.EMPTY;
        this.templateDataDirty = true;
        if (stack.isEmpty() || !matchesTemplate(stack, origin.template())) return;
        if (notifyMenu) {
            this.inputSlots.setItem(TEMPLATE_SLOT, ItemStack.EMPTY);
        } else {
            this.inputSlots.removeItemNoUpdate(TEMPLATE_SLOT);
        }
        IItemHandler handler = this.templateLevel.getBlockEntity(origin.sourcePos()) == origin.sourceBlockEntity()
            ? this.getItemHandler(origin.sourcePos())
            : null;
        this.returnToHandlerOrDrop(handler, origin.sourceSlot(), stack);
    }

    @Nullable
    private IItemHandler getItemHandler(BlockPos pos) {
        IItemHandler handler = this.templateLevel.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler != null) return handler;
        if (this.templateLevel.getBlockEntity(pos) instanceof Container container) {
            return new InvWrapper(container);
        }
        return null;
    }

    private void returnToHandlerOrDrop(@Nullable IItemHandler handler, int preferredSlot, ItemStack stack) {
        ItemStack remainder = stack;
        if (handler != null) {
            if (preferredSlot >= 0 && preferredSlot < handler.getSlots()) {
                if (handler instanceof ReadOnlyItemHandlerWrapper wrapper) {
                    remainder = wrapper.insertItemBypass(preferredSlot, remainder, false);
                } else {
                    remainder = handler.insertItem(preferredSlot, remainder, false);
                }
            }
            if (!remainder.isEmpty() && !(handler instanceof ReadOnlyItemHandlerWrapper)) {
                for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
                    if (slot == preferredSlot) continue;
                    remainder = handler.insertItem(slot, remainder, false);
                }
            }
        }
        if (remainder.isEmpty()) return;
        BlockPos dropPos = this.tablePos == null ? this.menuPlayer.blockPosition() : this.tablePos;
        Containers.dropItemStack(
            this.templateLevel,
            dropPos.getX() + 0.5,
            dropPos.getY() + 1.0,
            dropPos.getZ() + 0.5,
            remainder
        );
    }

    private void syncTemplateData(ServerPlayer player) {
        if (!this.templateDataDirty) return;
        PacketDistributor.sendToPlayer(player, new SmithingTemplatePackets.Sync(
            this.containerId,
            this.adjacentTemplates,
            this.favoriteTemplates,
            this.borrowedTemplateStack
        ));
        this.templateDataDirty = false;
    }

    private boolean containsTemplate(ResourceLocation template) {
        return this.adjacentTemplates.stream().anyMatch(stack -> matchesTemplate(stack, template));
    }

    private boolean isBorrowedTemplateId(ResourceLocation template) {
        return !this.borrowedTemplateStack.isEmpty() && itemId(this.borrowedTemplateStack).equals(template);
    }

    private static void addUniqueTemplate(List<ItemStack> templates, ItemStack stack) {
        if (templates.stream().anyMatch(existing -> existing.is(stack.getItem()))) return;
        templates.add(stack.copyWithCount(1));
    }

    private static boolean sameTemplates(List<ItemStack> first, List<ItemStack> second) {
        if (first.size() != second.size()) return false;
        for (int index = 0; index < first.size(); index++) {
            if (!ItemStack.isSameItemSameComponents(first.get(index), second.get(index))) return false;
        }
        return true;
    }

    private static int favoriteIndex(List<ResourceLocation> favorites, ResourceLocation template) {
        int index = favorites.indexOf(template);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private static int templateIndex(List<ItemStack> templates, ResourceLocation template) {
        for (int index = 0; index < templates.size(); index++) {
            if (itemId(templates.get(index)).equals(template)) return index;
        }
        return Integer.MAX_VALUE;
    }

    private static boolean matchesTemplate(ItemStack stack, ResourceLocation template) {
        return !stack.isEmpty() && itemId(stack).equals(template);
    }

    private static ResourceLocation itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    private record BorrowedTemplate(
        BlockPos sourcePos,
        int sourceSlot,
        ResourceLocation template,
        @Nullable BlockEntity sourceBlockEntity
    ) {
    }

    private record ExtractedTemplate(
        BlockPos sourcePos,
        int sourceSlot,
        ItemStack stack,
        @Nullable BlockEntity sourceBlockEntity
    ) {
    }
}
