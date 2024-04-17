package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.item.ICursed;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class RoyalAnvilMenu extends AnvilMenu {
    public RoyalAnvilMenu(int containerId, Inventory playerInventory) {
        super(containerId, playerInventory);
    }

    public RoyalAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(containerId, playerInventory, access);
    }

    @Override
    public @NotNull MenuType<?> getType() {
        return ModMenuTypes.ROYAL_ANVIL.get();
    }

    @Override
    public void createResult() {
        ItemStack itemStack = this.inputSlots.getItem(0);
        this.cost.set(1);
        if (itemStack.isEmpty()) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.cost.set(0);
            return;
        }
        ItemStack itemStack2 = itemStack.copy();
        ItemStack itemStack3 = this.inputSlots.getItem(1);
        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(itemStack2);
        int i = 0;
        int j = 0;
        j += itemStack.getBaseRepairCost() + (itemStack3.isEmpty() ? 0 : itemStack3.getBaseRepairCost());
        this.repairItemCountCost = 0;
        if (!itemStack3.isEmpty()) {
            boolean bl;
            bl = itemStack3.is(Items.ENCHANTED_BOOK) && !EnchantedBookItem.getEnchantments(itemStack3).isEmpty();
            if (itemStack2.isDamageableItem() && itemStack2.getItem().isValidRepairItem(itemStack, itemStack3)) {
                int m;
                int l = Math.min(itemStack2.getDamageValue(), itemStack2.getMaxDamage() / 4);
                if (l <= 0) {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.cost.set(0);
                    return;
                }
                for (m = 0; l > 0 && m < itemStack3.getCount(); ++m) {
                    int n = itemStack2.getDamageValue() - l;
                    itemStack2.setDamageValue(n);
                    ++i;
                    l = Math.min(itemStack2.getDamageValue(), itemStack2.getMaxDamage() / 4);
                }
                this.repairItemCountCost = m;
            } else {
                if (!(bl || itemStack2.is(itemStack3.getItem()) && itemStack2.isDamageableItem())) {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.cost.set(0);
                    return;
                }
                if (itemStack2.isDamageableItem() && !bl) {
                    int l = itemStack.getMaxDamage() - itemStack.getDamageValue();
                    int m = itemStack3.getMaxDamage() - itemStack3.getDamageValue();
                    int n = m + itemStack2.getMaxDamage() * 12 / 100;
                    int o = l + n;
                    int p = itemStack2.getMaxDamage() - o;
                    if (p < 0) {
                        p = 0;
                    }
                    if (p < itemStack2.getDamageValue()) {
                        itemStack2.setDamageValue(p);
                        i += 2;
                    }
                }
                Map<Enchantment, Integer> map2 = EnchantmentHelper.getEnchantments(itemStack3);
                boolean bl22 = false;
                boolean bl3 = false;
                for (Enchantment enchantment : map2.keySet()) {
                    int r;
                    if (enchantment == null) continue;
                    int q = map.getOrDefault(enchantment, 0);
                    r = q == (r = map2.get(enchantment)) ? r + 1 : Math.max(r, q);
                    boolean bl4 = true;
                    for (Enchantment enchantment2 : map.keySet()) {
                        if (enchantment2 == enchantment || enchantment.isCompatibleWith(enchantment2)) continue;
                        bl4 = false;
                        ++i;
                    }
                    if (!bl4) {
                        bl3 = true;
                        continue;
                    }
                    bl22 = true;
                    if (r > enchantment.getMaxLevel()) r = enchantment.getMaxLevel();
                    map.put(enchantment, r);
                    int s = switch (enchantment.getRarity()) {
                        case COMMON -> 1;
                        case UNCOMMON -> 2;
                        case RARE -> 4;
                        case VERY_RARE -> 8;
                    };
                    if (bl) s = Math.max(1, s / 2);
                    i += s * r;
                }
                if (bl3 && !bl22) {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.cost.set(0);
                    return;
                }
            }
        }
        int k = 0;
        if (this.itemName == null || Util.isBlank(this.itemName)) {
            if (itemStack.hasCustomHoverName()) {
                k = 1;
                i += k;
                itemStack2.resetHoverName();
            }
        } else if (!this.itemName.equals(itemStack.getHoverName().getString())) {
            k = 1;
            i += k;
            itemStack2.setHoverName(Component.literal(this.itemName));
        }
        int count = itemStack.getCount();
        this.cost.set(j + i * (int) Math.pow(count, 2));
        if (i <= 0) itemStack2 = ItemStack.EMPTY;
        if (!itemStack2.isEmpty()) {
            int t = itemStack2.getBaseRepairCost();
            if (!itemStack3.isEmpty() && t < itemStack3.getBaseRepairCost()) {
                t = itemStack3.getBaseRepairCost();
            }
            if (k != i || k == 0) {
                t = AnvilMenu.calculateIncreasedRepairCost(t);
            }
            itemStack2.setRepairCost(t);
            EnchantmentHelper.setEnchantments(map, itemStack2);
        }
        this.resultSlots.setItem(0, itemStack2);
        this.broadcastChanges();
    }

    @Override
    protected void onTake(@NotNull Player player, @NotNull ItemStack stack) {
        super.onTake(player, stack);
        Level level = player.level();
        if (level.isClientSide()) return;
        int curedNumber = ICursed.hasCuredNumber(player);
        if (curedNumber <= 0) return;
        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setPos(player.getX(), player.getY(), player.getZ());
        level.addFreshEntity(bolt);
    }
}
