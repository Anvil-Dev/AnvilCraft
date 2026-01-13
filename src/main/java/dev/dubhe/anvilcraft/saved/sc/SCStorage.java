package dev.dubhe.anvilcraft.saved.sc;

import dev.dubhe.anvilcraft.api.sc.item.ItemEntries;
import dev.dubhe.anvilcraft.api.sc.upgrade.Upgrades;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.saved.sc.client.ClientSCStorage;
import dev.dubhe.anvilcraft.saved.sc.server.ServerSCStorage;
import dev.dubhe.anvilcraft.util.Util;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

@Getter
public abstract class SCStorage {
    private Component name = ModBlocks.SHULKER_CONTAINER.get().getName();
    protected final UUID id;
    protected final ItemEntries entries;
    protected final Upgrades upgrades = new Upgrades();

    protected SCStorage(UUID id) {
        this.id = id;
        this.entries = new ItemEntries(this.upgrades);
    }

    protected SCStorage(UUID id, ItemEntries entries, Upgrades upgrades) {
        this(id);
        this.upgrades.sync(upgrades);
        this.entries.sync(entries);
    }

    public UnlimitedItemStack getItem(int index) {
        return this.entries.getItem(index);
    }

    public ItemStack splitUnchecked(int index, int amount) {
        return this.entries.split(index, amount).toStack();
    }

    public ItemStack split(int index, int amount) {
        UnlimitedItemStack stack = this.getItem(index);
        float part = (float) amount / stack.getCount();
        amount = Math.min(amount, (int) Math.ceil(stack.getStack().getMaxStackSize() * part));
        return this.splitUnchecked(index, amount);
    }

    public int getMaxStackSize() {
        return Item.DEFAULT_MAX_STACK_SIZE * this.upgrades.getStackPower();
    }

    public int getMaxStackSize(ItemStack stack) {
        return stack.getMaxStackSize() * this.upgrades.getStackPower();
    }

    public boolean isFull(UnlimitedItemStack stack) {
        return this.getMaxStackSize(stack.getStack()) <= stack.getCount();
    }

    public boolean isMaxEntries() {
        return this.entries.entrySize() >= this.upgrades.getEntryLimit();
    }

    public ClientSCStorage intoClient() {
        throw new IllegalStateException("Trying to get client storage in non-client environment");
    }

    public ServerSCStorage intoServer() {
        throw new IllegalStateException("Trying to get server storage in non-server environment");
    }
}
