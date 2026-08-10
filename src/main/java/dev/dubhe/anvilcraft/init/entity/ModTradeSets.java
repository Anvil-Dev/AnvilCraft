package dev.dubhe.anvilcraft.init.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Optional;

public class ModTradeSets {

    public static final ResourceKey<TradeSet> JEWELER_LEVEL_1 = ModTradeSets.key("jeweler/level_1");
    public static final ResourceKey<TradeSet> JEWELER_LEVEL_2 = ModTradeSets.key("jeweler/level_2");
    public static final ResourceKey<TradeSet> JEWELER_LEVEL_3 = ModTradeSets.key("jeweler/level_3");
    public static final ResourceKey<TradeSet> JEWELER_LEVEL_4 = ModTradeSets.key("jeweler/level_4");
    public static final ResourceKey<TradeSet> JEWELER_LEVEL_5 = ModTradeSets.key("jeweler/level_5");

    public static ResourceKey<TradeSet> key(String name) {
        return ResourceKey.create(Registries.TRADE_SET, AnvilCraft.of(name));
    }

    public static void bootstrap(BootstrapContext<TradeSet> context) {
        // Level 1: 2 trades
        ModTradeSets.register(context, ModTradeSets.JEWELER_LEVEL_1, HolderSet.direct(
            context.lookup(Registries.VILLAGER_TRADE).getOrThrow(ModVillagerTrades.AMETHYST_SHARD_FOR_EMERALD),
            context.lookup(Registries.VILLAGER_TRADE).getOrThrow(ModVillagerTrades.EMERALD_FOR_TINTED_GLASS)
        ));

        // Level 2: 2 trades
        ModTradeSets.register(context, ModTradeSets.JEWELER_LEVEL_2, HolderSet.direct(
            context.lookup(Registries.VILLAGER_TRADE).getOrThrow(ModVillagerTrades.SEA_LANTERN_FOR_EMERALD),
            context.lookup(Registries.VILLAGER_TRADE).getOrThrow(ModVillagerTrades.AMBER_FOR_EMERALD)
        ));

        // Level 3: 2 random gem trades; the template trade is added separately
        ModTradeSets.register(context, ModTradeSets.JEWELER_LEVEL_3, HolderSet.direct(
            context.lookup(Registries.VILLAGER_TRADE).getOrThrow(ModVillagerTrades.TOPAZ_BLOCK_FOR_EMERALD),
            context.lookup(Registries.VILLAGER_TRADE).getOrThrow(ModVillagerTrades.SAPPHIRE_BLOCK_FOR_EMERALD),
            context.lookup(Registries.VILLAGER_TRADE).getOrThrow(ModVillagerTrades.RUBY_BLOCK_FOR_EMERALD)
        ));

        // Level 4: 3 trades
        ModTradeSets.register(context, ModTradeSets.JEWELER_LEVEL_4, HolderSet.direct(
            context.lookup(Registries.VILLAGER_TRADE).getOrThrow(ModVillagerTrades.NAUTILUS_SHELL_FOR_EMERALD),
            context.lookup(Registries.VILLAGER_TRADE).getOrThrow(ModVillagerTrades.MOB_AMBER_FOR_EMERALD),
            context.lookup(Registries.VILLAGER_TRADE).getOrThrow(ModVillagerTrades.RESENTFUL_AMBER_FOR_EMERALD)
        ));

        // Level 5: 2 trades
        ModTradeSets.register(context, ModTradeSets.JEWELER_LEVEL_5, HolderSet.direct(
            context.lookup(Registries.VILLAGER_TRADE).getOrThrow(ModVillagerTrades.EMERALD_FOR_GEODE),
            context.lookup(Registries.VILLAGER_TRADE).getOrThrow(ModVillagerTrades.EMERALD_FOR_AMULET_BOX)
        ));
    }

    private static void register(
        BootstrapContext<TradeSet> context,
        ResourceKey<TradeSet> key,
        HolderSet<VillagerTrade> trades
    ) {
        context.register(key, new TradeSet(
            trades,
            ConstantValue.exactly(2.0F),
            false,
            Optional.of(key.identifier().withPrefix("trade_set/"))
        ));
    }
}
