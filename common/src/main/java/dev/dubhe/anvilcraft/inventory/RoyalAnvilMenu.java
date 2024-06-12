package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.item.ICursed;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
        ItemStack inputItem1 = this.inputSlots.getItem(0);
        this.cost.set(1);
        if (inputItem1.isEmpty()) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.cost.set(0);
            return;
        }
        ItemStack outputItem = inputItem1.copy();
        ItemStack inputItem2 = this.inputSlots.getItem(1);
        Map<Enchantment, Integer> applicableEnchantments =
                EnchantmentHelper.getEnchantments(outputItem);
        boolean textChanged = false;
        int totalCost = 0;
        int j = 0;
        j += inputItem1.getBaseRepairCost()
                + (inputItem2.isEmpty() ? 0 : inputItem2.getBaseRepairCost());
        this.repairItemCountCost = 0;
        if (!inputItem2.isEmpty()) {
            boolean hasEnchantedBook = inputItem2.is(Items.ENCHANTED_BOOK)
                    && !EnchantedBookItem.getEnchantments(inputItem2).isEmpty();
            // 修理物品
            if (outputItem.isDamageableItem()
                    && outputItem.getItem().isValidRepairItem(inputItem1, inputItem2)) {
                int cost;
                int l = Math.min(outputItem.getDamageValue(), outputItem.getMaxDamage() / 4);
                if (l <= 0) {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.cost.set(0);
                    return;
                }
                for (cost = 0; l > 0 && cost < inputItem2.getCount(); ++cost) {
                    int n = outputItem.getDamageValue() - l;
                    outputItem.setDamageValue(n);
                    ++totalCost;
                    l = Math.min(outputItem.getDamageValue(), outputItem.getMaxDamage() / 4);
                }
                this.repairItemCountCost = cost;
            } else {
                if (inputItem2.is(Items.NAME_TAG)) {
                    Component name = inputItem2.getHoverName();
                    String fmt = name.getString();
                    if (fmt.startsWith("&") && fmt.length() >= 2) {

                        char styleCh = fmt.charAt(1);
                        ChatFormatting formatting = ChatFormatting.getByCode(styleCh);
                        if (formatting == null) return;
                        if (outputItem != null && !outputItem.is(Items.AIR)) {
                            MutableComponent originalText = outputItem.getHoverName().copy();
                            outputItem.setHoverName(
                                    originalText.setStyle(originalText.getStyle().applyFormat(formatting)));
                            textChanged = true;
                        }
                    }
                } else {
                    // 附魔物品
                    if (!(hasEnchantedBook
                            || outputItem.is(inputItem2.getItem()) && outputItem.isDamageableItem())) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                        return;
                    }
                }
                if (outputItem.isDamageableItem() && !hasEnchantedBook) {
                    int l = inputItem1.getMaxDamage() - inputItem1.getDamageValue();
                    int m = inputItem2.getMaxDamage() - inputItem2.getDamageValue();
                    int n = m + outputItem.getMaxDamage() * 12 / 100;
                    int o = l + n;
                    int p = outputItem.getMaxDamage() - o;
                    if (p < 0) {
                        p = 0;
                    }
                    if (p < outputItem.getDamageValue()) {
                        outputItem.setDamageValue(p);
                        totalCost += 2;
                    }
                }
                Map<Enchantment, Integer> enchantmentsInInput2 =
                        EnchantmentHelper.getEnchantments(inputItem2);
                boolean canApplyEnchantment = false;
                boolean bl3 = false;
                for (Enchantment enchantment : enchantmentsInInput2.keySet()) {
                    int level;
                    if (enchantment == null) continue;
                    int originalLevel = applicableEnchantments.getOrDefault(enchantment, 0);
                    level = enchantmentsInInput2.get(enchantment);
                    level = originalLevel == level ? level + 1 : Math.max(level, originalLevel);
                    boolean hasNoIncompatibleEnchantment = true;
                    for (Enchantment enchantment2 : applicableEnchantments.keySet()) {
                        if (enchantment2 == enchantment || enchantment.isCompatibleWith(enchantment2)) continue;
                        hasNoIncompatibleEnchantment = false;
                        ++totalCost;
                    }
                    if (!hasNoIncompatibleEnchantment) {
                        bl3 = true;
                        continue;
                    }
                    canApplyEnchantment = true;
                    if (level > enchantment.getMaxLevel()) level = enchantment.getMaxLevel();
                    applicableEnchantments.put(enchantment, level);
                    int rarityCostFactor =
                            switch (enchantment.getRarity()) {
                                case COMMON -> 1;
                                case UNCOMMON -> 2;
                                case RARE -> 4;
                                case VERY_RARE -> 8;
                            };
                    if (hasEnchantedBook) rarityCostFactor = Math.max(1, rarityCostFactor / 2);
                    totalCost += rarityCostFactor * level;
                }
                if (bl3 && !canApplyEnchantment) {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.cost.set(0);
                    return;
                }
            }
        }
        int k = 0;
        // 重命名物品
        if (textChanged) {
            k = 1;
            totalCost += k;
            if (this.itemName == null || Util.isBlank(this.itemName)) {
                outputItem.setHoverName(
                        outputItem.getHoverName().copy().setStyle(outputItem.getHoverName().getStyle()));
            } else {
                outputItem.setHoverName(
                        Component.literal(this.itemName).setStyle(outputItem.getHoverName().getStyle()));
            }

        } else {
            if (this.itemName == null || Util.isBlank(this.itemName)) {
                if (inputItem1.hasCustomHoverName()) {
                    k = 1;
                    totalCost += k;
                    outputItem.resetHoverName();
                }
            } else if (!this.itemName.equals(inputItem1.getHoverName().getString())) {
                k = 1;
                totalCost += k;
                outputItem.setHoverName(
                        Component.literal(this.itemName).setStyle(outputItem.getHoverName().getStyle()));
            }
        }

        int count = inputItem1.getCount();
        this.cost.set(j + totalCost * (int) Math.pow(count, 2));
        if (totalCost <= 0) outputItem = ItemStack.EMPTY;
        if (!outputItem.isEmpty()) {
            int t = outputItem.getBaseRepairCost();
            if (!inputItem2.isEmpty() && t < inputItem2.getBaseRepairCost()) {
                t = inputItem2.getBaseRepairCost();
            }
            if (k != totalCost || k == 0) {
                t = AnvilMenu.calculateIncreasedRepairCost(t);
            }
            outputItem.setRepairCost(t);
            EnchantmentHelper.setEnchantments(applicableEnchantments, outputItem);
        }
        this.resultSlots.setItem(0, outputItem);
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
