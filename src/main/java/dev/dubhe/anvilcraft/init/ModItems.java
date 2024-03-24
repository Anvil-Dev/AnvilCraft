package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("unused")
public class ModItems {
    private static final Map<String, Item> ITEM_MAP = new HashMap<>();
    public static final Item MAGNET = registerItem("magnet", MagnetItem::new, defaultProperties().durability(114));
    public static final Item CHANGEABLE_PICKAXE_FORTUNE = registerItem("changeable_pickaxe_fortune", ChangeablePickaxeItem::new, defaultProperties());
    public static final Item CHANGEABLE_PICKAXE_SILK_TOUCH = registerItem("changeable_pickaxe_silk_touch", ChangeablePickaxeItem::new, defaultProperties());
    public static final Item AMETHYST_PICKAXE = registerItem("amethyst_pickaxe", properties -> new PickaxeItem(ModTiers.AMETHYST, 1, -2.8f, properties) {
        @Override
        public @NotNull ItemStack getDefaultInstance() {
            ItemStack stack = super.getDefaultInstance();
            stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
            return stack;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
            super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

            tooltipComponents.add(Component.translatable("item.anvilcraft.amethyst_pickaxe.tooltip").withStyle(ChatFormatting.GRAY));
        }
    }, defaultProperties());
    public static final Item AMETHYST_AXE = registerItem("amethyst_axe", properties -> new AxeItem(ModTiers.AMETHYST, 7, -3.2f, properties) {
        @Override
        public @NotNull ItemStack getDefaultInstance() {
            ItemStack stack = super.getDefaultInstance();
            stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
            stack.enchant(Enchantments.MOB_LOOTING, 3);
            return stack;
        }
    }, defaultProperties());
    public static final Item AMETHYST_HOE = registerItem("amethyst_hoe", properties -> new HoeItem(ModTiers.AMETHYST, -1, -2.0f, properties) {
        @Override
        public @NotNull ItemStack getDefaultInstance() {
            ItemStack stack = super.getDefaultInstance();
            stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
            return stack;
        }
    }, defaultProperties());
    public static final Item AMETHYST_SWORD = registerItem("amethyst_sword", properties -> new SwordItem(ModTiers.AMETHYST, 3, -2.4f, properties) {
        @Override
        public @NotNull ItemStack getDefaultInstance() {
            ItemStack stack = super.getDefaultInstance();
            stack.enchant(Enchantments.MOB_LOOTING, 3);
            return stack;
        }
    }, defaultProperties());
    public static final Item AMETHYST_SHOVEL = registerItem("amethyst_shovel", properties -> new ShovelItem(ModTiers.AMETHYST, 1.5f, -3.0f, properties) {
        @Override
        public @NotNull ItemStack getDefaultInstance() {
            ItemStack stack = super.getDefaultInstance();
            stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
            return stack;
        }
    }, defaultProperties());
    public static final Item PROTOCOL_EMPTY = registerItem("protocol_empty", Item::new, defaultProperties());
    public static final Item PROTOCOL_PROTECT = registerItem("protocol_protect", Item::new, defaultProperties());
    public static final Item PROTOCOL_REPAIR = registerItem("protocol_repair", Item::new, defaultProperties());
    public static final Item PROTOCOL_RESTOCK = registerItem("protocol_restock", Item::new, defaultProperties());
    public static final Item PROTOCOL_ABSORB = registerItem("protocol_absorb", Item::new, defaultProperties());
    public static final Item COCOA_LIQUOR = registerItem("cocoa_liquor", Item::new, defaultProperties());
    public static final Item COCOA_BUTTER = registerItem("cocoa_butter", Item::new, defaultProperties());
    public static final Item COCOA_POWDER = registerItem("cocoa_powder", Item::new, defaultProperties());
    public static final Item GREASE = registerItem("grease", Item::new, defaultProperties());
    public static final Item CREAM = registerItem("cream", Item::new, defaultProperties());
    public static final Item FLOUR = registerItem("flour", Item::new, defaultProperties());
    public static final Item DOUGH = registerItem("dough", Item::new, defaultProperties());
    public static final Item FLATDOUGH = registerItem("flatdough", Item::new, defaultProperties());
    public static final Item MEAT_STUFFING = registerItem("meat_stuffing", Item::new, defaultProperties());
    public static final Item CHOCOLATE = registerItem("chocolate", Item::new, defaultProperties().food(ModFoods.CHOCOLATE));
    public static final Item CHOCOLATE_BLACK = registerItem("chocolate_black", Item::new, defaultProperties().food(ModFoods.CHOCOLATE_BLACK));
    public static final Item CHOCOLATE_WHITE = registerItem("chocolate_white", Item::new, defaultProperties().food(ModFoods.CHOCOLATE_WHITE));
    public static final Item CREAMY_BREAD_ROLL = registerItem("creamy_bread_roll", Item::new, defaultProperties().food(ModFoods.CREAMY_BREAD_ROLL));
    public static final Item MEATBALLS_RAW = registerItem("meatballs_raw", Item::new, defaultProperties());
    public static final Item MEATBALLS = registerItem("meatballs", Item::new, defaultProperties().food(ModFoods.MEATBALLS));
    public static final Item DUMPLING_RAW = registerItem("dumpling_raw", Item::new, defaultProperties());
    public static final Item DUMPLING = registerItem("dumpling", Item::new, defaultProperties().food(ModFoods.DUMPLING));
    public static final Item SHENGJIAN_RAW = registerItem("shengjian_raw", Item::new, defaultProperties());
    public static final Item SHENGJIAN = registerItem("shengjian", Item::new, defaultProperties().food(ModFoods.SHENGJIAN));
    public static final Item SWEET_DUMPLING_RAW = registerItem("sweet_dumpling_raw", Item::new, defaultProperties());
    public static final Item SWEET_DUMPLING = registerItem("sweet_dumpling", Item::new, defaultProperties().food(ModFoods.SWEET_DUMPLING));
    public static final Item BEEF_MUSHROOM_STEW_RAW = registerItem("beef_mushroom_stew_raw", Item::new, defaultProperties());
    public static final Item BEEF_MUSHROOM_STEW = registerItem("beef_mushroom_stew", Item::new, defaultProperties().food(ModFoods.BEEF_MUSHROOM_STEW));
    public static final Item UTUSAN_RAW = registerItem("utusan_raw", Item::new, defaultProperties());
    public static final Item UTUSAN = registerItem("utusan", UtusanItem::new, defaultProperties());
    public static final Item MAGNET_INGOT = registerItem("magnet_ingot", Item::new, defaultProperties());
    public static final Item ROYAL_STEEL_INGOT = registerItem("royal_steel_ingot", Item::new, defaultProperties());
    public static final Item DEBRIS_SCRAP = registerItem("debris_scrap", Item::new, defaultProperties());
    public static final Item NETHER_STAR_SHARD = registerItem("nether_star_shard", Item::new, defaultProperties());
    public static final Item NETHERITE_CORE = registerItem("netherite_core", Item::new, defaultProperties());
    public static final Item NETHERITE_COIL = registerItem("netherite_coil", Item::new, defaultProperties());
    public static final Item ELYTRA_FRAME = registerItem("elytra_frame", Item::new, defaultProperties());
    public static final Item ELYTRA_MEMBRANE = registerItem("elytra_membrane", Item::new, defaultProperties());
    public static final Item SEED_OF_THE_SEA = registerItem("seed_of_the_sea", Item::new, defaultProperties());
    public static final Item FRUIT_OF_THE_SEA = registerItem("fruit_of_the_sea", Item::new, defaultProperties());
    public static final Item KERNEL_OF_THE_SEA = registerItem("kernel_of_the_sea", Item::new, defaultProperties());
    public static final Item TEAR_OF_THE_SEA = registerItem("tear_of_the_sea", Item::new, defaultProperties());
    public static final Item BLADE_OF_THE_SEA = registerItem("blade_of_the_sea", Item::new, defaultProperties());
    public static final Item BARK = registerItem("bark", Item::new, defaultProperties());
    public static final Item PULP = registerItem("pulp", Item::new, defaultProperties());
    public static final Item SPONGE_GEMMULE = registerItem("sponge_gemmule", Item::new, defaultProperties());

