package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Logistics interface for the Celestial Forging Anvil.
 * Stores up to 16 different item types, one stack per type.
 * Items auto-route to their type's slot and don't overflow to other slots.
 */
public class CelestialForgingAnvilLogisticsInterfaceBlockEntity extends BlockEntity {
    private static final int TYPE_COUNT = 16;

    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(TYPE_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            ItemStack current = getStackInSlot(slot);
            if (current.isEmpty()) {
                for (int i = 0; i < TYPE_COUNT; i++) {
                    if (i != slot && ItemStack.isSameItemSameComponents(getStackInSlot(i), stack)) {
                        return false;
                    }
                }
                return true;
            }
            return ItemStack.isSameItemSameComponents(current, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            CelestialForgingAnvilLogisticsInterfaceBlockEntity.this.setChanged();
        }
    };

    public CelestialForgingAnvilLogisticsInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * Sync block entity data to all tracking clients.
     */
    public void syncToClients() {
        if (level instanceof ServerLevel serverLevel) {
            Packet<?> packet = getUpdatePacket();
            if (packet != null) {
                for (ServerPlayer player : serverLevel.getChunkSource().chunkMap
                    .getPlayers(serverLevel.getChunkAt(worldPosition).getPos(), false)) {
                    player.connection.send(packet);
                }
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            syncToClients();
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @SuppressWarnings("unused")
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    private static final int MAX_EJECT_PER_OP = 64; // Max 1 stack per ejection
    private static final int EJECT_COOLDOWN = 8;     // 8gt between ejections (like MagneticChute)

    @Setter
    private int ejectCooldown = 0;
    private int lastEjectSlot = 0;

    /**
     * Server-side tick. When active (redstone powered), auto-ejects items
     * from internal inventory toward the facing direction every 8gt,
     * max 1 stack per ejection, with velocity like MagneticChute.
     * Uses round-robin across slots to prevent one slot from being starved.
     */
    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)) return;
        if (!state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE)) return;

        if (ejectCooldown > 0) {
            ejectCooldown--;
            return;
        }

        Direction facing = state.getValue(CelestialForgingAnvilInterfaceBlock.FACING);
        BlockPos targetPos = worldPosition.relative(facing);
        boolean ejected = false;
        int totalSlots = itemHandler.getSlots();

