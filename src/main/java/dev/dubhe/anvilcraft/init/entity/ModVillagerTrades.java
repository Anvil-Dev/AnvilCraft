package dev.dubhe.anvilcraft.init.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.item.trading.VillagerTrade;

import java.util.List;
import java.util.Optional;

public class ModVillagerTrades {

    // Level 1
    public static final ResourceKey<VillagerTrade> AMETHYST_SHARD_FOR_EMERALD = ModVillagerTrades.key("jeweler/amethyst_shard_for_emerald");
    public static final ResourceKey<VillagerTrade> EMERALD_FOR_TINTED_GLASS = ModVillagerTrades.key("jeweler/emerald_for_tinted_glass");

    // Level 2
    public static final ResourceKey<VillagerTrade> SEA_LANTERN_FOR_EMERALD = ModVillagerTrades.key("jeweler/sea_lantern_for_emerald");
    public static final ResourceKey<VillagerTrade> AMBER_FOR_EMERALD = ModVillagerTrades.key("jeweler/amber_for_emerald");

    // Level 3
    public static final ResourceKey<VillagerTrade> TOPAZ_BLOCK_FOR_EMERALD = ModVillagerTrades.key("jeweler/topaz_block_for_emerald");
    public static final ResourceKey<VillagerTrade> SAPPHIRE_BLOCK_FOR_EMERALD = ModVillagerTrades.key("jeweler/sapphire_block_for_emerald");
    public static final ResourceKey<VillagerTrade> RUBY_BLOCK_FOR_EMERALD = ModVillagerTrades.key("jeweler/ruby_block_for_emerald");
    public static final ResourceKey<VillagerTrade> EMERALD_FOR_ROYAL_STEEL_TEMPLATE = ModVillagerTrades.key(
        "jeweler/emerald_for_royal_steel_template"
    );

    // Level 4
    public static final ResourceKey<VillagerTrade> NAUTILUS_SHELL_FOR_EMERALD = ModVillagerTrades.key("jeweler/nautilus_shell_for_emerald");
    public static final ResourceKey<VillagerTrade> MOB_AMBER_FOR_EMERALD = ModVillagerTrades.key("jeweler/mob_amber_for_emerald");
    public static final ResourceKey<VillagerTrade> RESENTFUL_AMBER_FOR_EMERALD = ModVillagerTrades.key(
        "jeweler/resentful_amber_for_emerald"
    );

    // Level 5
    public static final ResourceKey<VillagerTrade> EMERALD_FOR_GEODE = ModVillagerTrades.key("jeweler/emerald_for_geode");
    public static final ResourceKey<VillagerTrade> EMERALD_FOR_AMULET_BOX = ModVillagerTrades.key("jeweler/emerald_for_amulet_box");

    public static ResourceKey<VillagerTrade> key(String name) {
        return ResourceKey.create(Registries.VILLAGER_TRADE, AnvilCraft.of(name));
    }

