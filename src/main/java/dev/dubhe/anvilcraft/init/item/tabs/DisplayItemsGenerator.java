package dev.dubhe.anvilcraft.init.item.tabs;

import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerRegistry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.item.armor.WeatherproofChestplateItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public abstract class DisplayItemsGenerator implements CreativeModeTab.DisplayItemsGenerator {
    protected CreativeModeTab.@Nullable ItemDisplayParameters itemDisplayParameters;
    protected CreativeModeTab.@Nullable Output output;

    @Override
    public void accept(CreativeModeTab.ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output output) {
        this.itemDisplayParameters = itemDisplayParameters;
        this.output = output;
        this.accept();
    }

    public abstract void accept();

    public void acceptFolded(CreativeModeTab.Output output, ItemLike item) {
        ItemStack stack = item.asItem().getDefaultInstance();
        List<ItemStack> variants = CreativeVariantPickerRegistry.createVariants(stack).orElse(List.of());
        if (variants.isEmpty() || ItemStack.isSameItemSameComponents(variants.getFirst(), stack)) {
            output.accept(item);
        } else {
            output.accept(item, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        }
    }

    public void plain(ItemLike item) {
        if (this.output == null) {
            return;
        }
        this.output.accept(item);
    }

    public void ionoCraftBackpack(ItemLike item) {
        if (this.output == null) {
            return;
        }
        ItemStack stack = item.asItem().getDefaultInstance();
        this.output.accept(stack);
        if (item.asItem() instanceof WeatherproofChestplateItem chestplate) this.output.accept(chestplate.creativeStack());
    }

    public void enchanting(ItemLike item, ResourceKey<Enchantment> enchKey, int level) {
        if (this.output == null || this.itemDisplayParameters == null) {
            return;
        }
        this.output.accept(DisplayItemsGenerator.enchanting(this.itemDisplayParameters, item, enchKey, level));
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

    protected static Identifier texSection(String id) {
        return SharedTextures.textureGui("creative_inventory/section/" + id);
    }
}
