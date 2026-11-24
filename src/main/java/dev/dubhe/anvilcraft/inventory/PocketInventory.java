package dev.dubhe.anvilcraft.inventory;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.init.ModAttributes;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PocketInventory extends SimpleContainer {
    /**
     * 菜单槽位上限，而不是默认容量：菜单槽位索引不能在运行期增删，因此按上限一次性注册，
     * 实际可用格数由 {@code pocket_count} 属性决定。快照与同步也按此上限收发。
     */
    public static final int MAX_SIZE = 32;
    public static final Codec<PocketInventory> CODEC = ItemStack.OPTIONAL_CODEC.listOf()
        .xmap(PocketInventory::new, PocketInventory::snapshot);
    public static final StreamCodec<RegistryFriendlyByteBuf, PocketInventory> STREAM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC
        .apply(ByteBufCodecs.list(PocketInventory.MAX_SIZE)).map(PocketInventory::new, PocketInventory::snapshot);
    private List<ItemStack> lastSynced = List.of();
    private @Nullable ItemStack wornLeggings;

    public PocketInventory() {
        super(PocketInventory.MAX_SIZE);
    }

    private PocketInventory(List<ItemStack> items) {
        this();
        for (int slot = 0; slot < Math.min(items.size(), PocketInventory.MAX_SIZE); slot++) {
            this.setItem(slot, items.get(slot).copy());
        }
    }

    public static PocketInventory get(Player player) {
        return player.getData(ModDataAttachments.POCKETS);
    }

    public static int capacity(Player player) {
        AttributeInstance instance = player.getAttribute(ModAttributes.POCKET_COUNT);
        if (instance == null) {
            return 0;
        }
        return Mth.clamp(Mth.floor(instance.getValue()), 0, PocketInventory.MAX_SIZE);
    }

    /**
     * 判断更换护腿是否会丢失已占用的口袋。只有「本次换装后仍会超出的物品」才算丢失，
     * 因此从不提供容量的护腿换成另一件普通护腿不会被拦截。
     *
     * @param target 将要穿上的护腿，取出时为 {@link ItemStack#EMPTY}
     */
    public static boolean blocksLeggingsChange(Player player, ItemStack target) {
        int next = Mth.clamp(PocketInventory.capacityWithoutLeggings(player) + PocketInventory.pocketCountOf(target), 0, MAX_SIZE);
        PocketInventory pockets = PocketInventory.get(player);
        for (int slot = next; slot < MAX_SIZE; slot++) {
            if (!pockets.getItem(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 读取一件物品在护腿槽能提供的口袋数量。走属性修饰符而不是读物品字段，
     * 使第三方提供的护腿同样被正确识别。
     */
    public static int pocketCountOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return Mth.clamp(Mth.floor(PocketInventory.pocketCountOf(stack.getAttributeModifiers())), 0, MAX_SIZE);
    }

    private static double pocketCountOf(ItemAttributeModifiers modifiers) {
        double total = 0;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() != ModAttributes.POCKET_COUNT.get()
                || !entry.slot().test(EquipmentSlot.LEGS)
                || entry.modifier().operation() != AttributeModifier.Operation.ADD_VALUE) {
                continue;
            }
            total += entry.modifier().amount();
        }
        return total;
    }

    private static int capacityWithoutLeggings(Player player) {
        return PocketInventory.capacity(player) - PocketInventory.pocketCountOf(player.getItemBySlot(EquipmentSlot.LEGS));
    }

    public static List<ItemStack> items(Player player) {
        List<ItemStack> result = new ArrayList<>();
        PocketInventory pockets = PocketInventory.get(player);
        for (int slot = 0; slot < PocketInventory.capacity(player); slot++) {
            result.add(pockets.getItem(slot));
        }
        return result;
    }

    public static List<ItemStack> carriedItems(Player player) {
        List<ItemStack> result = PocketInventory.items(player);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            result.add(player.getInventory().getItem(slot));
        }
        return result;
    }

    public List<ItemStack> snapshot() {
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < PocketInventory.MAX_SIZE; slot++) {
            result.add(this.getItem(slot).copy());
        }
        return result;
    }

    public void tick(Player player) {
        int capacity = PocketInventory.capacity(player);
        ItemStack equipped = player.getItemBySlot(EquipmentSlot.LEGS);
        int retained = this.wornLeggings != null && this.wornLeggings != equipped ? 0 : capacity;
        this.wornLeggings = equipped;
        for (int slot = retained; slot < PocketInventory.MAX_SIZE; slot++) {
            ItemStack returned = this.removeItemNoUpdate(slot);
            if (!returned.isEmpty()) {
                player.getInventory().placeItemBackInInventory(returned);
            }
        }
        for (int slot = 0; slot < capacity; slot++) {
            ItemStack stack = this.getItem(slot);
            if (!stack.isEmpty()) {
                stack.inventoryTick(player.level(), player, null);
            }
        }
        this.syncChanges(player);
    }

    public void rememberLeggings(Player player) {
        if (this.wornLeggings == null) {
            this.wornLeggings = player.getItemBySlot(EquipmentSlot.LEGS);
        }
    }

    public void syncChanges(Player player) {
        boolean changed = this.lastSynced.size() != PocketInventory.MAX_SIZE;
        for (int slot = 0; !changed && slot < PocketInventory.MAX_SIZE; slot++) {
            changed = !ItemStack.matches(this.getItem(slot), this.lastSynced.get(slot));
        }
        if (changed) {
            this.lastSynced = this.snapshot();
            player.syncData(ModDataAttachments.POCKETS);
        }
    }

    public void dropAll(Player player) {
        for (int slot = 0; slot < PocketInventory.MAX_SIZE; slot++) {
            ItemStack stack = this.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) {
                player.drop(stack, true, false);
            }
        }
    }
}