        // Round-robin: start from lastEjectSlot, iterate all slots
        for (int offset = 0; offset < totalSlots; offset++) {
            int slot = (lastEjectSlot + offset) % totalSlots;
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            int toExtract = Math.min(stack.getCount(), MAX_EJECT_PER_OP);
            ItemStack extracted = itemHandler.extractItem(slot, toExtract, false);
            if (extracted.isEmpty()) continue;

            // Try to insert into target container
            IItemHandler targetHandler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, targetPos, facing.getOpposite()
            );
            if (targetHandler != null) {
                ItemStack remainder = ItemHandlerHelper.insertItem(targetHandler, extracted, false);
                if (!remainder.isEmpty()) {
                    itemHandler.insertItem(slot, remainder, false);
                }
                if (remainder.getCount() < extracted.getCount()) {
                    ejected = true;
                    lastEjectSlot = (slot + 1) % totalSlots;
                    break;
                }
            } else {
                // No target container — eject items into the world with velocity
                Vec3 ejectPos = worldPosition.relative(facing).getCenter();
                Vec3 velocity = new Vec3(
                    facing.getStepX() * 0.25,
                    facing.getStepY() * 0.25,
                    facing.getStepZ() * 0.25
                );
                ItemEntity entity = new ItemEntity(level, ejectPos.x, ejectPos.y, ejectPos.z, extracted);
                entity.setDeltaMovement(velocity);
                entity.setDefaultPickUpDelay();
                level.addFreshEntity(entity);
                ejected = true;
                lastEjectSlot = (slot + 1) % totalSlots;
                break;
            }
        }

        if (ejected) {
            ejectCooldown = EJECT_COOLDOWN;
            setChanged();
        }
    }

    // === Temple demand display (pushed by CFA controller) ===
    @Getter @Setter
    private ItemStack templeDemandItem = ItemStack.EMPTY;
    @Getter @Setter
    private int templeDemandCount = 0;
    @Getter @Setter
    private boolean templeDemandSatisfied = false;

    // === Collider target items display (pushed by CFA controller) ===
    @Getter @Setter
    private List<ItemStack> colliderTargetItems = new ArrayList<>();
    @Getter @Setter
    private boolean colliderProcessing = false;
    @Getter @Setter
    private boolean colliderStarMissing = false;

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ejectCooldown", ejectCooldown);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        if (!templeDemandItem.isEmpty()) {
            tag.put("templeDemandItem", templeDemandItem.save(registries));
        }
        tag.putInt("templeDemandCount", templeDemandCount);
        tag.putBoolean("templeDemandSatisfied", templeDemandSatisfied);
        if (!colliderTargetItems.isEmpty()) {
            ListTag list = new ListTag();
            for (ItemStack stack : colliderTargetItems) {
                if (!stack.isEmpty()) {
                    list.add(stack.save(registries));
                }
            }
            tag.put("colliderTargetItems", list);
        }
        tag.putBoolean("colliderProcessing", colliderProcessing);
        tag.putBoolean("colliderStarMissing", colliderStarMissing);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.ejectCooldown = tag.getInt("ejectCooldown");
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("templeDemandItem")) {
            this.templeDemandItem = ItemStack.parse(registries, tag.getCompound("templeDemandItem"))
                .orElse(ItemStack.EMPTY);
        } else {
            this.templeDemandItem = ItemStack.EMPTY;
        }
        this.templeDemandCount = tag.getInt("templeDemandCount");
        this.templeDemandSatisfied = tag.getBoolean("templeDemandSatisfied");
        this.colliderTargetItems.clear();
        if (tag.contains("colliderTargetItems")) {
            ListTag list = tag.getList("colliderTargetItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack.parse(registries, list.getCompound(i)).ifPresent(colliderTargetItems::add);
            }
        }
        this.colliderProcessing = tag.getBoolean("colliderProcessing");
        this.colliderStarMissing = tag.getBoolean("colliderStarMissing");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        if (!templeDemandItem.isEmpty()) {
            tag.put("templeDemandItem", templeDemandItem.save(registries));
        }
        tag.putInt("templeDemandCount", templeDemandCount);
        tag.putBoolean("templeDemandSatisfied", templeDemandSatisfied);
        if (!colliderTargetItems.isEmpty()) {
            ListTag list = new ListTag();
            for (ItemStack stack : colliderTargetItems) {
                if (!stack.isEmpty()) {
                    list.add(stack.save(registries));
                }
            }
            tag.put("colliderTargetItems", list);
        }
        tag.putBoolean("colliderProcessing", colliderProcessing);
        tag.putBoolean("colliderStarMissing", colliderStarMissing);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("templeDemandItem")) {
            this.templeDemandItem = ItemStack.parse(registries, tag.getCompound("templeDemandItem"))
                .orElse(ItemStack.EMPTY);
        } else {
            this.templeDemandItem = ItemStack.EMPTY;
        }
        this.templeDemandCount = tag.getInt("templeDemandCount");
        this.templeDemandSatisfied = tag.getBoolean("templeDemandSatisfied");
        this.colliderTargetItems.clear();
        if (tag.contains("colliderTargetItems")) {
            ListTag list = tag.getList("colliderTargetItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack.parse(registries, list.getCompound(i)).ifPresent(colliderTargetItems::add);
            }
        }
        this.colliderProcessing = tag.getBoolean("colliderProcessing");
        this.colliderStarMissing = tag.getBoolean("colliderStarMissing");
    }
}
