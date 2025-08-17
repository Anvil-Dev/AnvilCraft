package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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

public class TranscendenceAnvilMenu extends AnvilMenu {

    public TranscendenceAnvilMenu(int containerId, Inventory playerInventory) {
        super(containerId, playerInventory);
    }

    public TranscendenceAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(containerId, playerInventory, access);
    }

    @Override
    public @NotNull MenuType<?> getType() {
        return ModMenuTypes.TRANSCENDENCE_ANVIL.get();
    }

    @Override
    public void createResult() {
        ItemStack inputLeft = this.inputSlots.getItem(0);
        this.cost.set(1);
        int totalCost = 0;
        long repairCost = 0L;
        int repairCostT = 0;
        if (!inputLeft.isEmpty() && EnchantmentHelper.canStoreEnchantments(inputLeft)) {
            ItemStack inputLeftCopy = inputLeft.copy();
            ItemStack inputRight = this.inputSlots.getItem(1);
            ItemEnchantments.Mutable enchantmentsOnLeft =
                new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(inputLeftCopy));
            repairCost += (long) inputLeft.getOrDefault(DataComponents.REPAIR_COST, 0)
                + (long) inputRight.getOrDefault(DataComponents.REPAIR_COST, 0);
            this.repairItemCountCost = 0;
            boolean hasStoredEnchantmentsOnInput2 = false;
            //noinspection DataFlowIssue
            if (!CommonHooks.onAnvilChange(
                this, inputLeft, inputRight, this.resultSlots, this.itemName, repairCost, this.player)) {
                return;
            }

            int damage;
            int repairItemCountCost;

            ChatFormatting extraFormat = null;
            if (inputRight.is(Items.NAME_TAG) && !inputLeft.isEmpty()) {
                if (!inputRight.has(DataComponents.CUSTOM_NAME)) {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.cost.set(0);
                    return;
                }
                Component formattingText = inputRight.get(DataComponents.CUSTOM_NAME);
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
            } else if (!inputRight.isEmpty()) {
                hasStoredEnchantmentsOnInput2 = inputRight.has(DataComponents.STORED_ENCHANTMENTS);
                int damageValue;
                if ((inputLeftCopy.isDamageableItem()
                    && inputLeftCopy.getItem().isValidRepairItem(inputLeft, inputRight))) {
                    damage = Math.min(inputLeftCopy.getDamageValue(), inputLeftCopy.getMaxDamage() / 4);
                    if (damage <= 0) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                        return;
                    }

                    for (repairItemCountCost = 0;
                         damage > 0 && repairItemCountCost < inputRight.getCount();
                         ++repairItemCountCost) {
                        damageValue = inputLeftCopy.getDamageValue() - damage;
                        inputLeftCopy.setDamageValue(damageValue);
                        ++totalCost;
                        damage = Math.min(inputLeftCopy.getDamageValue(), inputLeftCopy.getMaxDamage() / 4);
                    }

                    this.repairItemCountCost = repairItemCountCost;
                } else {
                    if (!hasStoredEnchantmentsOnInput2
                        && (!inputLeftCopy.is(inputRight.getItem())
                        || !inputLeftCopy.isDamageableItem())) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                        return;
                    }

                    if (inputLeftCopy.isDamageableItem() && !hasStoredEnchantmentsOnInput2) {
                        damage = inputLeft.getMaxDamage() - inputLeft.getDamageValue();
                        repairItemCountCost = inputRight.getMaxDamage() - inputRight.getDamageValue();
                        damageValue = repairItemCountCost + inputLeftCopy.getMaxDamage() * 12 / 100;
                        int k1 = damage + damageValue;
                        int l1 = inputLeftCopy.getMaxDamage() - k1;
                        if (l1 < 0) {
                            l1 = 0;
                        }

                        if (l1 < inputLeftCopy.getDamageValue()) {
                            inputLeftCopy.setDamageValue(l1);
                            totalCost += 2;
                        }
                    }

                    ItemEnchantments enchantmentsOnRight = EnchantmentHelper.getEnchantmentsForCrafting(inputRight);
                    for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantmentsOnRight.entrySet()) {
                        Holder<Enchantment> holder = entry.getKey();
                        int enchantmentsOnLeftLevel = enchantmentsOnLeft.getLevel(holder);
                        int enchantmentsOnRightLevel = entry.getIntValue();
                        Enchantment enchantment = holder.value();
                        enchantmentsOnRightLevel =
                            enchantmentsOnLeftLevel == enchantmentsOnRightLevel
                                ? enchantmentsOnRightLevel + 1
                                : Math.max(enchantmentsOnRightLevel, enchantmentsOnLeftLevel);

                        if (!AnvilCraft.config.transcendenceAnvilBeyondMaxLevel && enchantmentsOnRightLevel > enchantment.getMaxLevel()) {
                            enchantmentsOnRightLevel = enchantment.getMaxLevel();
                        }

                        enchantmentsOnLeft.set(holder, enchantmentsOnRightLevel);
                        int anvilCost = enchantment.getAnvilCost();
                        if (hasStoredEnchantmentsOnInput2) {
                            anvilCost = Math.max(1, anvilCost / 2);
                        }

                        long cost = (long) anvilCost
                            * enchantmentsOnRightLevel
                            * inputLeft.getCount() * inputLeft.getCount();
                        totalCost += Math.clamp(cost, 0, Integer.MAX_VALUE);
                    }
                }
            }

            if (extraFormat != null) {
                repairCostT = 1;
                totalCost += repairCostT
                    * inputLeft.getCount()
                    * inputRight.getCount();
                Component currentName = inputLeft.getHoverName();
                if (!this.itemName.equals(currentName.getString())
                    && this.itemName != null
                    && !this.itemName.isBlank()) {
                    currentName = Component.literal(this.itemName);
                }
                inputLeftCopy.set(DataComponents.CUSTOM_NAME, currentName.copy().withStyle(extraFormat));
            } else {
                if (this.itemName != null && !StringUtil.isBlank(this.itemName)) {
                    boolean nameChanged =
                        !this.itemName.equals(inputLeft.getHoverName().getString());
                    if (nameChanged) {
                        repairCostT = 1;
                        totalCost += repairCostT;
                        Component name = Component.literal(this.itemName);
                        inputLeftCopy.set(DataComponents.CUSTOM_NAME, name);
                    }
                } else {
                    if (inputLeft.has(DataComponents.CUSTOM_NAME)) {
                        repairCostT = 1;
                        totalCost += repairCostT;
                        inputLeftCopy.remove(DataComponents.CUSTOM_NAME);
                    }
                }
            }

            if (hasStoredEnchantmentsOnInput2 && !inputLeftCopy.isBookEnchantable(inputRight)) {
                inputLeftCopy = ItemStack.EMPTY;
            }

            damage = Math.clamp(repairCost + (long) totalCost, 0, Integer.MAX_VALUE);
            this.cost.set(damage);
            if (totalCost <= 0) {
                inputLeftCopy = ItemStack.EMPTY;
            }

            if (repairCostT == totalCost && repairCostT > 0 && this.cost.get() >= 40) {
                this.cost.set(39);
            }

            if (!inputLeftCopy.isEmpty()) {
                repairItemCountCost = inputLeftCopy.getOrDefault(DataComponents.REPAIR_COST, 0);
                if (repairItemCountCost < inputRight.getOrDefault(DataComponents.REPAIR_COST, 0)) {
                    repairItemCountCost = inputRight.getOrDefault(DataComponents.REPAIR_COST, 0);
                }

                if (repairCostT != totalCost || repairCostT == 0) {
                    repairItemCountCost = calculateIncreasedRepairCost(repairItemCountCost);
                }

                inputLeftCopy.set(DataComponents.REPAIR_COST, repairItemCountCost);
                EnchantmentHelper.setEnchantments(inputLeftCopy, enchantmentsOnLeft.toImmutable());
            }

            this.resultSlots.setItem(0, inputLeftCopy);
            this.broadcastChanges();
        } else {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.cost.set(0);
        }
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        int costCache = this.cost.get();
        super.onTake(player, stack);
        if (costCache >= 5 && costCache < 15) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 6000, 1));
        } else if (costCache >= 15) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, 2));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 12000, 2));
        }
    }
}
