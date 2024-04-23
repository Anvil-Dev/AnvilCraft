package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.forge.ModVillagers;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class VillagerEvent {
    /**
     * 添加自定义交易时间
     *
     * @param event 事件
     */
    @SubscribeEvent
    public static void addCustomTrades(VillagerTradesEvent event) {
        if (event.getType() == ModVillagers.JEWELER.get()) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            // level 1
            trades.get(1).add((entity, random) -> new MerchantOffer(
                new ItemStack(Items.AMETHYST_SHARD, 4), new ItemStack(Items.EMERALD, 1),
                16, 2, 0.05f
            ));
            trades.get(1).add((entity, random) -> new MerchantOffer(
                new ItemStack(Items.EMERALD, 4), new ItemStack(Items.SPYGLASS, 1),
                2, 4, 0.05f
            ));

            // level 2
            trades.get(2).add((entity, random) -> new MerchantOffer(
                new ItemStack(ModItems.ROYAL_STEEL_INGOT, 1), new ItemStack(Items.EMERALD, 6),
                16, 20, 0.05f
            ));
            trades.get(2).add((entity, random) -> new MerchantOffer(
                new ItemStack(ModItems.AMBER, 4), new ItemStack(Items.EMERALD, 1),
                16, 5, 0.05f
            ));

            // level 3
            trades.get(3).add((entity, random) -> {
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
            trades.get(3).add((entity, random) -> new MerchantOffer(
                new ItemStack(Items.EMERALD, 40), new ItemStack(ModItems.ROYAL_STEEL_INGOT, 4),
                new ItemStack(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE, 1),
                1, 10, 0.05f
            ));

            // level 4
            trades.get(4).add((entity, random) -> new MerchantOffer(
                new ItemStack(ModBlocks.AMBER_BLOCK), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 8),
                2, 2, 10, 0.05f
            ));
            trades.get(4).add((entity, random) -> new MerchantOffer(
                new ItemStack(ModBlocks.AMBER_BLOCK), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 24),
                2, 2, 30, 0.05f
            ));

            // level 5
            trades.get(5).add((entity, random) -> new MerchantOffer(
                new ItemStack(Items.EMERALD, 64), new ItemStack(Blocks.SMOOTH_BASALT, 4),
                new ItemStack(ModItems.GEODE),
                1, 30, 0.05f
            ));
            trades.get(5).add((entity, random) -> new MerchantOffer(
                new ItemStack(Items.EMERALD, 64), new ItemStack(Items.TOTEM_OF_UNDYING),
                new ItemStack(Items.TOTEM_OF_UNDYING),
                1, 30, 0.05f
            ));
        }
    }
}