    public static final Item ROYAL_ANVIL = registerBlock(ModBlocks.ROYAL_ANVIL, BlockItem::new, defaultProperties());
    public static final Item MAGNET_BLOCK = registerBlock(ModBlocks.MAGNET_BLOCK, BlockItem::new, defaultProperties());
    public static final Item HOLLOW_MAGNET_BLOCK = registerBlock(ModBlocks.HOLLOW_MAGNET_BLOCK, BlockItem::new, defaultProperties());
    public static final Item FERRITE_CORE_MAGNET_BLOCK = registerBlock(ModBlocks.FERRITE_CORE_MAGNET_BLOCK, BlockItem::new, defaultProperties());
    public static final Item INTERACT_MACHINE = registerBlock(ModBlocks.INTERACT_MACHINE, BlockItem::new, defaultProperties());
    public static final Item AUTO_CRAFTER = registerBlock(ModBlocks.AUTO_CRAFTER, BlockItem::new, defaultProperties());
    public static final Item ROYAL_STEEL_BLOCK = registerBlock(ModBlocks.ROYAL_STEEL_BLOCK, BlockItem::new, defaultProperties());
    public static final Item SMOOTH_ROYAL_STEEL_BLOCK = registerBlock(ModBlocks.SMOOTH_ROYAL_STEEL_BLOCK, BlockItem::new, defaultProperties());
    public static final Item CUT_ROYAL_STEEL_BLOCK = registerBlock(ModBlocks.CUT_ROYAL_STEEL_BLOCK, BlockItem::new, defaultProperties());
    public static final Item CUT_ROYAL_STEEL_SLAB = registerBlock(ModBlocks.CUT_ROYAL_STEEL_SLAB, BlockItem::new, defaultProperties());
    public static final Item CUT_ROYAL_STEEL_STAIRS = registerBlock(ModBlocks.CUT_ROYAL_STEEL_STAIRS, BlockItem::new, defaultProperties());

    private static Item registerItem(String id, @NotNull Function<Item.Properties, Item> itemCreator, Item.Properties properties) {
        Item item = itemCreator.apply(properties);
        ITEM_MAP.put(id, item);
        return item;
    }

    private static Item registerBlock(Block block, @NotNull BiFunction<Block, Item.Properties, Item> itemCreator, Item.Properties properties) {
        Item item = itemCreator.apply(block, properties);
        ITEM_MAP.put(BuiltInRegistries.BLOCK.getKey(block).getPath(), item);
        return item;
    }

    private static @NotNull Item.Properties defaultProperties() {
        return new Item.Properties();
    }

    public static void register() {
        for (Map.Entry<String, Item> entry : ModItems.ITEM_MAP.entrySet()) {
            Registry.register(BuiltInRegistries.ITEM, AnvilCraft.of(entry.getKey()), entry.getValue());
        }
    }
}