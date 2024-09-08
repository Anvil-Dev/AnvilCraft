package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

public class ModEnchantments {

    public static final ResourceKey<Enchantment> FELLING = key("felling");
    public static final ResourceKey<Enchantment> HARVEST = key("harvest");
    public static final ResourceKey<Enchantment> BEHEADING = key("beheading");

//    public static final RegistryEntry<FellingEnchantment> FELLING = REGISTRATE
//            .enchantment("felling", EnchantmentCategory.DIGGER, FellingEnchantment::new)
//            .rarity(Enchantment.Rarity.RARE)
//            .register();
//    public static final RegistryEntry<HarvestEnchantment> HARVEST = REGISTRATE
//            .enchantment("harvest", EnchantmentCategory.DIGGER, HarvestEnchantment::new)
//            .rarity(Enchantment.Rarity.RARE)
//            .register();
//    public static final RegistryEntry<BeheadingEnchantment> BEHEADING = REGISTRATE
//            .enchantment("beheading", EnchantmentCategory.WEAPON, BeheadingEnchantment::new)
//            .rarity(Enchantment.Rarity.RARE)
//            .register();

    public static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, AnvilCraft.of(name));
    }

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<DamageType> damageTypeHolderGetter = context.lookup(Registries.DAMAGE_TYPE);
        HolderGetter<Enchantment> enchantmentHolderGetter = context.lookup(Registries.ENCHANTMENT);
        HolderGetter<Item> itemHolderGetter = context.lookup(Registries.ITEM);
        HolderGetter<Block> blockHolderGetter = context.lookup(Registries.BLOCK);

        register(
                context,
                FELLING,
                Enchantment.enchantment(
                        Enchantment.definition(
                                itemHolderGetter.getOrThrow(ItemTags.AXES),
                                5,
                                3,
                                Enchantment.dynamicCost(1, 10),
                                Enchantment.dynamicCost(15, 10),
                                2,
                                EquipmentSlotGroup.MAINHAND
                        )
                )
        );
        register(
                context,
                HARVEST,
                Enchantment.enchantment(
                        Enchantment.definition(
                                itemHolderGetter.getOrThrow(ItemTags.HOES),
                                5,
                                3,
                                Enchantment.dynamicCost(1, 10),
                                Enchantment.dynamicCost(15, 10),
                                2,
                                EquipmentSlotGroup.MAINHAND
                        )
                )
        );
        register(
                context,
                BEHEADING,
                Enchantment.enchantment(
                        Enchantment.definition(
                                itemHolderGetter.getOrThrow(ItemTags.SWORDS),
                                5,
                                3,
                                Enchantment.dynamicCost(1, 10),
                                Enchantment.dynamicCost(15, 10),
                                2,
                                EquipmentSlotGroup.MAINHAND
                        )
                )
        );

    }

    public static void register(BootstrapContext<Enchantment> context, ResourceKey<Enchantment> key, Enchantment.Builder builder) {
        context.register(key, builder.build(key.location()));
    }
}
