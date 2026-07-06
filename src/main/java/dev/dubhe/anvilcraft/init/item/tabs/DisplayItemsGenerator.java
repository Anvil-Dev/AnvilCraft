package dev.dubhe.anvilcraft.init.item.tabs;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ItemLike;

import java.util.NoSuchElementException;
import java.util.Optional;
import javax.annotation.Nullable;

public abstract class DisplayItemsGenerator implements CreativeModeTab.DisplayItemsGenerator {
    protected @Nullable CreativeModeTab.ItemDisplayParameters itemDisplayParameters;
    protected @Nullable CreativeModeTab.Output output;

    @Override
    public void accept(CreativeModeTab.ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output output) {
        this.itemDisplayParameters = itemDisplayParameters;
        this.output = output;
        this.accept();
    }

    public abstract void accept();

    public void plain(ItemLike item) {
        if (this.output == null) {
            return;
        }
        this.output.accept(item);
    }

    public void energy(ItemLike item) {
        if (this.output == null) {
            return;
        }
        ItemStack stack = item.asItem().getDefaultInstance();
        stack.set(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY);
        this.output.accept(stack);
    }

    public void ionoCraftBackpack(ItemLike item) {
        if (this.output == null) {
            return;
        }
        ItemStack stack = item.asItem().getDefaultInstance();
        ItemStack full = stack.copy();
        full.set(ModComponents.STORED_ENERGY, new StoredEnergy(IonoCraftBackpackItem.MAX_ENERGY));
        this.output.accept(stack);
        this.output.accept(full);
    }

    public void enchanting(ItemLike item, ResourceKey<Enchantment> enchKey, int level) {
        if (this.output == null || this.itemDisplayParameters == null) {
            return;
        }
        this.output.accept(enchanting(this.itemDisplayParameters, item, enchKey, level));
    }

    private static ItemStack enchanting(
        CreativeModeTab.ItemDisplayParameters parameters,
        ItemLike item,
        ResourceKey<Enchantment> enchKey,
        int level
    ) {
        ItemStack stack = item.asItem().getDefaultInstance();
        Optional<Holder.Reference<Enchantment>> holder = parameters.holders().holder(enchKey);
        if (holder.isPresent()) {
            stack.enchant(holder.get(), level);
        } else {
            NoSuchElementException exception = new NoSuchElementException(enchKey.identifier().toString());
            AnvilCraft.LOGGER.error(exception.getLocalizedMessage(), exception);
        }
        return stack;
    }
}
