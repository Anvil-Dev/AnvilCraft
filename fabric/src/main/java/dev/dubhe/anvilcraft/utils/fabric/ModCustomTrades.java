package dev.dubhe.anvilcraft.utils.fabric;

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
                new ItemStack(Items.EMERALD, 4), new ItemStack(Items.SPYGLASS, 1),
                2, 4, 0.05f
            ));
        });
        TradeOfferHelper.registerVillagerOffers(ModVillagers.JEWELER, 2, factories -> {
            factories.add((entity, random) -> new MerchantOffer(
                new ItemStack(ModItems.ROYAL_STEEL_INGOT, 1), new ItemStack(Items.EMERALD, 6),
                16, 20, 0.05f
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
                new ItemStack(ModBlocks.AMBER_BLOCK), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 8),
                2, 2, 10, 0.05f
            ));
            factories.add((entity, random) -> new MerchantOffer(
                new ItemStack(ModBlocks.AMBER_BLOCK), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 24),
                2, 2, 30, 0.05f
            ));
        });
        TradeOfferHelper.registerVillagerOffers(ModVillagers.JEWELER, 5, factories -> {
            factories.add((entity, random) -> new MerchantOffer(
                new ItemStack(Items.EMERALD, 64), new ItemStack(Blocks.SMOOTH_BASALT, 4),
                new ItemStack(ModItems.GEODE),
                1, 30, 0.05f
            ));
            factories.add((entity, random) -> new MerchantOffer(
                new ItemStack(Items.EMERALD, 64), new ItemStack(Items.TOTEM_OF_UNDYING),
                new ItemStack(Items.TOTEM_OF_UNDYING),
                1, 30, 0.05f
            ));
        });
    }

}
