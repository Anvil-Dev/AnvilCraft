package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.NotNull;

public class EmberAnvilMenu extends AnvilMenu {

    public EmberAnvilMenu(int containerId, Inventory playerInventory) {
        super(containerId, playerInventory);
    }

    public EmberAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(containerId, playerInventory, access);
    }

    @Override
    public @NotNull MenuType<?> getType() {
        return ModMenuTypes.EMBER_ANVIL.get();
    }

    @Override
    public void createResult() {
        ItemStack inputItemLeft = this.inputSlots.getItem(0);
        this.cost.set(1);
        int totalCost = 0;
        long repairCost = 0L;
        int repairCostT = 0;
        if (!inputItemLeft.isEmpty() && EnchantmentHelper.canStoreEnchantments(inputItemLeft)) {
            ItemStack inputItemLeftCopy = inputItemLeft.copy();
            ItemStack inputItemRight = this.inputSlots.getItem(1);
            ItemEnchantments.Mutable enchantmentsOnLeft =
                new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(inputItemLeftCopy));
            repairCost += (long) inputItemLeft.getOrDefault(DataComponents.REPAIR_COST, 0)
                + (long) inputItemRight.getOrDefault(DataComponents.REPAIR_COST, 0);
            this.repairItemCountCost = 0;
            boolean hasStoredEnchantmentsOnInput2 = false;
            //noinspection DataFlowIssue
            if (!CommonHooks.onAnvilChange(
                this, inputItemLeft, inputItemRight, this.resultSlots, this.itemName, repairCost, this.player)) {
                return;
            }

            int damage;
            int repairItemCountCost;

            ChatFormatting extraFormat = null;
            if (inputItemRight.is(Items.NAME_TAG) && !inputItemLeft.isEmpty()) {
                if (!inputItemRight.has(DataComponents.CUSTOM_NAME)) {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.cost.set(0);
                    return;
                }
                Component formattingText = inputItemRight.get(DataComponents.CUSTOM_NAME);
                if (formattingText == null) {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.cost.set(0);
                    return;
                }
                String format = formattingText.getString();
                if (format.startsWith("&") && format.length() >= 2) {
                    extraFormat =
                        ChatFormatting.getByCode(format.substring(1, 2).charAt(0));
                } else {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.cost.set(0);
                    return;
                }
            } else if (!inputItemRight.isEmpty()) {
                hasStoredEnchantmentsOnInput2 = inputItemRight.has(DataComponents.STORED_ENCHANTMENTS);
                int damageValue;
                if ((inputItemLeftCopy.isDamageableItem()
                    && inputItemLeftCopy.getItem().isValidRepairItem(inputItemLeft, inputItemRight))) {
                    damage = Math.min(inputItemLeftCopy.getDamageValue(), inputItemLeftCopy.getMaxDamage() / 4);
                    if (damage <= 0) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                        return;
                    }

                    for (repairItemCountCost = 0;
                         damage > 0 && repairItemCountCost < inputItemRight.getCount();
                         ++repairItemCountCost) {
                        damageValue = inputItemLeftCopy.getDamageValue() - damage;
                        inputItemLeftCopy.setDamageValue(damageValue);
                        ++totalCost;
                        damage = Math.min(inputItemLeftCopy.getDamageValue(), inputItemLeftCopy.getMaxDamage() / 4);
                    }

                    this.repairItemCountCost = repairItemCountCost;
                } else {
                    if (!hasStoredEnchantmentsOnInput2
                        && (!inputItemLeftCopy.is(inputItemRight.getItem())
                        || !inputItemLeftCopy.isDamageableItem())) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                        return;
                    }

                    if (inputItemLeftCopy.isDamageableItem() && !hasStoredEnchantmentsOnInput2) {
                        damage = inputItemLeft.getMaxDamage() - inputItemLeft.getDamageValue();
                        repairItemCountCost = inputItemRight.getMaxDamage() - inputItemRight.getDamageValue();
                        damageValue = repairItemCountCost + inputItemLeftCopy.getMaxDamage() * 12 / 100;
                        int k1 = damage + damageValue;
                        int l1 = inputItemLeftCopy.getMaxDamage() - k1;
                        if (l1 < 0) {
                            l1 = 0;
                        }

                        if (l1 < inputItemLeftCopy.getDamageValue()) {
                            inputItemLeftCopy.setDamageValue(l1);
                            totalCost += 2;
                        }
                    }

                    ItemEnchantments enchantmentsOnRight = EnchantmentHelper.getEnchantmentsForCrafting(inputItemRight);
                    for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantmentsOnRight.entrySet()) {
                        Holder<Enchantment> holder = entry.getKey();
                        int enchantmentsOnLeftLevel = enchantmentsOnLeft.getLevel(holder);
                        int enchantmentsOnRightLevel = entry.getIntValue();
                        Enchantment enchantment = holder.value();
                        enchantmentsOnRightLevel =
                            enchantmentsOnLeftLevel == enchantmentsOnRightLevel
                                ? enchantmentsOnRightLevel + 1
                                : Math.max(enchantmentsOnRightLevel, enchantmentsOnLeftLevel);

                        if (!AnvilCraft.config.emberAnvilBeyondMaxLevel && enchantmentsOnRightLevel > enchantment.getMaxLevel()) {
                            enchantmentsOnRightLevel = enchantment.getMaxLevel();
                        }

                        enchantmentsOnLeft.set(holder, enchantmentsOnRightLevel);
                        int anvilCost = enchantment.getAnvilCost();
                        if (hasStoredEnchantmentsOnInput2) {
                            anvilCost = Math.max(1, anvilCost / 2);
                        }

                        totalCost += Math.clamp((long) anvilCost * enchantmentsOnRightLevel, 0, Integer.MAX_VALUE);
                        if (inputItemLeft.getCount() > 1) {
                            totalCost = 99999999;
                        }
                    }
                }
            }

            if (extraFormat != null) {
                repairCostT = 1;
                totalCost += repairCostT
                    * inputItemLeft.getCount()
                    * inputItemRight.getCount();
                Component currentName = inputItemLeft.getHoverName();
                if (!this.itemName.equals(currentName.getString())
                    && this.itemName != null
                    && !this.itemName.isBlank()) {
                    currentName = Component.literal(this.itemName);
                }
                inputItemLeftCopy.set(DataComponents.CUSTOM_NAME, currentName.copy().withStyle(extraFormat));
            } else {
                if (this.itemName != null && !StringUtil.isBlank(this.itemName)) {
                    boolean nameChanged =
                        !this.itemName.equals(inputItemLeft.getHoverName().getString());
                    if (nameChanged) {
                        repairCostT = 1;
                        totalCost += repairCostT;
                        Component name = Component.literal(this.itemName);
                        inputItemLeftCopy.set(DataComponents.CUSTOM_NAME, name);
                    }
                } else {
                    if (inputItemLeft.has(DataComponents.CUSTOM_NAME)) {
                        repairCostT = 1;
                        totalCost += repairCostT;
                        inputItemLeftCopy.remove(DataComponents.CUSTOM_NAME);
                    }
                }
            }

            if (hasStoredEnchantmentsOnInput2 && !inputItemLeftCopy.isBookEnchantable(inputItemRight)) {
                inputItemLeftCopy = ItemStack.EMPTY;
            }

            damage = Math.clamp(repairCost + (long) totalCost, 0, Integer.MAX_VALUE);
            this.cost.set(damage);
            if (totalCost <= 0) {
                inputItemLeftCopy = ItemStack.EMPTY;
            }

            if (repairCostT == totalCost && repairCostT > 0 && this.cost.get() >= 40) {
                this.cost.set(39);
            }

            if (!inputItemLeftCopy.isEmpty()) {
                repairItemCountCost = inputItemLeftCopy.getOrDefault(DataComponents.REPAIR_COST, 0);
                if (repairItemCountCost < inputItemRight.getOrDefault(DataComponents.REPAIR_COST, 0)) {
                    repairItemCountCost = inputItemRight.getOrDefault(DataComponents.REPAIR_COST, 0);
                }

                if (repairCostT != totalCost || repairCostT == 0) {
                    repairItemCountCost = calculateIncreasedRepairCost(repairItemCountCost);
                }

                inputItemLeftCopy.set(DataComponents.REPAIR_COST, repairItemCountCost);
                EnchantmentHelper.setEnchantments(inputItemLeftCopy, enchantmentsOnLeft.toImmutable());
            }

            this.resultSlots.setItem(0, inputItemLeftCopy);
            this.broadcastChanges();
        } else {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.cost.set(0);
        }
    }
}
