package dev.dubhe.anvilcraft.inventory;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PocketInventory extends SimpleContainer {
    public static final int MAX_SIZE = 12;
    public static final Codec<PocketInventory> CODEC = ItemStack.OPTIONAL_CODEC.listOf()
        .xmap(PocketInventory::new, PocketInventory::snapshot);
    public static final StreamCodec<RegistryFriendlyByteBuf, PocketInventory> STREAM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC
        .apply(ByteBufCodecs.list(MAX_SIZE)).map(PocketInventory::new, PocketInventory::snapshot);
    private List<ItemStack> lastSynced = List.of();
    private @Nullable ItemStack wornLeggings;

    public PocketInventory() {
        super(MAX_SIZE);
    }

    private PocketInventory(List<ItemStack> items) {
        this();
        for (int slot = 0; slot < Math.min(items.size(), MAX_SIZE); slot++) this.setItem(slot, items.get(slot).copy());
    }

    public static PocketInventory get(Player player) {
        return player.getData(ModDataAttachments.POCKETS);
    }

    public static int capacity(Player player) {
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        if (legs.is(ModItems.WEATHERPROOF_SPACESUIT_LEGGINGS)) return 12;
        return legs.is(ModItems.POCKETS_LEGGINGS) ? 6 : 0;
    }

    public static boolean isLocked(Player player) {
        return capacity(player) > 0 && !get(player).isEmpty();
    }

    public static List<ItemStack> items(Player player) {
        List<ItemStack> result = new ArrayList<>();
        PocketInventory pockets = get(player);
        for (int slot = 0; slot < capacity(player); slot++) result.add(pockets.getItem(slot));
        return result;
    }

    public static List<ItemStack> carriedItems(Player player) {
        List<ItemStack> result = items(player);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            result.add(player.getInventory().getItem(slot));
        }
        return result;
    }

    public List<ItemStack> snapshot() {
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < MAX_SIZE; slot++) result.add(this.getItem(slot).copy());
        return result;
    }

    public void tick(Player player) {
        int capacity = capacity(player);
        ItemStack equipped = player.getItemBySlot(EquipmentSlot.LEGS);
        int retained = this.wornLeggings != null && this.wornLeggings != equipped ? 0 : capacity;
        this.wornLeggings = equipped;
        for (int slot = retained; slot < MAX_SIZE; slot++) {
            ItemStack returned = this.removeItemNoUpdate(slot);
            if (!returned.isEmpty()) player.getInventory().placeItemBackInInventory(returned);
        }
        for (int slot = 0; slot < capacity; slot++) {
            ItemStack stack = this.getItem(slot);
            if (!stack.isEmpty()) stack.inventoryTick(player.level(), player, null);
        }
        this.syncChanges(player);
    }

    public void rememberLeggings(Player player) {
        if (this.wornLeggings == null) this.wornLeggings = player.getItemBySlot(EquipmentSlot.LEGS);
    }

    public void syncChanges(Player player) {
        boolean changed = this.lastSynced.size() != MAX_SIZE;
        for (int slot = 0; !changed && slot < MAX_SIZE; slot++) {
            changed = !ItemStack.matches(this.getItem(slot), this.lastSynced.get(slot));
        }
        if (changed) {
            this.lastSynced = this.snapshot();
            player.syncData(ModDataAttachments.POCKETS);
        }
    }

    public void dropAll(Player player) {
        for (int slot = 0; slot < MAX_SIZE; slot++) {
            ItemStack stack = this.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) player.drop(stack, true, false);
        }
    }
}