    public static void bootstrap(BootstrapContext<VillagerTrade> context) {
        // Level 1
        ModVillagerTrades.register(context, ModVillagerTrades.AMETHYST_SHARD_FOR_EMERALD,
                                   new TradeCost(Items.AMETHYST_SHARD, 4),
                                   Optional.empty(),
                                   new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F);
        ModVillagerTrades.register(context, ModVillagerTrades.EMERALD_FOR_TINTED_GLASS,
                                   new TradeCost(Items.EMERALD, 1),
                                   Optional.empty(),
                                   new ItemStackTemplate(Items.TINTED_GLASS), 12, 4, 0.05F);

        // Level 2
        ModVillagerTrades.register(context, ModVillagerTrades.SEA_LANTERN_FOR_EMERALD,
                                   new TradeCost(Items.SEA_LANTERN, 8),
                                   Optional.empty(),
                                   new ItemStackTemplate(Items.EMERALD), 12, 10, 0.05F);
        ModVillagerTrades.register(context, ModVillagerTrades.AMBER_FOR_EMERALD,
                                   new TradeCost(ModItems.AMBER, 4),
                                   Optional.empty(),
                                   new ItemStackTemplate(Items.EMERALD), 16, 5, 0.05F);

        // Level 3
        ModVillagerTrades.register(context, ModVillagerTrades.TOPAZ_BLOCK_FOR_EMERALD,
                                   new TradeCost(ModBlocks.TOPAZ_BLOCK.get().asItem(), 1),
                                   Optional.empty(),
                                   new ItemStackTemplate(Items.EMERALD, 8), 8, 10, 0.05F);
        ModVillagerTrades.register(context, ModVillagerTrades.SAPPHIRE_BLOCK_FOR_EMERALD,
                                   new TradeCost(ModBlocks.SAPPHIRE_BLOCK.get().asItem(), 1),
                                   Optional.empty(),
                                   new ItemStackTemplate(Items.EMERALD, 8), 8, 10, 0.05F);
        ModVillagerTrades.register(context, ModVillagerTrades.RUBY_BLOCK_FOR_EMERALD,
                                   new TradeCost(ModBlocks.RUBY_BLOCK.get().asItem(), 1),
                                   Optional.empty(),
                                   new ItemStackTemplate(Items.EMERALD, 8), 8, 10, 0.05F);
        ModVillagerTrades.register(context, ModVillagerTrades.EMERALD_FOR_ROYAL_STEEL_TEMPLATE,
                                   new TradeCost(Items.EMERALD, 40),
                                   Optional.of(new TradeCost(ModItems.ROYAL_STEEL_INGOT, 4)),
                                   new ItemStackTemplate(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE.get()), 2, 10, 0.05F);

        // Level 4
        ModVillagerTrades.register(context, ModVillagerTrades.NAUTILUS_SHELL_FOR_EMERALD,
                                   new TradeCost(Items.NAUTILUS_SHELL, 1),
                                   Optional.empty(),
                                   new ItemStackTemplate(Items.EMERALD, 2), 12, 10, 0.05F);
        ModVillagerTrades.register(context, ModVillagerTrades.MOB_AMBER_FOR_EMERALD,
                                   new TradeCost(ModBlocks.MOB_AMBER_BLOCK.get().asItem(), 1),
                                   Optional.empty(),
                                   new ItemStackTemplate(Items.EMERALD, 8), 2, 10, 0.05F);
        ModVillagerTrades.register(context, ModVillagerTrades.RESENTFUL_AMBER_FOR_EMERALD,
                                   new TradeCost(ModBlocks.RESENTFUL_AMBER_BLOCK.get().asItem(), 1),
                                   Optional.empty(),
                                   new ItemStackTemplate(Items.EMERALD, 24), 2, 30, 0.05F);

        // Level 5
        ModVillagerTrades.register(context, ModVillagerTrades.EMERALD_FOR_GEODE,
                                   new TradeCost(Items.EMERALD, 64),
                                   Optional.of(new TradeCost(Items.SMOOTH_BASALT.asItem(), 32)),
                                   new ItemStackTemplate(ModItems.GEODE.get()), 4, 30, 0.05F);
        ModVillagerTrades.register(context, ModVillagerTrades.EMERALD_FOR_AMULET_BOX,
                                   new TradeCost(Items.EMERALD, 64),
                                   Optional.of(new TradeCost(Items.TOTEM_OF_UNDYING, 1)),
                                   new ItemStackTemplate(ModItems.AMULET_BOX.get()), 1, 30, 0.05F);
    }

    private static void register(
        BootstrapContext<VillagerTrade> context,
        ResourceKey<VillagerTrade> key,
        TradeCost wants,
        Optional<TradeCost> additionalWants,
        ItemStackTemplate gives,
        int maxUses,
        int xp,
        float reputationDiscount
    ) {
        context.register(key, new VillagerTrade(
            wants,
            additionalWants,
            gives,
            maxUses,
            xp,
            reputationDiscount,
            Optional.empty(),
            List.of(),
            Optional.empty()
        ));
    }
}
