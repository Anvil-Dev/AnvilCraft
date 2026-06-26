package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Logistics interface for the Celestial Forging Anvil.
 * Stores up to 16 different item types, one stack per type.
 * Items auto-route to their type's slot and don't overflow to other slots.
 * When active (redstone powered), auto-ejects items toward the facing direction.
 */
public class CelestialForgingAnvilLogisticsInterfaceBlockEntity extends BlockEntity implements IItemResourceHandlerHolder {
    private static final int TYPE_COUNT = 16;
    @Setter
    private boolean syncing = false; // re-entrancy guard

    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(TYPE_COUNT) {
        @Override
        public boolean isValid(int slot, ItemResource resource) {
            ItemResource current = this.getResource(slot);
            if (current.isEmpty()) {
                for (int i = 0; i < TYPE_COUNT; i++) {
                    if (i != slot) {
                        ItemResource other = this.getResource(i);
                        if (!other.isEmpty() && ItemStack.isSameItemSameComponents(other.toStack(), resource.toStack())) {
                            return false;
                        }
                    }
                }
                return true;
            }
            return ItemStack.isSameItemSameComponents(current.toStack(), resource.toStack());
        }

        @Override
        protected void onContentsChanged(int slot, ItemStack stack) {
            CelestialForgingAnvilLogisticsInterfaceBlockEntity.this.setChanged();
            if (!CelestialForgingAnvilLogisticsInterfaceBlockEntity.this.syncing) {
                CelestialForgingAnvilLogisticsInterfaceBlockEntity.this.triggerWormholeSync(slot);
            }
        }
    };

    public CelestialForgingAnvilLogisticsInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public CelestialForgingAnvilLogisticsInterfaceBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CELESTIAL_FORGING_ANVIL_LOGISTICS_INTERFACE.get(), pos, blockState);
    }

    public static CelestialForgingAnvilLogisticsInterfaceBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState state
    ) {
        return new CelestialForgingAnvilLogisticsInterfaceBlockEntity(type, pos, state);
    }

    // === Network sync ===

    /**
     * Sync block entity data to all tracking clients.
     */
    public void syncToClients() {
        if (level instanceof ServerLevel serverLevel) {
            Packet<?> packet = this.getUpdatePacket();
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
            this.syncToClients();
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @SuppressWarnings("unused")
    public ResourceHandler<ItemResource> getItemHandler() {
        return this.itemHandler;
    }

    // === Wormhole sync ===

    /**
     * Called from {@code onContentChanged} when a player inserts or removes items.
     * Immediately triggers the parent CFA's wormhole sync for this specific interface,
     * pushing the change to the canonical and to other CFAs in the same tick.
     */
    private void triggerWormholeSync(int changedSlot) {
        if (level == null || level.isClientSide()) return;
        BlockPos cfaPos = this.findParentCfa();
        if (cfaPos == null) return;
        if (level.getBlockEntity(cfaPos) instanceof CelestialForgingAnvilBlockEntity cfa) {
            cfa.syncLogisticsOnChange(worldPosition, changedSlot);
        }
    }

    /**
     * Find the parent CFA controller by following the FACING direction.
     * The interface faces AWAY from the CFA, so the adjacent block in the
     * opposite direction is always a CFA part. From there, HALF offset
     * navigates to the controller (BOTTOM_CENTER).
     */
    @Nullable
    private BlockPos findParentCfa() {
        if (level == null) return null;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof CelestialForgingAnvilInterfaceBlock)) return null;
        Direction towardsCfa = state.getValue(CelestialForgingAnvilInterfaceBlock.FACING).getOpposite();
        BlockPos cfaBlockPos = worldPosition.relative(towardsCfa);
        BlockState cfaState = level.getBlockState(cfaBlockPos);
        if (cfaState.getBlock() instanceof CelestialForgingAnvilBlock) {
            Cube323PartHalf half = cfaState.getValue(CelestialForgingAnvilBlock.HALF);
            BlockPos controllerPos = cfaBlockPos.offset(half.getOffset().multiply(-1));
            if (level.getBlockEntity(controllerPos) instanceof CelestialForgingAnvilBlockEntity) {
                return controllerPos;
            }
        }
        return null;
    }

    // === Auto-eject tick ===

    private static final int MAX_EJECT_PER_OP = 64; // Max 1 stack per ejection
    public static final int EJECT_COOLDOWN = 8;     // 8gt between ejections (like MagneticChute)

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

        if (this.ejectCooldown > 0) {
            this.ejectCooldown--;
            return;
        }

        Direction facing = state.getValue(CelestialForgingAnvilInterfaceBlock.FACING);
        BlockPos targetPos = worldPosition.relative(facing);
        boolean ejected = false;
        int totalSlots = this.itemHandler.size();

        // Round-robin: start from lastEjectSlot, iterate all slots
        for (int offset = 0; offset < totalSlots; offset++) {
            int slot = (this.lastEjectSlot + offset) % totalSlots;
            ItemResource resource = this.itemHandler.getResource(slot);
            if (resource.isEmpty()) continue;
            int amount = this.itemHandler.getAmountAsInt(slot);
            int toExtract = Math.min(amount, MAX_EJECT_PER_OP);
            ItemStack stackToMove = resource.toStack(toExtract);

            // Try to insert into target container
            ResourceHandler<ItemResource> targetHandler = level.getCapability(
                Capabilities.Item.BLOCK, targetPos, facing.getOpposite()
            );
            if (targetHandler != null) {
                ItemStack remainder = ItemHandlerUtil.insertItem(targetHandler, stackToMove, false);
                int inserted = toExtract - remainder.getCount();
                if (inserted > 0) {
                    try (Transaction tx = Transaction.openRoot()) {
                        this.itemHandler.extract(slot, resource, inserted, tx);
                        tx.commit();
                    }
                }
                if (remainder.getCount() < stackToMove.getCount()) {
                    ejected = true;
                    this.lastEjectSlot = (slot + 1) % totalSlots;
                    break;
                }
            } else {
                // No target container — eject items into the world with velocity
                try (Transaction tx = Transaction.openRoot()) {
                    int extracted = this.itemHandler.extract(slot, resource, toExtract, tx);
                    if (extracted > 0) {
                        tx.commit();
                        ItemStack toEject = resource.toStack(extracted);
                        Vec3 ejectPos = worldPosition.relative(facing).getCenter();
                        Vec3 velocity = new Vec3(
                            facing.getStepX() * 0.25,
                            facing.getStepY() * 0.25,
                            facing.getStepZ() * 0.25
                        );
                        ItemEntity entity = new ItemEntity(level, ejectPos.x, ejectPos.y, ejectPos.z, toEject);
                        entity.setDeltaMovement(velocity);
                        entity.setDefaultPickUpDelay();
                        level.addFreshEntity(entity);
                        ejected = true;
                        this.lastEjectSlot = (slot + 1) % totalSlots;
                        break;
                    }
                }
            }
        }

        if (ejected) {
            this.ejectCooldown = EJECT_COOLDOWN;
            this.setChanged();
        }
    }

    // === Temple demand display (pushed by CFA controller) ===
    @Getter
    private ItemStack templeDemandItem = ItemStack.EMPTY;
    @Getter
    private int templeDemandCount = 0;
    @Getter
    private int templeDemandProgress = 0;
    @Getter
    private boolean templeDemandSatisfied = false;

    // === Collider target items display (pushed by CFA controller) ===
    @Getter
    private List<ItemStack> colliderTargetItems = new ArrayList<>();
    @Getter
    private boolean colliderProcessing = false;
    @Getter
    private boolean colliderStarMissing = false;

    // Custom setters that trigger network sync for tooltip updates

    public void setTempleDemandItem(ItemStack templeDemandItem) {
        this.templeDemandItem = templeDemandItem;
        this.setChanged();
    }

    public void setTempleDemandCount(int templeDemandCount) {
        this.templeDemandCount = templeDemandCount;
        this.setChanged();
    }

    public void setTempleDemandProgress(int templeDemandProgress) {
        this.templeDemandProgress = templeDemandProgress;
        this.setChanged();
    }

    public void setTempleDemandSatisfied(boolean templeDemandSatisfied) {
        this.templeDemandSatisfied = templeDemandSatisfied;
        this.setChanged();
    }

    public void setColliderTargetItems(List<ItemStack> colliderTargetItems) {
        this.colliderTargetItems = colliderTargetItems;
        this.setChanged();
    }

    public void setColliderProcessing(boolean colliderProcessing) {
        this.colliderProcessing = colliderProcessing;
        this.setChanged();
    }

    public void setColliderStarMissing(boolean colliderStarMissing) {
        this.colliderStarMissing = colliderStarMissing;
        this.setChanged();
    }

    public void setEjectCooldown(int ejectCooldown) {
        this.ejectCooldown = ejectCooldown;
        this.setChanged();
    }

    // === Persistence (26.1: ValueOutput / ValueInput for disk) ===

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("ejectCooldown", this.ejectCooldown);
        this.itemHandler.serialize(output.child("inventory"));
        if (!this.templeDemandItem.isEmpty()) {
            output.store("templeDemandItem", ItemStack.OPTIONAL_CODEC, this.templeDemandItem);
        }
        output.putInt("templeDemandCount", this.templeDemandCount);
        output.putInt("templeDemandProgress", this.templeDemandProgress);
        output.putBoolean("templeDemandSatisfied", this.templeDemandSatisfied);
        if (!this.colliderTargetItems.isEmpty()) {
            ValueOutput.ValueOutputList list = output.childrenList("colliderTargetItems");
            for (ItemStack stack : this.colliderTargetItems) {
                if (!stack.isEmpty()) {
                    list.addChild().store("item", ItemStack.OPTIONAL_CODEC, stack);
                }
            }
        }
        output.putBoolean("colliderProcessing", this.colliderProcessing);
        output.putBoolean("colliderStarMissing", this.colliderStarMissing);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.ejectCooldown = input.getIntOr("ejectCooldown", 0);
        this.itemHandler.deserialize(input.childOrEmpty("inventory"));
        this.templeDemandItem = input.read("templeDemandItem", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.templeDemandCount = input.getIntOr("templeDemandCount", 0);
        this.templeDemandProgress = input.getIntOr("templeDemandProgress", 0);
        this.templeDemandSatisfied = input.getBooleanOr("templeDemandSatisfied", false);
        this.colliderTargetItems.clear();
        input.childrenList("colliderTargetItems").ifPresent(list -> {
            for (ValueInput child : list) {
                child.read("item", ItemStack.OPTIONAL_CODEC).ifPresent(this.colliderTargetItems::add);
            }
        });
        this.colliderProcessing = input.getBooleanOr("colliderProcessing", false);
        this.colliderStarMissing = input.getBooleanOr("colliderStarMissing", false);
    }

    // === Network sync (26.1: getUpdateTag sends CompoundTag → client loadAdditional(ValueInput)) ===

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("ejectCooldown", this.ejectCooldown);
        // Serialize inventory data for client-side tooltip display
        TagValueOutput invOutput = TagValueOutput.createWithContext(
            new ProblemReporter.Collector(this.problemPath()), registries);
        this.itemHandler.serialize(invOutput);
        tag.put("inventory", invOutput.buildResult());
        if (!this.templeDemandItem.isEmpty()) {
            tag.put("templeDemandItem", ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, this.templeDemandItem).getOrThrow());
        }
        tag.putInt("templeDemandCount", this.templeDemandCount);
        tag.putInt("templeDemandProgress", this.templeDemandProgress);
        tag.putBoolean("templeDemandSatisfied", this.templeDemandSatisfied);
        if (!this.colliderTargetItems.isEmpty()) {
            ListTag list = new ListTag();
            for (ItemStack stack : this.colliderTargetItems) {
                if (!stack.isEmpty()) {
                    list.add(ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).getOrThrow());
                }
            }
            tag.put("colliderTargetItems", list);
        }
        tag.putBoolean("colliderProcessing", this.colliderProcessing);
        tag.putBoolean("colliderStarMissing", this.colliderStarMissing);
        return tag;
    }
}
