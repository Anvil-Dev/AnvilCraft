package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.block.TradingStationBlock;
import dev.dubhe.anvilcraft.block.state.DirectionVertical2PartHalf;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.TradingStationMenu;
import dev.dubhe.anvilcraft.inventory.container.FilterOnlyContainer;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

@Getter
public class TradingStationBlockEntity extends BlockEntity implements IItemHandlerHolder, MenuProvider, IDiskCloneable {
    public static final String OWNER_NBT_ID = "Owner";
    public static final String STORAGE_NBT_ID = "Items";
    public static final String FILTERS_NBT_ID = "Filters";
    public static final String ALLOW_PLAYER_NBT_ID = "AllowPlayer";
    public static final String ALLOW_VILLAGER_NBT_ID = "AllowVillager";

    private final ItemStackHandler handler = new ItemStackHandler(12) {
        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (!this.isItemValid(slot, stack) && !stack.isEmpty()) return;
            super.setStackInSlot(slot, stack);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (!TradingStationBlockEntity.this.canPlace(stack)) return false;
            return super.isItemValid(slot, stack);
        }
    };
    private final ItemStackHandler proxy = new ItemStackHandler(12) {
        @Override
        public void setSize(int size) {
            TradingStationBlockEntity.this.handler.setSize(size);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            TradingStationBlockEntity.this.handler.setStackInSlot(slot, stack);
        }

        @Override
        public int getSlots() {
            return TradingStationBlockEntity.this.handler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return TradingStationBlockEntity.this.handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!TradingStationBlockEntity.this.inputAllowed) return stack;
            return TradingStationBlockEntity.this.handler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!TradingStationBlockEntity.this.outputAllowed) return ItemStack.EMPTY;
            return TradingStationBlockEntity.this.handler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return TradingStationBlockEntity.this.handler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return TradingStationBlockEntity.this.handler.isItemValid(slot, stack);
        }
    };
    private final FilterOnlyContainer filters = new FilterOnlyContainer(this, 3) {
        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            super.deserializeNBT(provider, nbt);
            this.setChanged();
        }

        @Override
        public void setChanged() {
            TradingStationBlockEntity.popoutInvalidItems(
                TradingStationBlockEntity.this.getLevel(),
                TradingStationBlockEntity.this.getBlockPos(),
                TradingStationBlockEntity.this.handler
            );
            TradingStationBlockEntity.updateAndSend(TradingStationBlockEntity.this);
        }
    };
    private @Nullable UUID owner;
    private boolean playerAllowed = false;
    private boolean villagerAllowed = false;
    private boolean inputAllowed = false;
    private boolean outputAllowed = false;

    public TradingStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public DirectionVertical2PartHalf getPart() {
        return this.getBlockState().getValue(TradingStationBlock.HALF);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.owner != null) tag.putUUID(OWNER_NBT_ID, this.owner);
        tag.put(STORAGE_NBT_ID, this.handler.serializeNBT(registries));
        tag.put(FILTERS_NBT_ID, this.filters.serializeNBT(registries));
        tag.putBoolean(ALLOW_PLAYER_NBT_ID, this.isPlayerAllowed());
        tag.putBoolean(ALLOW_VILLAGER_NBT_ID, this.villagerAllowed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(OWNER_NBT_ID)) this.owner = tag.getUUID(OWNER_NBT_ID);
        this.handler.deserializeNBT(registries, tag.getCompound(STORAGE_NBT_ID));
        this.filters.deserializeNBT(registries, tag.getCompound(FILTERS_NBT_ID));
        this.playerAllowed = tag.getBoolean(ALLOW_PLAYER_NBT_ID);
        this.villagerAllowed = tag.getBoolean(ALLOW_VILLAGER_NBT_ID);
        TradingStationBlockEntity.popoutInvalidItems(this.getLevel(), this.getBlockPos(), this.handler);
        TradingStationBlockEntity.updateAndSend(this);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag data = super.getUpdateTag(registries);
        this.saveAdditional(data, registries);
        return data;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public @Nullable TradingStationMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (player.isSpectator()) return null;
        if (this.owner != null && !player.getGameProfile().getId().equals(this.owner)) return null;
        return new TradingStationMenu(ModMenuTypes.TRADING_STATION.get(), containerId, playerInventory, this);
    }

    @Override
    public void storeDiskData(CompoundTag tag) {
        if (this.owner != null) tag.putUUID(OWNER_NBT_ID, this.owner);
        if (this.level != null) {
            tag.put(FILTERS_NBT_ID, this.filters.serializeNBT(this.level.registryAccess()));
        }
        tag.putBoolean(ALLOW_PLAYER_NBT_ID, this.isPlayerAllowed());
        tag.putBoolean(ALLOW_VILLAGER_NBT_ID, this.villagerAllowed);
    }

    @Override
    public void applyDiskData(CompoundTag tag) {
        if (!tag.contains(OWNER_NBT_ID)) return;
        UUID owner = tag.getUUID(OWNER_NBT_ID);
        if (this.owner != null) {
            if (!this.owner.equals(owner)) return;
        } else {
            this.owner = owner;
        }

        if (this.level != null) {
            this.filters.deserializeNBT(this.level.registryAccess(), tag.getCompound(FILTERS_NBT_ID));
        }
        this.playerAllowed = tag.getBoolean(ALLOW_PLAYER_NBT_ID);
        this.villagerAllowed = tag.getBoolean(ALLOW_VILLAGER_NBT_ID);
        TradingStationBlockEntity.popoutInvalidItems(this.getLevel(), this.getBlockPos(), this.handler);
        TradingStationBlockEntity.updateAndSend(this);
    }

    @Override
    public ItemStackHandler getItemHandler() {
        return this.proxy;
    }

    public boolean canPlace(ItemStack stack) {
        return TradingStationBlockEntity.this.isProviding(stack)
               || TradingStationBlockEntity.this.isRequesting(stack);
    }

    public boolean isProviding(ItemStack stack) {
        ItemStack provide = this.filters.getItem(0);
        ItemStack provide1 = this.filters.getItem(1);
        if (provide.isEmpty() && provide1.isEmpty()) return false;
        return FilterContent.filter(provide, stack, !provide.getComponentsPatch().isEmpty())
               || FilterContent.filter(provide1, stack, !provide1.getComponentsPatch().isEmpty());
    }

    public boolean isRequesting(ItemStack stack) {
        ItemStack request = this.filters.getItem(2);
        if (request.isEmpty()) return false;
        return FilterContent.filter(request, stack, !request.getComponentsPatch().isEmpty());
    }

    public boolean tryTradingWithPlayer(ServerPlayer sp, InteractionHand hand) {
        // Only allow if player trading is enabled
        if (!this.isPlayerAllowed()) return false;

        ItemStack inHand = sp.getItemInHand(hand);
        // Ensure the item in hand matches the request filter
        ItemStack requesting = this.filters.getItem(2);
        if (requesting.isEmpty()) return false;
        if (!FilterContent.filter(requesting, inHand, !requesting.getComponentsPatch().isEmpty())) return false;

        // Determine which providing filter to use (slot 0 takes precedence)
        ItemStack providing = this.filters.getItem(0);
        if (providing.isEmpty()) providing = this.filters.getItem(1);
        if (providing.isEmpty()) return false;

        final int provideCount = providing.getCount();
        final int requestCount = requesting.getCount();

        // If player is in creative / has infinite materials, simply give them the provided items
        if (sp.hasInfiniteMaterials()) {
            ItemStack give = providing.copy();
            // Try to add to inventory, otherwise drop on ground
            if (!sp.getInventory().add(give)) sp.drop(give, false);
            return true;
        }

        // Non-creative: ensure the trading station has enough providing items
        Int2IntMap modifying = new Int2IntArrayMap();
        int remainingProvide = provideCount;
        for (int i = 0; i < this.handler.getSlots(); i++) {
            ItemStack stack = this.handler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (!FilterContent.filter(providing, stack, !providing.getComponentsPatch().isEmpty())) continue;
            int take = Math.min(remainingProvide, stack.getCount());
            modifying.put(i, take);
            remainingProvide -= take;
            if (remainingProvide <= 0) break;
        }
        if (remainingProvide > 0) return false; // not enough items to give

        // Ensure the player actually has enough items to give (based on request filter count)
        if (inHand.getCount() < requestCount) return false;

        // Check there is enough space in the handler to insert the requested items
        ItemStack toInsertPrototype = requesting.copy();
        toInsertPrototype.setCount(requestCount);
        int space = 0;
        for (int i = 0; i < this.handler.getSlots(); i++) {
            ItemStack stack = this.handler.getStackInSlot(i);
            if (stack.isEmpty()) {
                space += toInsertPrototype.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(stack, toInsertPrototype)) {
                space += stack.getMaxStackSize() - stack.getCount();
            }
            if (space >= requestCount) break;
        }
        if (space < requestCount) return false; // no space to accept player's item

        // Remove providing items from the handler according to the plan in 'modifying'
        for (Int2IntMap.Entry entry : modifying.int2IntEntrySet()) {
            int slot = entry.getIntKey();
            int amount = entry.getIntValue();
            ItemStack stack = this.handler.getStackInSlot(slot).copy();
            stack.shrink(amount);
            if (stack.getCount() <= 0) stack = ItemStack.EMPTY;
            this.handler.setStackInSlot(slot, stack);
        }

        // Insert the requested items into handler (merge into existing or empty slots)
        int remainingInsert = requestCount;
        for (int i = 0; i < this.handler.getSlots() && remainingInsert > 0; i++) {
            ItemStack stack = this.handler.getStackInSlot(i);
            if (stack.isEmpty()) {
                int put = Math.min(remainingInsert, toInsertPrototype.getMaxStackSize());
                this.handler.setStackInSlot(i, toInsertPrototype.copyWithCount(put));
                remainingInsert -= put;
            } else if (ItemStack.isSameItemSameComponents(stack, toInsertPrototype)) {
                int can = stack.getMaxStackSize() - stack.getCount();
                if (can <= 0) continue;
                int put = Math.min(can, remainingInsert);
                this.handler.setStackInSlot(i, stack.copyWithCount(stack.getCount() + put));
                remainingInsert -= put;
            }
        }

        if (remainingInsert > 0) {
            // This should not happen due to prior space check, but if it does, try to revert providing removal
            for (Int2IntMap.Entry entry : modifying.int2IntEntrySet()) {
                int slot = entry.getIntKey();
                int amount = entry.getIntValue();
                ItemStack current = this.handler.getStackInSlot(slot);
                if (current.isEmpty()) {
                    this.handler.setStackInSlot(slot, providing.copyWithCount(amount));
                } else if (ItemStack.isSameItemSameComponents(current, providing)) {
                    int newCount = Math.min(current.getCount() + amount, current.getMaxStackSize());
                    this.handler.setStackInSlot(slot, current.copyWithCount(newCount));
                } else {
                    // as a fallback, drop the items into the world
                    if (this.level != null) {
                        Block.popResourceFromFace(this.level, this.getBlockPos(), Direction.UP, providing.copyWithCount(amount));
                    }
                }
            }
            return false;
        }

        // Successfully accepted the player's items: shrink player's hand and give them the provided items
        ItemStack newInHand = inHand.copy();
        newInHand.shrink(requestCount);
        sp.setItemInHand(hand, newInHand.isEmpty() ? ItemStack.EMPTY : newInHand);

        ItemStack give = providing.copy();
        if (!sp.getInventory().add(give)) sp.drop(give, false);

        // Notify clients of change
        TradingStationBlockEntity.updateAndSend(this);
        return true;
    }

    public boolean isOwner(Player sp) {
        return sp.getGameProfile().getId().equals(this.owner);
    }

    public void setOwner(UUID owner) {
        Level level = this.getLevel();
        if (level == null) return;
        if (this.owner == null) this.owner = owner;
        TradingStationBlockEntity.updateAndSend(this);
    }

    @Override
    public void setRemoved() {
        this.owner = null;
    }

    public boolean isPlayerAllowed() {
        if (TradingStationBlockEntity.isProvideMultiple(this.filters)) return false;
        return this.playerAllowed;
    }

    public void setPlayerAllowed(boolean playerAllowed) {
        Level level = this.getLevel();
        if (level == null) return;
        this.playerAllowed = playerAllowed;
        TradingStationBlockEntity.updateAndSend(this);
    }

    public void setVillagerAllowed(boolean villagerAllowed) {
        Level level = this.getLevel();
        if (level == null) return;
        this.villagerAllowed = villagerAllowed;
        TradingStationBlockEntity.updateAndSend(this);
    }

    public void setInputAllowed(boolean inputAllowed) {
        Level level = this.getLevel();
        if (level == null) return;
        this.inputAllowed = inputAllowed;
        TradingStationBlockEntity.updateAndSend(this);
    }

    public void setOutputAllowed(boolean outputAllowed) {
        Level level = this.getLevel();
        if (level == null) return;
        this.outputAllowed = outputAllowed;
        TradingStationBlockEntity.updateAndSend(this);
    }

    public static void popoutInvalidItems(@Nullable Level level, BlockPos pos, ItemStackHandler handler) {
        if (level == null || level.isClientSide) return;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (handler.isItemValid(i, stack)) continue;
            handler.setStackInSlot(i, ItemStack.EMPTY);
            Block.popResourceFromFace(level, pos, Direction.UP, stack);
        }
    }

    public static void updateAndSend(TradingStationBlockEntity be) {
        be.setChanged();
        Level level = be.getLevel();
        if (level == null) return;
        level.sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), Block.UPDATE_CLIENTS);
    }

    public static boolean isProvideMultiple(FilterOnlyContainer filters) {
        int count = 0;
        ItemStack provide = filters.getItem(0);
        if (!provide.isEmpty()) {
            if (provide.has(ModComponents.FILTER_CONTENT)) {
                FilterContent content = Objects.requireNonNull(provide.get(ModComponents.FILTER_CONTENT));
                if (content.list().isEmpty()) count++;
                if (content.list().size() > 1) return true;
                count += content.list().size();
            } else {
                count++;
            }
        }
        ItemStack provide1 = filters.getItem(1);
        if (!provide1.isEmpty()) {
            if (provide1.has(ModComponents.FILTER_CONTENT)) {
                FilterContent content = Objects.requireNonNull(provide1.get(ModComponents.FILTER_CONTENT));
                if (content.list().isEmpty()) count++;
                if (content.list().size() > 1) return true;
                count += content.list().size();
            } else {
                count++;
            }
        }
        return count != 1;
    }
}
