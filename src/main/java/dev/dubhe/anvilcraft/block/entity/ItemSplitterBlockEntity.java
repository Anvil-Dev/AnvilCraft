package dev.dubhe.anvilcraft.block.entity;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** 严格均分给连续容器，或按铁砧下落高度向空位分配；余数与未送出的份额留在内部。 */
public class ItemSplitterBlockEntity extends BlockEntity implements IItemResourceHandlerHolder {
    public static final int SLOT_COUNT = 16;

    @Getter
    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            ItemSplitterBlockEntity.this.setChanged();
            if (level != null && !level.isClientSide()) {
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
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
        this.cooldown = AnvilCraft.CONFIG.machines.itemSplitterInterval;
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
            this.sendShare(handler, share);
        }
    }

    /// 按槽位顺序向目标容器送入指定数量的物品，可以混装多种物品；送不进去的部分留在内部。
    private void sendShare(ResourceHandler<ItemResource> handler, int share) {
        int remaining = share;
        for (int slot = 0; slot < this.itemHandler.size() && remaining > 0; slot++) {
            ItemResource resource = this.itemHandler.getResource(slot);
            if (resource.isEmpty()) continue;
            int amount = Math.min(remaining, this.itemHandler.getAmountAsInt(slot));
            if (amount <= 0) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = handler.insert(resource, amount, transaction);
                if (inserted > 0 && this.itemHandler.extract(slot, resource, inserted, transaction) == inserted) {
                    transaction.commit();
                    remaining -= inserted;
                }
            }
        }
    }

    public boolean splitToSpace(int shares) {
        if (this.level == null || shares <= 0 || this.getFacing() == Direction.UP) return false;
        if (this.getItemHandlerAt(this.getBlockPos().relative(this.getFacing())) != null) return false;
        List<BlockPos> targets = this.collectSpaceTargets(shares);
        if (targets.isEmpty()) return false;
        int share = this.getTotalCount() / shares;
        if (share <= 0) return false;
        boolean moved = false;
        for (BlockPos target : targets) {
            List<ItemStack> stacks = this.extractShare(share);
            if (stacks.isEmpty()) break;
            for (ItemStack stack : stacks) this.dropAt(target, stack);
            moved = true;
        }
        return moved;
    }

    /// 按槽位顺序取出指定数量的物品，可以混装多种物品。
    private List<ItemStack> extractShare(int share) {
        List<ItemStack> stacks = new ArrayList<>();
        int remaining = share;
        for (int slot = 0; slot < this.itemHandler.size() && remaining > 0; slot++) {
            ItemResource resource = this.itemHandler.getResource(slot);
            if (resource.isEmpty()) continue;
            int amount = Math.min(remaining, this.itemHandler.getAmountAsInt(slot));
            if (amount <= 0) continue;
            int extracted;
            try (Transaction transaction = Transaction.openRoot()) {
                extracted = this.itemHandler.extract(slot, resource, amount, transaction);
                transaction.commit();
            }
            if (extracted <= 0) continue;
            stacks.add(resource.toStack(extracted));
            remaining -= extracted;
        }
        return stacks;
    }

    private List<BlockPos> collectContainerTargets() {
        List<BlockPos> targets = new ArrayList<>();
        for (int distance = 1; distance <= AnvilCraft.CONFIG.machines.itemSplitterMaxDistance; distance++) {
            BlockPos target = this.worldPosition.relative(this.getFacing(), distance);
            if (this.getItemHandlerAt(target) == null) break;
            targets.add(target);
        }
        return targets;
    }

    private List<BlockPos> collectSpaceTargets(int shares) {
        List<BlockPos> targets = new ArrayList<>();
        for (int distance = 1; distance <= AnvilCraft.CONFIG.machines.itemSplitterMaxDistance && targets.size() < shares; distance++) {
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

    /// 空手右键取出内部物品：潜行取出一组，否则只取一个；返回是否有物品被取出。
    public boolean giveToPlayer(Player player, boolean fullStack) {
        for (int slot = 0; slot < this.itemHandler.size(); slot++) {
            ItemResource resource = this.itemHandler.getResource(slot);
            if (resource.isEmpty()) continue;
            int amount = fullStack ? Math.min(this.itemHandler.getAmountAsInt(slot), resource.toStack(1).getMaxStackSize()) : 1;
            if (amount <= 0) continue;
            int extracted;
            try (Transaction transaction = Transaction.openRoot()) {
                extracted = this.itemHandler.extract(slot, resource, amount, transaction);
                transaction.commit();
            }
            if (extracted <= 0) continue;
            ItemStack stack = resource.toStack(extracted);
            if (this.level != null && !player.addItem(stack)) {
                Block.popResource(this.level, BlockPos.containing(player.position()), stack);
            }
            return true;
        }
        return false;
    }

    private void dropAt(BlockPos target, ItemStack stack) {
        if (this.level == null) return;
        Vec3 center = target.getCenter();
        while (!stack.isEmpty()) {
            ItemEntity item = new ItemEntity(
                this.level, center.x, center.y, center.z, stack.split(stack.getMaxStackSize()), 0, 0, 0
            );
            item.setDefaultPickUpDelay();
            this.level.addFreshEntity(item);
        }
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
