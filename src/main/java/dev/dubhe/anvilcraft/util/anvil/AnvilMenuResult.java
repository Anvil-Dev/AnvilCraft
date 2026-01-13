package dev.dubhe.anvilcraft.util.anvil;

import dev.dubhe.anvilcraft.init.item.ModItems;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.LongPredicate;
import java.util.function.ToIntFunction;

public class AnvilMenuResult {
    private final boolean ignoreEnchantmentCompatible;
    private final boolean allowBeyondMaxLevel;
    private final boolean allowEnchantingMultipleItems;
    private final boolean allowUsingFrostMetalToRepair;
    public final boolean noCostInRenaming;
    private final boolean noTaxInRepairUsingItem;
    private final boolean useNewRepairCostAlgorithm;
    public int xpCost = 0;
    public int repairItemCountCost = 0;
    public ItemStack result = ItemStack.EMPTY;
    public boolean onlyRenaming;
    private boolean shouldCancel;

    private AnvilMenuResult(
        boolean ignoreEnchantmentCompatible,
        boolean allowBeyondMaxLevel,
        boolean allowEnchantingMultipleItems,
        boolean allowUsingFrostMetalToRepair,
        boolean noCostInRenaming,
        boolean noTaxInRepairUsingItem,
        boolean useNewRepairCostAlgorithm
    ) {
        this.ignoreEnchantmentCompatible = ignoreEnchantmentCompatible;
        this.allowBeyondMaxLevel = allowBeyondMaxLevel;
        this.allowEnchantingMultipleItems = allowEnchantingMultipleItems;
        this.allowUsingFrostMetalToRepair = allowUsingFrostMetalToRepair;
        this.noCostInRenaming = noCostInRenaming;
        this.noTaxInRepairUsingItem = noTaxInRepairUsingItem;
        this.useNewRepairCostAlgorithm = useNewRepairCostAlgorithm;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void createResult(
        Player player,
        ItemStack inputLeft,
        ItemStack inputRight,
        @Nullable String itemName,
        LongPredicate onAnvilChangeEventSender
    ) {
        this.shouldCancel = false;
        this.onlyRenaming = false;

        // 左侧为空或无法存储魔咒，则返回
        if (inputLeft.isEmpty() || !EnchantmentHelper.canStoreEnchantments(inputLeft)) {
            this.result = ItemStack.EMPTY;
            return;
        }
        
        // 变量初始化
        int price = 0;
        int repairingCost = 0;
        long tax = (long) inputLeft.getOrDefault(DataComponents.REPAIR_COST, 0)
                  + (long) inputRight.getOrDefault(DataComponents.REPAIR_COST, 0);
        ItemStack result = inputLeft.copy();
        final ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(result));
        boolean usingBook = false;

        // 发送事件，若事件取消，则返回
        if (!onAnvilChangeEventSender.test(tax)) {
            this.result = ItemStack.EMPTY;
            return;
        }

        // 若右侧为命名牌，则尝试获取特殊格式，否则进入魔咒逻辑
        ChatFormatting extraFormat = null;
        if (inputRight.is(Items.NAME_TAG)) {
            extraFormat = this.computeExtraFormatting(inputRight);
            if (this.shouldCancel) {
                this.result = ItemStack.EMPTY;
                return;
            }
        } else if (!inputRight.isEmpty()) {
            usingBook = inputRight.has(DataComponents.STORED_ENCHANTMENTS);
            
            // 若左侧可损失耐久且右侧为合适的修复材料，则尝试使用材料修复耐久度，否则尝试合并左右耐久度和魔咒
            // 仅当允许使用浮霜金属修复时才会允许浮霜金属作为修复材料
            if (
                result.isDamageableItem()
                && (
                    result.getItem().isValidRepairItem(inputLeft, inputRight)
                    || (
                        this.allowUsingFrostMetalToRepair
                        && (
                            inputRight.is(ModItems.FROST_METAL_INGOT) 
                            || inputRight.is(ModItems.FROST_METAL_NUGGET)
                        )
                    )
                )
            ) {
                ToIntFunction<ItemStack> nextComputer;
                if (this.allowUsingFrostMetalToRepair) {
                    if (inputRight.is(ModItems.FROST_METAL_INGOT)) {
                        nextComputer = result1 -> 1080;
                    } else if (inputRight.is(ModItems.FROST_METAL_NUGGET)) {
                        nextComputer = result1 -> 120;
                    } else {
                        nextComputer = result1 -> result1.getMaxDamage() / 4;
                    }
                } else {
                    nextComputer = result1 -> result1.getMaxDamage() / 4;
                }
                repairingCost = this.repairUsingItem(inputRight, result, nextComputer);
                price += repairingCost;
            } else {
                // 若未使用附魔书且左右不是同种物品或左侧不可损失耐久度，则返回
                if (!usingBook && (!result.is(inputRight.getItem()) || !result.isDamageableItem())) return;

                if (result.isDamageableItem() && !usingBook) {
                    price = this.combineDurability(inputLeft, inputRight, result, price);
                }

                price = this.applyEnchantment(player, inputLeft, inputRight, enchantments, usingBook, price);
            }
            if (this.shouldCancel) {
                this.result = ItemStack.EMPTY;
                return;
            }
        }
        this.xpCost = 1;

        // 尝试重命名
        RenamingResult renamingResult = this.renaming(
            inputLeft,
            inputRight,
            itemName,
            extraFormat,
            price,
            result
        );

        // 若左侧不可使用附魔书附魔且右侧为附魔书，则返回
        if (usingBook && !result.isBookEnchantable(inputRight)) {
            this.result = ItemStack.EMPTY;
            return;
        }

        // 计算最终经验消耗
        price = Math.clamp(tax + (long) renamingResult.price(), 0, Integer.MAX_VALUE);
        this.xpCost = price;
        // 若重命名不消耗经验但没有重命名，或经验消耗为空，则返回
        if ((!this.noCostInRenaming || !this.onlyRenaming) && price - tax <= 0) {
            this.result = ItemStack.EMPTY;
            return;
        }

        // 检查仅重命名时，惩罚是否超出上限（40级）
        this.checkRenamingCostOverflow(price, tax, renamingResult.namingCost());

        if (result.isEmpty()) {
            this.result = ItemStack.EMPTY;
            return;
        }

        // 计算最终惩罚并应用到结果
        result.set(
            DataComponents.REPAIR_COST,
            this.calculateFinalRepairCost(inputRight, result, renamingResult.namingCost, repairingCost, price)
        );
        EnchantmentHelper.setEnchantments(result, enchantments.toImmutable());
        this.result = result;
    }

