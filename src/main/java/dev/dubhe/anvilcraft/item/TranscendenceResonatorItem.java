package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.property.Eternal;
import dev.dubhe.anvilcraft.api.item.property.Merciless;
import dev.dubhe.anvilcraft.api.item.property.Multiphase;
import dev.dubhe.anvilcraft.api.item.property.Providence;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.recipe.multiple.MultipleToOneSmithingRecipeInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Objects;

public class TranscendenceResonatorItem extends ResonatorItem {
    public static final Multiphase DEFAULT_MULTIPHASE = Multiphase.make(
        Component.translatable("item.anvilcraft.transcendence_resonator")
    );

    public TranscendenceResonatorItem(Properties properties) {
        super(
            ModTiers.TRANSCENDIUM,
            properties
                .attributes(ResonatorItem.createAttributes(ModTiers.TRANSCENDIUM, 17, -3f))
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
    public ItemStack assemble(int id, MultipleToOneSmithingRecipeInput input, HolderLookup.Provider registries) {
        if (id == 0) {
            ItemStack firstStack = input.getInputItem(0);
            ItemStack secondStack = input.getInputItem(1);
            Multiphase.PhaseData first = Multiphase.PhaseData.of(
                firstStack.get(DataComponents.CUSTOM_NAME), firstStack.get(DataComponents.ITEM_NAME),
                firstStack.getOrDefault(DataComponents.REPAIR_COST, 0),
                firstStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY),
                firstStack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
            Multiphase.PhaseData second = Multiphase.PhaseData.of(
                secondStack.get(DataComponents.CUSTOM_NAME), secondStack.get(DataComponents.ITEM_NAME),
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
