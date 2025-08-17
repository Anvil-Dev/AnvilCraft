package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.IMultipleResult;
import dev.dubhe.anvilcraft.api.item.property.Eternal;
import dev.dubhe.anvilcraft.api.item.property.Merciless;
import dev.dubhe.anvilcraft.api.item.property.Multiphase;
import dev.dubhe.anvilcraft.api.item.property.Providence;
import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import dev.dubhe.anvilcraft.entity.ThrownTranscendenceHeavyHalberdEntity;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.recipe.multiple.MultipleToOneSmithingRecipeInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import java.util.Objects;

public class TranscendenceHeavyHalberdItem extends HeavyHalberdItem implements IMultipleResult {
    public static final Multiphase DEFAULT_MULTIPHASE = Multiphase.make(
        Component.translatable("item.anvilcraft.transcendence_heavy_halberd")
    );

    public TranscendenceHeavyHalberdItem(Properties properties) {
        super(
            ModTiers.TRANSCENDIUM,
            properties.fireResistant()
                .attributes(HeavyHalberdItem.createAttributes(ModTiers.TRANSCENDIUM, 17, -2.4f))
                .component(ModComponents.MULTIPHASE, DEFAULT_MULTIPHASE.copy())
                .component(DataComponents.ITEM_NAME, Objects.requireNonNull(DEFAULT_MULTIPHASE.peekFirst().getItemName()))
                .component(ModComponents.ETERNAL, Eternal.INSTANCE)
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                .component(ModComponents.PROVIDENCE, Providence.INSTANCE)
                .component(ModComponents.MERCILESS, Merciless.DEFAULT)
        );
    }

    @Override
    protected double getBaseAttackDamage() {
        return 17;
    }

    @Override
    public ThrownHeavyHalberdEntity createThrown(Level level, LivingEntity shooter, ItemStack pickupItemStack) {
        return new ThrownTranscendenceHeavyHalberdEntity(level, shooter, pickupItemStack);
    }

    @Override
    public ThrownHeavyHalberdEntity createThrown(Level level, double x, double y, double z, ItemStack pickupItemStack) {
        return new ThrownTranscendenceHeavyHalberdEntity(level, x, y, z, pickupItemStack);
    }

    @Override
    public ItemStack assemble(int id, MultipleToOneSmithingRecipeInput input, HolderLookup.Provider registries) {
        if (id == 0) {
            ItemStack firstStack = input.getInputItem(0);
            ItemStack secondStack = input.getInputItem(1);
            Multiphase.PhaseData first = Multiphase.PhaseData.of(
                firstStack.get(DataComponents.CUSTOM_NAME), null,
                firstStack.getOrDefault(DataComponents.REPAIR_COST, 0),
                firstStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY),
                firstStack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
            Multiphase.PhaseData second = Multiphase.PhaseData.of(
                secondStack.get(DataComponents.CUSTOM_NAME), null,
                secondStack.getOrDefault(DataComponents.REPAIR_COST, 0),
                secondStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY),
                secondStack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));

            Multiphase multiphase = Multiphase.make(this, first, second);
            ItemStack result = this.getDefaultInstance();
            result.set(ModComponents.MULTIPHASE, multiphase);
            multiphase.applyToStack(result);

            return result;
        }
        return ItemStack.EMPTY;
    }
}