    private @Nullable ChatFormatting computeExtraFormatting(ItemStack inputRight) {
        if (!inputRight.has(DataComponents.CUSTOM_NAME)) {
            this.shouldCancel = true;
            return null;
        }

        Component formattingText = inputRight.get(DataComponents.CUSTOM_NAME);
        if (formattingText == null) {
            this.shouldCancel = true;
            return null;
        }

        String format = formattingText.getString();
        if (!format.startsWith("&") || format.length() < 2) {
            this.shouldCancel = true;
            return null;
        }

        return ChatFormatting.getByCode(format.substring(1, 2).charAt(0));
    }

    private int repairUsingItem(ItemStack inputRight, ItemStack result, ToIntFunction<ItemStack> nextComputer) {
        int repairAmount = Math.min(result.getDamageValue(), nextComputer.applyAsInt(result));
        if (repairAmount <= 0) {
            this.shouldCancel = true;
            return 0;
        }

        int xpCost = 0;
        int repairItemCountCost;
        for (
            repairItemCountCost = 0;
            repairAmount > 0 && repairItemCountCost < inputRight.getCount();
            ++repairItemCountCost
        ) {
            repairAmount = result.getDamageValue() - repairAmount;
            result.setDamageValue(repairAmount);
            ++xpCost;
            repairAmount = Math.min(result.getDamageValue(), nextComputer.applyAsInt(result));
        }

        this.repairItemCountCost = repairItemCountCost;
        return xpCost;
    }

