package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.block.TradingStationBlock;
import dev.dubhe.anvilcraft.block.state.DirectionVertical2PartHalf;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.TradingStationMenu;
import dev.dubhe.anvilcraft.inventory.container.FilterOnlyContainer;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import dev.dubhe.anvilcraft.mixin.accessor.VillagerAccessor;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Getter
public class TradingStationBlockEntity extends BlockEntity
    implements IItemResourceHandlerHolder, IFilterBlockEntity, MenuProvider, IDiskCloneable {
    public static final String OWNER_NBT_ID = "Owner";
    public static final String STORAGE_NBT_ID = "Items";
    public static final String STORAGE_FILTERING_NBT_ID = "StorageFiltering";
    public static final String FILTERS_NBT_ID = "Filters";
    public static final String ALLOW_PLAYER_NBT_ID = "AllowPlayer";
    public static final String ALLOW_VILLAGER_NBT_ID = "AllowVillager";

    private final FilteredItemStackHandler handler = new FilteredItemStackHandler(12) {
        @Override
        public boolean isValid(int slot, ItemResource resource) {
            ItemStack filter = this.getFilter(slot);
            return filter.isEmpty() || this.isFiltered(slot, resource.toStack());
        }

        @Override
        protected void onContentsChanged(int slot, ItemStack previousContents) {
            ItemStack stack = this.getResource(slot).toStack(this.getAmountAsInt(slot));
            if (!stack.isEmpty() && this.getFilter(slot).isEmpty()) {
                this.setFilter(slot, stack);
            }
            TradingStationBlockEntity.updateAndSend(TradingStationBlockEntity.this);
        }
    };
    private final ResourceHandler<ItemResource> proxy = new ResourceHandler<>() {
        @Override
        public int size() {
            return TradingStationBlockEntity.this.handler.size();
        }

        @Override
        public ItemResource getResource(int index) {
            return TradingStationBlockEntity.this.handler.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return TradingStationBlockEntity.this.handler.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return TradingStationBlockEntity.this.handler.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return TradingStationBlockEntity.this.handler.isValid(index, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (!TradingStationBlockEntity.this.inputAllowed) return 0;
            return TradingStationBlockEntity.this.handler.insert(index, resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (!TradingStationBlockEntity.this.outputAllowed) return 0;
            return TradingStationBlockEntity.this.handler.extract(index, resource, amount, transaction);
        }
    };
    private final FilterOnlyContainer filters = new FilterOnlyContainer(this, 3) {
        @Override
        public void deserialize(ValueInput input) {
            super.deserialize(input);
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.owner != null) output.store(OWNER_NBT_ID, UUIDUtil.CODEC, this.owner);
        this.handler.serialize(output.child(STORAGE_NBT_ID));
        this.filters.serialize(output.child(FILTERS_NBT_ID));
        output.putBoolean(ALLOW_PLAYER_NBT_ID, this.playerAllowed);
        output.putBoolean(ALLOW_VILLAGER_NBT_ID, this.villagerAllowed);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.owner = input.read(OWNER_NBT_ID, UUIDUtil.CODEC).orElse(null);
        input.child(STORAGE_NBT_ID).ifPresent(this.handler::deserialize);
        input.child(FILTERS_NBT_ID).ifPresent(this.filters::deserialize);
        this.playerAllowed = input.getBooleanOr(ALLOW_PLAYER_NBT_ID, false);
        this.villagerAllowed = input.getBooleanOr(ALLOW_VILLAGER_NBT_ID, false);
        TradingStationBlockEntity.popoutInvalidItems(this.getLevel(), this.getBlockPos(), this.handler);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public @Nullable TradingStationMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (this.owner != null && !player.getGameProfile().id().equals(this.owner)) return null;
        return new TradingStationMenu(ModMenuTypes.TRADING_STATION.get(), containerId, playerInventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.getBlockPos());
    }

    @Override
    public void storeDiskData(ValueOutput output) {
        if (this.owner != null) output.store(OWNER_NBT_ID, UUIDUtil.CODEC, this.owner);
        this.filters.serialize(output.child(FILTERS_NBT_ID));
        this.handler.serializeFiltering(output.child(STORAGE_FILTERING_NBT_ID));
        output.putBoolean(ALLOW_PLAYER_NBT_ID, this.playerAllowed);
        output.putBoolean(ALLOW_VILLAGER_NBT_ID, this.villagerAllowed);
    }

    @Override
    public void applyDiskData(ValueInput input) {
        Optional<UUID> ownerValue = input.read(OWNER_NBT_ID, UUIDUtil.CODEC);
        if (ownerValue.isEmpty()) return;
        UUID owner = ownerValue.get();
        if (this.owner != null) {
            if (!this.owner.equals(owner)) return;
        } else {
            this.owner = owner;
        }

        input.child(FILTERS_NBT_ID).ifPresent(this.filters::deserialize);
        input.child(STORAGE_FILTERING_NBT_ID).ifPresent(this.handler::deserializeFiltering);
        this.playerAllowed = input.getBooleanOr(ALLOW_PLAYER_NBT_ID, false);
        this.villagerAllowed = input.getBooleanOr(ALLOW_VILLAGER_NBT_ID, false);
        TradingStationBlockEntity.popoutInvalidItems(this.getLevel(), this.getBlockPos(), this.handler);
        TradingStationBlockEntity.updateAndSend(this);
    }

    @Override
    public ResourceHandler<ItemResource> getItemHandler() {
        return this.proxy;
    }

    @Override
    public FilteredItemStackHandler getFilteredItemStackHandler() {
        return this.handler;
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

        // Require exactly one provide item type for unambiguous player trading
        if (TradingStationBlockEntity.isProvideMultiple(this.filters)) return false;

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
        for (int i = 0; i < this.handler.size(); i++) {
            ItemStack stack = this.getStack(i);
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

        // Check there is enough filtered space in the handler to insert the requested items.
        ItemStack toInsertPrototype = requesting.copy();
        toInsertPrototype.setCount(requestCount);
        if (!this.insertIntoStorage(toInsertPrototype, true).isEmpty()) return false;

        // Remove providing items from the handler according to the plan in 'modifying'
        for (Int2IntMap.Entry entry : modifying.int2IntEntrySet()) {
            int slot = entry.getIntKey();
            int amount = entry.getIntValue();
            ItemStack stack = this.getStack(slot).copy();
            stack.shrink(amount);
            if (stack.getCount() <= 0) stack = ItemStack.EMPTY;
            this.setStack(slot, stack);
        }

        // Insert the requested items into slots whose filters accept them.
        if (!this.insertIntoStorage(toInsertPrototype, false).isEmpty()) {
            // This should not happen due to prior space check, but if it does, try to revert providing removal
            for (Int2IntMap.Entry entry : modifying.int2IntEntrySet()) {
                int slot = entry.getIntKey();
                int amount = entry.getIntValue();
                ItemStack current = this.getStack(slot);
                if (current.isEmpty()) {
                    this.setStack(slot, providing.copyWithCount(amount));
                } else if (ItemStack.isSameItemSameComponents(current, providing)) {
                    int newCount = Math.min(current.getCount() + amount, current.getMaxStackSize());
                    this.setStack(slot, current.copyWithCount(newCount));
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

    public boolean canTradeWithVillager(Villager villager) {
        if (!this.villagerAllowed) return false;
        if (this.level == null || this.level.isClientSide()) return false;
        if (this.filters.getItem(2).isEmpty()) return false;
        if (this.filters.getItem(0).isEmpty() && this.filters.getItem(1).isEmpty()) return false;
        return this.findAcceptableOffer(villager).isPresent();
    }

    public boolean tryTradingWithVillager(Villager villager) {
        if (!this.villagerAllowed) return false;
        if (this.level == null || this.level.isClientSide()) return false;
        Optional<MerchantOffer> op = this.findAcceptableOffer(villager);
        if (op.isEmpty()) return false;
        MerchantOffer offer = op.get();
        if (!this.executeVillagerTrade(offer)) return false;
        offer.increaseUses();
        villager.setVillagerXp(villager.getVillagerXp() + offer.getXp());
        VillagerAccessor accessor = (VillagerAccessor) villager;
        if (accessor.invokeShouldIncreaseLevel()) {
            accessor.setUpdateMerchantTimer(40);
            accessor.setIncreaseProfessionLevelOnUpdate(true);
        }
        this.spawnVillagerTradeParticles();
        TradingStationBlockEntity.updateAndSend(this);
        return true;
    }

    private void spawnVillagerTradeParticles() {
        if (!(this.level instanceof ServerLevel serverLevel)) return;
        BlockPos pos = this.getBlockPos();
        serverLevel.sendParticles(
            ParticleTypes.HAPPY_VILLAGER,
            pos.getX() + 0.5,
            pos.getY() + 1.0,
            pos.getZ() + 0.5,
            12,
            0.35,
            0.3,
            0.35,
            0.02
        );
    }

    private Optional<MerchantOffer> findAcceptableOffer(Villager villager) {
        for (MerchantOffer offer : villager.getOffers()) {
            if (offer.isOutOfStock()) continue;
            if (this.matchesFilters(offer) && this.canFulfillOffer(offer)) {
                return Optional.of(offer);
            }
        }
        return Optional.empty();
    }

    private boolean matchesFilters(MerchantOffer offer) {
        ItemStack result = offer.getResult();
        ItemStack req = this.filters.getItem(2);
        if (req.isEmpty()) return false;
        if (!FilterContent.filter(req, result, !req.getComponentsPatch().isEmpty())) return false;
        if (req.getCount() > result.getCount()) return false;
        return assignProvideFilters(this.filters.getItem(0), this.filters.getItem(1), offer.getCostA(), offer.getCostB());
    }

    private static boolean assignProvideFilters(ItemStack p0, ItemStack p1, ItemStack costA, ItemStack costB) {
        if (costB.isEmpty()) {
            return provideMatches(p0, costA) || provideMatches(p1, costA);
        }
        if (provideMatches(p0, costA) && provideMatches(p1, costB)) return true;
        return provideMatches(p0, costB) && provideMatches(p1, costA);
    }

    private static boolean provideMatches(ItemStack filter, ItemStack cost) {
        if (filter.isEmpty() || cost.isEmpty()) return false;
        if (!FilterContent.filter(filter, cost, !filter.getComponentsPatch().isEmpty())) return false;
        return filter.getCount() >= cost.getCount();
    }

    private boolean canFulfillOffer(MerchantOffer offer) {
        return this.simulateVillagerTrade(offer, false);
    }

    private boolean executeVillagerTrade(MerchantOffer offer) {
        return this.simulateVillagerTrade(offer, true);
    }

    private boolean simulateVillagerTrade(MerchantOffer offer, boolean commit) {
        ItemStack[] snapshot = new ItemStack[this.handler.size()];
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i] = this.getStack(i).copy();
        }
        if (!removeMatching(offer.getCostA(), snapshot)) return false;
        if (!offer.getCostB().isEmpty() && !removeMatching(offer.getCostB(), snapshot)) return false;
        ItemStack result = offer.getResult();
        if (!this.insertMatching(result, snapshot)) return false;
        if (!commit) return true;
        for (int i = 0; i < snapshot.length; i++) {
            if (!ItemStack.matches(snapshot[i], this.getStack(i))) {
                this.setStack(i, snapshot[i]);
            }
        }
        return true;
    }

    private static boolean removeMatching(ItemStack cost, ItemStack[] snapshot) {
        int remaining = cost.getCount();
        for (int i = 0; i < snapshot.length && remaining > 0; i++) {
            ItemStack stack = snapshot[i];
            if (stack.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(stack, cost)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            if (stack.isEmpty()) snapshot[i] = ItemStack.EMPTY;
            remaining -= take;
        }
        return remaining <= 0;
    }

    private boolean insertMatching(ItemStack result, ItemStack[] snapshot) {
        int remaining = result.getCount();
        for (int i = 0; i < snapshot.length && remaining > 0; i++) {
            ItemStack existing = snapshot[i];
            if (existing.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(existing, result)) continue;
            int max = Math.min(this.handler.getSlotLimit(i), result.getMaxStackSize());
            int can = max - existing.getCount();
            if (can <= 0) continue;
            int put = Math.min(can, remaining);
            snapshot[i] = existing.copyWithCount(existing.getCount() + put);
            remaining -= put;
        }
        for (int i = 0; i < snapshot.length && remaining > 0; i++) {
            if (!snapshot[i].isEmpty()) continue;
            if (!this.handler.isValid(i, ItemResource.of(result))) continue;
            int put = Math.min(remaining, Math.min(this.handler.getSlotLimit(i), result.getMaxStackSize()));
            snapshot[i] = result.copyWithCount(put);
            remaining -= put;
        }
        return remaining <= 0;
    }

    private ItemStack insertIntoStorage(ItemStack stack, boolean simulate) {
        ItemResource resource = ItemResource.of(stack);
        int inserted;
        try (Transaction transaction = Transaction.openRoot()) {
            inserted = this.handler.insert(resource, stack.getCount(), transaction);
            if (!simulate) transaction.commit();
        }
        int remaining = stack.getCount() - inserted;
        return remaining == 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining);
    }

    public boolean isOwner(Player sp) {
        return sp.getGameProfile().id().equals(this.owner);
    }

    public void setOwner(UUID owner) {
        Level level = this.getLevel();
        if (level == null) return;
        if (this.owner == null) this.owner = owner;
        TradingStationBlockEntity.updateAndSend(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.owner = null;
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

    public static void popoutInvalidItems(@Nullable Level level, BlockPos pos, FilteredItemStackHandler handler) {
        if (level == null || level.isClientSide()) return;
        for (int i = 0; i < handler.size(); i++) {
            ItemStack stack = handler.getResource(i).toStack(handler.getAmountAsInt(i));
            if (stack.isEmpty() || handler.isValid(i, ItemResource.of(stack))) continue;
            handler.set(i, ItemResource.EMPTY, 0);
            Block.popResourceFromFace(level, pos, Direction.UP, stack);
        }
    }

    private ItemStack getStack(int slot) {
        return this.handler.getResource(slot).toStack(this.handler.getAmountAsInt(slot));
    }

    private void setStack(int slot, ItemStack stack) {
        this.handler.set(slot, ItemResource.of(stack), stack.getCount());
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
