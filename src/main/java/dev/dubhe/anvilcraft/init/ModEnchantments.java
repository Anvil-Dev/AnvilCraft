package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.Lazy;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("unchecked")
public class ModEnchantments {

    public static final ResourceKey<Enchantment> FELLING_KEY = key("felling");
    public static final ResourceKey<Enchantment> HARVEST_KEY = key("harvest");
    public static final ResourceKey<Enchantment> BEHEADING_KEY = key("beheading");

    public static final Lazy<Holder<Enchantment>> FELLING = new Lazy<>(() -> {
        Registry<Enchantment> enchantmentRegistry =
                (Registry<Enchantment>) BuiltInRegistries.REGISTRY.get(Registries.ENCHANTMENT.location());
        return enchantmentRegistry.getHolderOrThrow(FELLING_KEY).getDelegate();
    });

    public static Lazy<Holder<Enchantment>> HARVEST = new Lazy<>(() -> {
        Registry<Enchantment> enchantmentRegistry =
                (Registry<Enchantment>) BuiltInRegistries.REGISTRY.get(Registries.ENCHANTMENT.location());
        return enchantmentRegistry.getHolderOrThrow(HARVEST_KEY).getDelegate();
    });

    public static final Lazy<Holder<Enchantment>> BEHEADING = new Lazy<>(() -> {
        Registry<Enchantment> enchantmentRegistry =
                (Registry<Enchantment>) BuiltInRegistries.REGISTRY.get(Registries.ENCHANTMENT.location());
        return enchantmentRegistry.getHolderOrThrow(BEHEADING_KEY).getDelegate();
    });

    public static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, AnvilCraft.of(name));
    }

    /**
     *
     */
    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<DamageType> damageTypeHolderGetter = context.lookup(Registries.DAMAGE_TYPE);
        HolderGetter<Enchantment> enchantmentHolderGetter = context.lookup(Registries.ENCHANTMENT);
        HolderGetter<Item> itemHolderGetter = context.lookup(Registries.ITEM);
        HolderGetter<Block> blockHolderGetter = context.lookup(Registries.BLOCK);
        register(
                context,
                FELLING_KEY,
                Enchantment.enchantment(Enchantment.definition(
                        itemHolderGetter.getOrThrow(ItemTags.AXES),
                        5,
                        3,
                        Enchantment.dynamicCost(1, 10),
                        Enchantment.dynamicCost(15, 10),
                        2,
                        EquipmentSlotGroup.MAINHAND)));
        register(
                context,
                HARVEST_KEY,
                Enchantment.enchantment(Enchantment.definition(
                        itemHolderGetter.getOrThrow(ItemTags.HOES),
                        5,
                        3,
                        Enchantment.dynamicCost(1, 10),
                        Enchantment.dynamicCost(15, 10),
                        2,
                        EquipmentSlotGroup.MAINHAND)));
        register(
                context,
                BEHEADING_KEY,
                Enchantment.enchantment(Enchantment.definition(
                        itemHolderGetter.getOrThrow(ItemTags.SWORDS),
                        5,
                        3,
                        Enchantment.dynamicCost(1, 10),
                        Enchantment.dynamicCost(15, 10),
                        2,
                        EquipmentSlotGroup.MAINHAND)));
    }

    public static void register(
            BootstrapContext<Enchantment> context, ResourceKey<Enchantment> key, Enchantment.Builder builder) {
        context.register(key, builder.build(key.location()));
    }
}
