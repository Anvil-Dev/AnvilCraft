package dev.dubhe.anvilcraft.inventory.container;

import dev.dubhe.anvilcraft.api.item.property.FilterContent;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.network.FilterContentSyncPacket;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FilterContainer implements Container {
    private final FilterContent content;
    private final Player player;
    private final int position;
    private final ItemStack stack;

    public FilterContainer(Player player, int position, ItemStack stack) {
        this.player = player;
        this.position = position;
        this.stack = stack;
        this.content = stack.getOrDefault(ModComponents.FILTER_CONTENT, new FilterContent());
    }

    public FilterContainer(Inventory inventory, FriendlyByteBuf buf) {
        this.player = inventory.player;
        this.position = buf.readInt();
        this.stack = inventory.getItem(position);
        this.content = this.stack.getOrDefault(ModComponents.FILTER_CONTENT, new FilterContent());
    }

    @Override
    public int getContainerSize() {
        return content.getList().size();
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return content.getList().get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        content.getList().set(slot, stack);
    }

    @Override
    public void setChanged() {
        this.stack.set(ModComponents.FILTER_CONTENT, content);
    }

    @Override
    public boolean stillValid(Player player) {
        return stack == player.getInventory().getItem(position);
    }

    @Override
    public void clearContent() {
    }

    @OnlyIn(Dist.CLIENT)
    public void sync() {
        PacketDistributor.sendToServer(new FilterContentSyncPacket(position, content));
    }
}
