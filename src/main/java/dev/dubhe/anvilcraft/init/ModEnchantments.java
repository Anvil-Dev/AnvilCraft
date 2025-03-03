package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.enchantment.FellingEffect;
import dev.dubhe.anvilcraft.enchantment.HarvestEffect;
import net.minecraft.advancements.critereon.ItemPredicate;
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
import net.minecraft.world.level.storage.loot.predicates.MatchTool;

public class ModEnchantments {

    public static final ResourceKey<Enchantment> FELLING_KEY = key("felling");
    public static final ResourceKey<Enchantment> HARVEST_KEY = key("harvest");
    public static final ResourceKey<Enchantment> BEHEADING_KEY = key("beheading");

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
            ).withEffect(
                ModEnchantmentEffectComponents.POST_BREAK_BLOCK,
                new FellingEffect(5),
                MatchTool.toolMatches(
                    ItemPredicate.Builder.item()
                        .of(ItemTags.AXES)
                )
            )
        );
        register(
            context,
            HARVEST_KEY,
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
            ).withEffect(
                ModEnchantmentEffectComponents.USE_ON_BLOCK,
                new HarvestEffect(5),
                MatchTool.toolMatches(
                    ItemPredicate.Builder.item()
                        .of(ItemTags.HOES)
                )
            )
        );
        register(
            context,
            BEHEADING_KEY,
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

    public static void register(
        BootstrapContext<Enchantment> context, ResourceKey<Enchantment> key, Enchantment.Builder builder) {
        context.register(key, builder.build(key.location()));
    }
}
