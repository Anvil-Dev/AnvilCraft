package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.api.item.IMultipleToOneSmithingRecipeResult;
import dev.dubhe.anvilcraft.api.item.property.Multiphase;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.recipe.multiple.MultipleToOneSmithingRecipeInput;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultiphaseMatterShovelItem extends ShovelItem implements IMultipleToOneSmithingRecipeResult {
    public static final Multiphase DEFAULT_MULTIPHASE = Multiphase.make(
        Component.translatable("item.anvilcraft.multiphase_matter_shovel")
    );

    public MultiphaseMatterShovelItem(Properties properties) {
        super(
            ModTiers.MULTIPHASE,
            properties.fireResistant()
                .attributes(ShovelItem.createAttributes(ModTiers.MULTIPHASE, 6.5f, -3f))
                .component(ModComponents.FIRE_REFORGING, Unit.INSTANCE)
                .component(ModComponents.MULTIPHASE, DEFAULT_MULTIPHASE.copy())
                .component(DataComponents.ITEM_NAME, Objects.requireNonNull(DEFAULT_MULTIPHASE.copy().alpha().itemName()))
        );
    }

    @Override
    public ItemStack assemble(int id, MultipleToOneSmithingRecipeInput input, HolderLookup.Provider registries) {
        if (id == 0) {
            Multiphase.PhaseData first = Multiphase.PhaseData.of(
                input.getInputItem(0).get(DataComponents.CUSTOM_NAME), input.getInputItem(0).get(DataComponents.ITEM_NAME),
                input.getInputItem(0).getOrDefault(DataComponents.REPAIR_COST, 0),
                input.getInputItem(0).get(EnchantmentHelper.getComponentType(input.getInputItem(0)))
            );
            Multiphase.PhaseData second = Multiphase.PhaseData.of(
                input.getInputItem(1).get(DataComponents.CUSTOM_NAME), input.getInputItem(1).get(DataComponents.ITEM_NAME),
                input.getInputItem(1).getOrDefault(DataComponents.REPAIR_COST, 0),
                input.getInputItem(1).get(EnchantmentHelper.getComponentType(input.getInputItem(1)))
            );

            Multiphase multiphase = Multiphase.make(this, first, second);
            ItemStack result = this.getDefaultInstance();
            result.set(ModComponents.MULTIPHASE, multiphase);
            multiphase.applyToStack(result);

            return result;
        } else {
            return ItemStack.EMPTY;
        }
    }
}