    private int combineDurability(ItemStack inputLeft, ItemStack inputRight, ItemStack result, int price) {
        int remainingLeft = inputLeft.getMaxDamage() - inputLeft.getDamageValue();
        int remainingRight = inputRight.getMaxDamage() - inputRight.getDamageValue();
        int adding = remainingRight + result.getMaxDamage() * 12 / 100;
        int remaining = remainingLeft + adding;
        int resultDamage = Math.max(result.getMaxDamage() - remaining, 0);

        if (resultDamage < result.getDamageValue()) {
            result.setDamageValue(resultDamage);
            price += 2;
        }
        return price;
    }

    private int applyEnchantment(
        Player player,
        ItemStack inputLeft,
        ItemStack inputRight,
        ItemEnchantments.Mutable enchantments,
        boolean usingBook,
        int price
    ) {
        ItemEnchantments enchantmentsOnRight = EnchantmentHelper.getEnchantmentsForCrafting(inputRight);
        boolean isAnyCompatible = false;
        boolean isAnyNotCompatible = false;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantmentsOnRight.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            int enchantmentsOnLeftLevel = enchantments.getLevel(holder);
            int enchantmentsOnRightLevel = entry.getIntValue();
            Enchantment enchantment = holder.value();
            enchantmentsOnRightLevel =
                enchantmentsOnLeftLevel == enchantmentsOnRightLevel
                ? enchantmentsOnRightLevel + 1
                : Math.max(enchantmentsOnRightLevel, enchantmentsOnLeftLevel);

            if (!this.ignoreEnchantmentCompatible) {
                boolean compatible = inputLeft.supportsEnchantment(holder);
                if (player.hasInfiniteMaterials() || inputLeft.is(Items.ENCHANTED_BOOK)) {
                    compatible = true;
                }

                for (Holder<Enchantment> other : enchantments.keySet()) {
                    if (!other.equals(holder) && !Enchantment.areCompatible(holder, other)) {
                        compatible = false;
                        ++price;
                    }
                }

                if (!compatible) {
                    isAnyNotCompatible = true;
                    continue;
                } else {
                    isAnyCompatible = true;
                }

            }

            if (!this.allowBeyondMaxLevel && enchantmentsOnRightLevel > enchantment.getMaxLevel()) {
                enchantmentsOnRightLevel = enchantment.getMaxLevel();
            }

            enchantments.set(holder, enchantmentsOnRightLevel);
            int anvilCost = enchantment.getAnvilCost();
            if (usingBook) {
                anvilCost = Math.max(1, anvilCost / 2);
            }

            price += Math.clamp(
                (long) anvilCost
                * enchantmentsOnRightLevel
                * inputLeft.getCount() * inputLeft.getCount(),
                0,
                Integer.MAX_VALUE
            );
            if (!this.allowEnchantingMultipleItems && inputLeft.getCount() > 1) {
                price = 99999999;
            }
        }

