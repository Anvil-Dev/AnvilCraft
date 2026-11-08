package dev.dubhe.anvilcraft.api.itemhandler.unlimited;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 大仓库的存取视图，将端口内部库存附加到原始库存之后，保存与掉落仍由各自容器负责。
 */
public class LinkedStorageItems extends UnlimitedItemStacksResourceHandler {
    private final UUID storageId;
    private final UnlimitedItemStacksResourceHandler core;
    private List<StoragePortBlockEntity> ports = List.of();
    private long portVersion = -1;
    private int bufferSlots;

    public LinkedStorageItems(UUID storageId, UnlimitedItemStacksResourceHandler core) {
        super(0);
        this.storageId = storageId;
        this.core = core;
    }

    private List<StoragePortBlockEntity> ports() {
        long version = StoragePortManager.itemPortVersion(this.storageId);
        if (this.portVersion != version) {
            this.ports = StoragePortManager.liveItemPorts(this.storageId);
            this.portVersion = version;
            this.bufferSlots = 0;
            for (StoragePortBlockEntity port : this.ports) {
                this.bufferSlots += port.getBuffer().getSlots();
            }
        }
        return this.ports;
    }

    @Override
    public int size() {
        this.ports();
        return this.core.size() + this.bufferSlots;
    }

    @Override
    public int getSlots() {
        return this.size() + 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < this.core.size()) {
            return this.core.getStackInSlot(slot);
        }
        slot -= this.core.size();
        for (StoragePortBlockEntity port : this.ports()) {
            if (slot < port.getBuffer().getSlots()) {
                return port.isConnectedTo(this.storageId) ? port.getBuffer().getStackInSlot(slot) : ItemStack.EMPTY;
            }
            slot -= port.getBuffer().getSlots();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public UnlimitedItemStack getUnlimitedStackInSlot(int slot) {
        return new UnlimitedItemStack(this.getStackInSlot(slot));
    }

    @Override
    public long getAmountAsLong(int slot) {
        return slot < this.core.size() ? this.core.getAmountAsLong(slot) : this.getStackInSlot(slot).getCount();
    }

    @Override
    public int findSlot(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        int coreSlot = this.core.findSlot(stack);
        if (coreSlot >= 0) {
            return coreSlot;
        }
        int index = this.core.size();
        for (StoragePortBlockEntity port : this.ports()) {
            for (int slot = 0; slot < port.getBuffer().getSlots(); slot++, index++) {
                if (port.isConnectedTo(this.storageId)
                    && ItemStack.isSameItemSameComponents(stack, port.getBuffer().getStackInSlot(slot))) {
                    return index;
                }
            }
        }
        return -1;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int matching = this.core.findSlot(stack);
        if (slot != (matching >= 0 ? matching : this.size())) {
            return stack;
        }
        return this.insertItem(stack, simulate);
    }

    @Override
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !this.core.isItemValid(0, stack)) {
            return stack;
        }
        return this.core.insertItem(stack, simulate);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        int matching = this.core.findSlot(stack);
        return slot == (matching >= 0 ? matching : this.size()) && this.core.isItemValid(0, stack);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return this.extractUnlimited(slot, amount, simulate).toStack();
    }

    @Override
    public UnlimitedItemStack extractUnlimited(int slot, int amount, boolean simulate) {
        ItemStack resource = this.getStackInSlot(slot);
        return new UnlimitedItemStack(this.extract(resource, Math.min(amount, resource.getCount()), simulate));
    }

    /** 统计核心与全部已连接端口中的同种物品。 */
    public long count(ItemStack resource) {
        long amount = this.core.countOfType(resource);
        for (StoragePortBlockEntity port : this.ports()) {
            if (!port.isConnectedTo(this.storageId)) {
                continue;
            }
            for (int slot = 0; slot < port.getBuffer().getSlots(); slot++) {
                ItemStack stack = port.getBuffer().getStackInSlot(slot);
                if (ItemStack.isSameItemSameComponents(resource, stack)) {
                    amount += stack.getCount();
                }
            }
        }
        return amount;
    }

    /** 按物品种类跨槽提取，核心库存耗尽后才使用端口内部库存。 */
    public ItemStack extract(ItemStack resource, int amount, boolean simulate) {
        if (resource.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int extracted = this.core.extract(resource, amount, simulate).getCount();
        for (StoragePortBlockEntity port : this.ports()) {
            if (extracted == amount) {
                break;
            }
            if (!port.isConnectedTo(this.storageId)) {
                continue;
            }
            for (int slot = 0; slot < port.getBuffer().getSlots() && extracted < amount; slot++) {
                if (ItemStack.isSameItemSameComponents(resource, port.getBuffer().getStackInSlot(slot))) {
                    extracted += port.getBuffer().extractItem(slot, amount - extracted, simulate).getCount();
                }
            }
        }
        return resource.copyWithCount(extracted);
    }

    @Override
    public int getTypeCount() {
        Set<ResourceKey> types = new HashSet<>();
        for (int slot = 0; slot < this.size(); slot++) {
            ItemStack stack = this.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                types.add(ResourceKey.of(stack));
            }
        }
        return types.size();
    }

    @Override
    public int getTypeLimit() {
        return this.core.getTypeLimit();
    }

    @Override
    public double getFullness() {
        return this.core.getFullness();
    }
}
