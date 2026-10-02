package dev.dubhe.anvilcraft.block.entity;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.block.logistics.ItemSplitterBlock;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** 严格均分给连续容器，或按铁砧下落高度向空位分配；余数与未送出的份额留在内部。 */
public class ItemSplitterBlockEntity extends BlockEntity implements IItemResourceHandlerHolder {
    public static final int SLOT_COUNT = 16;
    public static final int SPLIT_INTERVAL = 8;
    public static final int MAX_DISTANCE = 16;

    @Getter
    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(SLOT_COUNT) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            for (int slot = 0; slot < this.size(); slot++) {
                ItemResource existing = this.getResource(slot);
                if (!existing.isEmpty() && !existing.equals(resource)) return false;
            }
            return super.isValid(index, resource);
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            ItemSplitterBlockEntity.this.setChanged();
        }
    };

    private int cooldown;

    public ItemSplitterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Cooldown", this.cooldown);
        this.itemHandler.serialize(output.child("Inventory"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.cooldown = input.getIntOr("Cooldown", 0);
        ValueInput inventory = input.childOrEmpty("Inventory");
        CompoundTag tag = inventory.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(CompoundTag::new);
        if (tag.get("Items") instanceof ListTag items) {
            for (int slot = 0; slot < SLOT_COUNT; slot++) this.itemHandler.set(slot, ItemResource.EMPTY, 0);
            var ops = input.lookup().createSerializationContext(NbtOps.INSTANCE);
            for (int i = 0; i < items.size(); i++) {
                CompoundTag entry = items.getCompoundOrEmpty(i);
                int slot = entry.getIntOr("Slot", -1);
                if (slot < 0 || slot >= SLOT_COUNT) continue;
                ItemStack stack = ItemStack.CODEC.parse(ops, entry).result().orElse(ItemStack.EMPTY);
                this.itemHandler.set(slot, ItemResource.of(stack), stack.getCount());
            }
        } else {
            this.itemHandler.deserialize(inventory);
        }
    }

    public void tick() {
        if (this.level == null || this.level.isClientSide()) return;
        if (this.cooldown > 0) this.cooldown--;
        if (this.cooldown > 0) return;
        this.cooldown = SPLIT_INTERVAL;
        this.splitToContainers();
    }

    public void splitToContainers() {
        if (this.level == null) return;
        List<BlockPos> targets = this.collectContainerTargets();
        if (targets.isEmpty()) return;
        int share = this.getTotalCount() / targets.size();
        if (share <= 0) return;
        for (BlockPos target : targets) {
            ResourceHandler<ItemResource> handler = this.getItemHandlerAt(target);
            if (handler == null) continue;
            ItemResource resource = this.firstResource();
            if (resource.isEmpty()) break;
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = handler.insert(resource, share, transaction);
                if (inserted > 0 && this.extractTotal(resource, inserted, transaction) == inserted) transaction.commit();
            }
        }
    }

    public boolean splitToSpace(int shares) {
        if (this.level == null || shares <= 0) return false;
        if (this.getItemHandlerAt(this.getBlockPos().relative(this.getFacing())) != null) return false;
        List<BlockPos> targets = this.collectSpaceTargets(shares);
        if (targets.isEmpty()) return false;
        int share = this.getTotalCount() / shares;
        if (share <= 0) return false;
        boolean moved = false;
        for (BlockPos target : targets) {
            ItemResource resource = this.firstResource();
            if (resource.isEmpty()) break;
            int extracted;
            try (Transaction transaction = Transaction.openRoot()) {
                extracted = this.extractTotal(resource, share, transaction);
                transaction.commit();
            }
            if (extracted <= 0) break;
            this.dropAt(target, resource.toStack(extracted));
            moved = true;
        }
        return moved;
    }

    private List<BlockPos> collectContainerTargets() {
        List<BlockPos> targets = new ArrayList<>();
        for (int distance = 1; distance <= MAX_DISTANCE; distance++) {
            BlockPos target = this.worldPosition.relative(this.getFacing(), distance);
            if (this.getItemHandlerAt(target) == null) break;
            targets.add(target);
        }
        return targets;
    }

    private List<BlockPos> collectSpaceTargets(int shares) {
        List<BlockPos> targets = new ArrayList<>();
        for (int distance = 1; distance <= MAX_DISTANCE && targets.size() < shares; distance++) {
            BlockPos target = this.worldPosition.relative(this.getFacing(), distance);
            Vec3 center = target.getCenter();
            if (this.level != null && this.level.noCollision(new AABB(center.add(-0.125, -0.125, -0.125),
                center.add(0.125, 0.125, 0.125)))) {
                targets.add(target);
            }
        }
        return targets;
    }

    private @Nullable ResourceHandler<ItemResource> getItemHandlerAt(BlockPos target) {
        return this.level == null ? null : this.level.getCapability(Capabilities.Item.BLOCK, target, this.getFacing().getOpposite());
    }

    public int getTotalCount() {
        int total = 0;
        for (int slot = 0; slot < this.itemHandler.size(); slot++) total += this.itemHandler.getAmountAsInt(slot);
        return total;
    }

    private ItemResource firstResource() {
        for (int slot = 0; slot < this.itemHandler.size(); slot++) {
            ItemResource resource = this.itemHandler.getResource(slot);
            if (!resource.isEmpty()) return resource;
        }
        return ItemResource.EMPTY;
    }

    private int extractTotal(ItemResource resource, int amount, TransactionContext transaction) {
        int extracted = 0;
        for (int slot = 0; slot < this.itemHandler.size() && extracted < amount; slot++) {
            extracted += this.itemHandler.extract(slot, resource, amount - extracted, transaction);
        }
        return extracted;
    }

    private void dropAt(BlockPos target, ItemStack stack) {
        if (this.level == null) return;
        Vec3 center = target.getCenter();
        ItemEntity item = new ItemEntity(this.level, center.x, center.y, center.z, stack, 0, 0, 0);
        item.setDefaultPickUpDelay();
        this.level.addFreshEntity(item);
    }

    public Direction getFacing() {
        BlockState state = this.getBlockState();
        return state.getBlock() instanceof ItemSplitterBlock ? state.getValue(ItemSplitterBlock.FACING) : Direction.NORTH;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level == null || this.level.isClientSide()) return;
        Vec3 center = pos.getCenter();
        for (int slot = 0; slot < this.itemHandler.size(); slot++) {
            ItemStack stack = this.itemHandler.getResource(slot).toStack(this.itemHandler.getAmountAsInt(slot));
            if (!stack.isEmpty()) Containers.dropItemStack(this.level, center.x, center.y, center.z, stack);
            this.itemHandler.set(slot, ItemResource.EMPTY, 0);
        }
        this.level.updateNeighbourForOutputSignal(pos, state.getBlock());
    }
}