        if (isAnyNotCompatible && !isAnyCompatible) this.shouldCancel = true;
        return price;
    }

    private @NotNull AnvilMenuResult.RenamingResult renaming(
        ItemStack inputLeft,
        ItemStack inputRight,
        @Nullable String itemName,
        ChatFormatting extraFormat,
        int price,
        ItemStack result
    ) {
        int namingCost = 0;
        if (extraFormat != null) {
            if (!this.noCostInRenaming) {
                namingCost = 1;
                price += inputLeft.getCount() * inputRight.getCount();
            }
            Component currentName = inputLeft.getHoverName();
            if (!Objects.equals(itemName, currentName.getString())
                && itemName != null
                && !itemName.isBlank()) {
                currentName = Component.literal(itemName);
            }
            result.set(DataComponents.CUSTOM_NAME, currentName.copy().withStyle(extraFormat));
            this.onlyRenaming = true;
        } else if (itemName != null && !StringUtil.isBlank(itemName)) {
            if (!itemName.equals(inputLeft.getHoverName().getString())) {
                if (!this.noCostInRenaming) {
                    namingCost = 1;
                    price += namingCost;
                }
                Component name = Component.literal(itemName);
                result.set(DataComponents.CUSTOM_NAME, name);
                this.onlyRenaming = true;
            }
        } else {
            if (inputLeft.has(DataComponents.CUSTOM_NAME)) {
                if (!this.noCostInRenaming) {
                    namingCost = 1;
                    price += namingCost;
                }
                result.remove(DataComponents.CUSTOM_NAME);
                this.onlyRenaming = true;
            }
        }
        return new RenamingResult(price, namingCost);
    }

    private record RenamingResult(int price, int namingCost) {
    }

    private void checkRenamingCostOverflow(int price, long tax, int namingCost) {
        if (this.noCostInRenaming && this.onlyRenaming && (namingCost == price || namingCost == price - tax)) {
            this.xpCost = 0;
        } else if (namingCost == price && namingCost > 0 && this.xpCost >= 40) {
            this.xpCost = 39;
        } else {
            this.onlyRenaming = false;
        }
    }

    private int calculateFinalRepairCost(ItemStack inputRight, ItemStack result, int namingCost, int repairingCost, int price) {
        if (price == 0) return 0;
        if (!this.useNewRepairCostAlgorithm) {
            int baseCost = result.getOrDefault(DataComponents.REPAIR_COST, 0);
            if (baseCost < inputRight.getOrDefault(DataComponents.REPAIR_COST, 0)) {
                baseCost = inputRight.getOrDefault(DataComponents.REPAIR_COST, 0);
            }

            if (namingCost == price || namingCost == price - baseCost || this.noTaxInRepairUsingItem && repairingCost == price - baseCost) {
                return baseCost;
            }
            return AnvilMenu.calculateIncreasedRepairCost(baseCost);
        } else {
            int baseCost = result.getOrDefault(DataComponents.REPAIR_COST, 0)
                       + inputRight.getOrDefault(DataComponents.REPAIR_COST, 0);
            return namingCost == price || namingCost == price - baseCost || this.noTaxInRepairUsingItem && repairingCost == price - baseCost
                   ? baseCost
                   : ++baseCost;
        }
    }
    
    @Accessors(fluent = true)
    @Setter
    public static class Builder {
        private boolean ignoreEnchantmentCompatible = false;
        private boolean allowBeyondMaxLevel = false;
        private boolean allowEnchantingMultipleItems = false;
        private boolean allowUsingFrostMetalToRepair = false;
        private boolean noCostInRenaming = false;
        private boolean noTaxInRepairUsingItem = false;
        private boolean useNewRepairCostAlgorithm = false;
        
        public Builder ignoreEnchantmentCompatible() {
            this.ignoreEnchantmentCompatible = true;
            return this;
        }

        public Builder allowBeyondMaxLevel() {
            this.allowBeyondMaxLevel = true;
            return this;
        }

        public Builder allowEnchantingMultipleItems() {
            this.allowEnchantingMultipleItems = true;
            return this;
        }

        public Builder allowUsingFrostMetalToRepair() {
            this.allowUsingFrostMetalToRepair = true;
            return this;
        }

        public Builder noCostInRenaming() {
            this.noCostInRenaming = true;
            return this;
        }

        public Builder noTaxInRepairUsingItem() {
            this.noTaxInRepairUsingItem = true;
            return this;
        }

        public Builder useNewRepairCostAlgorithm() {
            this.useNewRepairCostAlgorithm = true;
            return this;
        }

        public AnvilMenuResult create() {
            return new AnvilMenuResult(
                this.ignoreEnchantmentCompatible,
                this.allowBeyondMaxLevel,
                this.allowEnchantingMultipleItems,
                this.allowUsingFrostMetalToRepair,
                this.noCostInRenaming,
                this.noTaxInRepairUsingItem,
                this.useNewRepairCostAlgorithm
            );
        }
    }
}
