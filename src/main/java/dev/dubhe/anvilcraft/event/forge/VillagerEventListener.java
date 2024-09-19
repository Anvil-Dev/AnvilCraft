package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModVillagers;

import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class VillagerEventListener {
    /**
     * 添加自定义交易时间
     *
     * @param event 事件
     */
    @SubscribeEvent
    public static void addCustomTrades(@NotNull VillagerTradesEvent event) {
        if (event.getType() == ModVillagers.JEWELER.get()) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            // level 1
            trades.get(1)
                    .add((entity, random) -> new MerchantOffer(
                            new ItemCost(Items.AMETHYST_SHARD, 4), new ItemStack(Items.EMERALD, 1), 16, 2, 0.05f));
            trades.get(1)
                    .add((entity, random) -> new MerchantOffer(
                            new ItemCost(Items.EMERALD, 1), new ItemStack(Items.TINTED_GLASS, 1), 12, 4, 0.05f));

            // level 2
            trades.get(2)
                    .add((entity, random) -> new MerchantOffer(
                            new ItemCost(Items.SEA_LANTERN, 8), new ItemStack(Items.EMERALD, 1), 12, 10, 0.05f));
            trades.get(2)
                    .add((entity, random) -> new MerchantOffer(
                            new ItemCost(ModItems.AMBER, 4), new ItemStack(Items.EMERALD, 1), 16, 5, 0.05f));

            // level 3
            trades.get(3).add((entity, random) -> {
                ItemCost cost =
                        switch ((int) (random.nextDouble() * 3)) {
                            case 0 -> new ItemCost(ModBlocks.TOPAZ_BLOCK);
                            case 1 -> new ItemCost(ModBlocks.SAPPHIRE_BLOCK);
                            default -> new ItemCost(ModBlocks.RUBY_BLOCK);
                        };
                return new MerchantOffer(cost, new ItemStack(Items.EMERALD, 8), 8, 10, 0.05f);
            });
            trades.get(3)
                    .add((entity, random) -> new MerchantOffer(
                            new ItemCost(Items.EMERALD, 40),
                            Optional.of(new ItemCost(ModItems.ROYAL_STEEL_INGOT, 4)),
                            new ItemStack(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE.asItem(), 1),
                            2,
                            10,
                            0.05f));

            // level 4
            trades.get(4)
                    .add((entity, random) -> new MerchantOffer(
                            new ItemCost(Items.NAUTILUS_SHELL),
                            new ItemStack(Items.EMERALD, 2),
                            12,
                            10,
                            0.05f));
            trades.get(4)
                    .add((entity, random) -> ((int) (random.nextDouble() * 2)) == 1
                            ? new MerchantOffer(
                                    new ItemCost(ModBlocks.MOB_AMBER_BLOCK),
                                    new ItemStack(Items.EMERALD, 8),
                                    2,
                                    10,
                                    0.05f)
                            : new MerchantOffer(
                                    new ItemCost(ModBlocks.RESENTFUL_AMBER_BLOCK),
                                    new ItemStack(Items.EMERALD, 24),
                                    2,
                                    30,
                                    0.05f));

            // level 5
            trades.get(5)
                    .add((entity, random) -> new MerchantOffer(
                            new ItemCost(Items.EMERALD, 64),
                            Optional.of(new ItemCost(Blocks.SMOOTH_BASALT, 32)),
                            new ItemStack(ModItems.GEODE.asItem()),
                            4,
                            30,
                            0.05f));
            trades.get(5)
                    .add((entity, random) -> new MerchantOffer(
                            new ItemCost(Items.EMERALD, 64),
                            Optional.of(new ItemCost(Items.TOTEM_OF_UNDYING)),
                            new ItemStack(ModItems.AMULET_BOX.asItem()),
                            1,
                            30,
                            0.05f));
        }
    }
}
