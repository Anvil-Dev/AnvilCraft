package dev.dubhe.anvilcraft.util.fabric;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.fabric.ModVillagers;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;

public class ModCustomTrades {
    /**
     * 注册自定义交易
     */
    public static void registerCustomTrades() {
        TradeOfferHelper.registerVillagerOffers(ModVillagers.JEWELER, 1, factories -> {
            factories.add((entity, random) -> new MerchantOffer(
                new ItemStack(Items.AMETHYST_SHARD, 4), new ItemStack(Items.EMERALD, 1),
                16, 2, 0.05f
            ));
            factories.add((entity, random) -> new MerchantOffer(
                new ItemStack(Items.EMERALD, 1), new ItemStack(Items.TINTED_GLASS, 1),
                12, 4, 0.05f
            ));
        });
        TradeOfferHelper.registerVillagerOffers(ModVillagers.JEWELER, 2, factories -> {
            factories.add((entity, random) -> new MerchantOffer(
                new ItemStack(Items.SEA_LANTERN, 8), new ItemStack(Items.EMERALD, 1),
                12, 10, 0.05f
            ));
            factories.add((entity, random) -> new MerchantOffer(
                new ItemStack(ModItems.AMBER, 4), new ItemStack(Items.EMERALD, 1),
                16, 5, 0.05f
            ));
        });
        TradeOfferHelper.registerVillagerOffers(ModVillagers.JEWELER, 3, factories -> {
            factories.add((entity, random) -> {
                ItemStack stack = switch ((int) (random.nextDouble() * 3)) {
                    case 0 -> new ItemStack(ModBlocks.TOPAZ_BLOCK);
                    case 1 -> new ItemStack(ModBlocks.SAPPHIRE_BLOCK);
                    default -> new ItemStack(ModBlocks.RUBY_BLOCK);
                };
                return new MerchantOffer(
                    stack, new ItemStack(Items.EMERALD, 8),
                    8, 10, 0.05f
                );
            });
            factories.add((entity, random) -> new MerchantOffer(
                new ItemStack(Items.EMERALD, 40), new ItemStack(ModItems.ROYAL_STEEL_INGOT, 4),
                new ItemStack(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE, 1),
                1, 10, 0.05f
            ));
        });
        TradeOfferHelper.registerVillagerOffers(ModVillagers.JEWELER, 4, factories -> {
            factories.add((entity, random) -> new MerchantOffer(
                new ItemStack(Items.NAUTILUS_SHELL), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 2),
                12, 10, 0.05f
            ));
            factories.add((entity, random) -> new MerchantOffer(
                new ItemStack(ModBlocks.CORRUPTED_BEACON), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 24),
                2, 2, 30, 0.05f
            ));
        });
        TradeOfferHelper.registerVillagerOffers(ModVillagers.JEWELER, 5, factories -> {
            factories.add((entity, random) -> new MerchantOffer(
                new ItemStack(Items.EMERALD, 64), new ItemStack(Blocks.SMOOTH_BASALT, 32),
                new ItemStack(ModItems.GEODE),
                1, 30, 0.05f
            ));
            factories.add((entity, random) -> new MerchantOffer(
                new ItemStack(Items.EMERALD, 64), new ItemStack(Items.TOTEM_OF_UNDYING),
                new ItemStack(ModItems.AMULET_BOX),
                1, 30, 0.05f
            ));
        });
    }

}
