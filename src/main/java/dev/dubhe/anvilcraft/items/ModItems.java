package dev.dubhe.anvilcraft.items;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class ModItems {
    public static final Map<String, Item> ITEM_MAP = new HashMap<>();
    public static final Map<String, CreativeModeTab.Builder> ITEM_GROUP_MAP = new HashMap<>();
    public static final CreativeModeTab.Builder ANVILCRAFT_MATERIAL = createItemGroup("material", FabricItemGroup::builder)
            .icon(() -> new ItemStack(ModItems.MAGNET_INGOT))
            .displayItems((ctx, entries) -> {
                entries.accept(ModItems.MAGNET_INGOT.getDefaultInstance());
                entries.accept(ModItems.DEBRIS_SCRAP.getDefaultInstance());
                entries.accept(ModItems.NETHER_STAR_SHARD.getDefaultInstance());
                entries.accept(ModItems.NETHERITE_CORE.getDefaultInstance());
                entries.accept(ModItems.NETHERITE_COIL.getDefaultInstance());
                entries.accept(ModItems.ELYTRA_FRAME.getDefaultInstance());
                entries.accept(ModItems.ELYTRA_MEMBRANE.getDefaultInstance());
                entries.accept(ModItems.SEED_OF_THE_SEA.getDefaultInstance());
                entries.accept(ModItems.TEAR_OF_THE_SEA.getDefaultInstance());
                entries.accept(ModItems.FRUIT_OF_THE_SEA.getDefaultInstance());
                entries.accept(ModItems.KERNEL_OF_THE_SEA.getDefaultInstance());
                entries.accept(ModItems.BLADE_OF_THE_SEA.getDefaultInstance());
                entries.accept(ModItems.BARK.getDefaultInstance());
                entries.accept(ModItems.PULP.getDefaultInstance());
                entries.accept(ModItems.SPONGE_GEMMULE.getDefaultInstance());
            });
    public static final Item MAGNET_INGOT = registerItem("magnet_ingot", Item::new, defaultProperties());
    public static final Item DEBRIS_SCRAP = registerItem("debris_scrap", Item::new, defaultProperties());
    public static final Item NETHER_STAR_SHARD = registerItem("nether_star_shard", Item::new, defaultProperties());
    public static final Item NETHERITE_CORE = registerItem("netherite_core", Item::new, defaultProperties());
    public static final Item NETHERITE_COIL = registerItem("netherite_coil", Item::new, defaultProperties());
    public static final Item ELYTRA_FRAME = registerItem("elytra_frame", Item::new, defaultProperties());
    public static final Item ELYTRA_MEMBRANE = registerItem("elytra_membrane", Item::new, defaultProperties());
    public static final Item SEED_OF_THE_SEA = registerItem("seed_of_the_sea", Item::new, defaultProperties());
    public static final Item TEAR_OF_THE_SEA = registerItem("tear_of_the_sea", Item::new, defaultProperties());
    public static final Item FRUIT_OF_THE_SEA = registerItem("fruit_of_the_sea", Item::new, defaultProperties());
    public static final Item KERNEL_OF_THE_SEA = registerItem("kernel_of_the_sea", Item::new, defaultProperties());
    public static final Item BLADE_OF_THE_SEA = registerItem("blade_of_the_sea", Item::new, defaultProperties());
    public static final Item BARK = registerItem("bark", Item::new, defaultProperties());
    public static final Item PULP = registerItem("pulp", Item::new, defaultProperties());
    public static final Item SPONGE_GEMMULE = registerItem("sponge_gemmule", Item::new, defaultProperties());
    public static final CreativeModeTab.Builder ANVILCRAFT_FOOD = createItemGroup("food", FabricItemGroup::builder)
            .icon(() -> new ItemStack(ModItems.CHOCOLATE))
            .displayItems((ctx, entries) -> {
                entries.accept(ModItems.COCOA_BUTTER.getDefaultInstance());
                entries.accept(ModItems.COCOA_LIQUOR.getDefaultInstance());
                entries.accept(ModItems.COCOA_POWDER.getDefaultInstance());
                entries.accept(ModItems.GREASE.getDefaultInstance());
                entries.accept(ModItems.CREAM.getDefaultInstance());
                entries.accept(ModItems.FLOUR.getDefaultInstance());
                entries.accept(ModItems.DOUGH.getDefaultInstance());
                entries.accept(ModItems.FLATDOUGH.getDefaultInstance());
                entries.accept(ModItems.MEAT_STUFFING.getDefaultInstance());
                entries.accept(ModItems.CHOCOLATE.getDefaultInstance());
                entries.accept(ModItems.CHOCOLATE_BLACK.getDefaultInstance());
                entries.accept(ModItems.CHOCOLATE_WHITE.getDefaultInstance());
                entries.accept(ModItems.CREAMY_BREAD_ROLL.getDefaultInstance());
                entries.accept(ModItems.MEATBALLS_RAW.getDefaultInstance());
                entries.accept(ModItems.MEATBALLS.getDefaultInstance());
                entries.accept(ModItems.DUMPLING_RAW.getDefaultInstance());
                entries.accept(ModItems.DUMPLING.getDefaultInstance());
                entries.accept(ModItems.SHENGJIAN_RAW.getDefaultInstance());
                entries.accept(ModItems.SHENGJIAN.getDefaultInstance());
                entries.accept(ModItems.SWEET_DUMPLING_RAW.getDefaultInstance());
                entries.accept(ModItems.SWEET_DUMPLING.getDefaultInstance());
                entries.accept(ModItems.BEEF_MUSHROOM_STEW_RAW.getDefaultInstance());
                entries.accept(ModItems.BEEF_MUSHROOM_STEW.getDefaultInstance());
                entries.accept(ModItems.UTUSAN_RAW.getDefaultInstance());
                entries.accept(ModItems.UTUSAN.getDefaultInstance());
            });
    public static final Item COCOA_BUTTER = registerItem("cocoa_butter", Item::new, defaultProperties());
    public static final Item COCOA_LIQUOR = registerItem("cocoa_liquor", Item::new, defaultProperties());
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
    public static final CreativeModeTab.Builder ANVILCRAFT_TOOL = createItemGroup("tool", FabricItemGroup::builder)
            .icon(() -> new ItemStack(ModItems.MAGNET))
            .displayItems((ctx, entries) -> {
                entries.accept(ModItems.MAGNET.getDefaultInstance());
                entries.accept(ModItems.CHANGEABLE_PICKAXE_FORTUNE.getDefaultInstance());
                entries.accept(ModItems.CHANGEABLE_PICKAXE_SILK_TOUCH.getDefaultInstance());
                entries.accept(ModItems.AMETHYST_PICKAXE.getDefaultInstance());
                entries.accept(ModItems.AMETHYST_AXE.getDefaultInstance());
                entries.accept(ModItems.AMETHYST_HOE.getDefaultInstance());
                entries.accept(ModItems.AMETHYST_SWORD.getDefaultInstance());
                entries.accept(ModItems.AMETHYST_SHOVEL.getDefaultInstance());
                entries.accept(ModItems.PROTOCOL_EMPTY.getDefaultInstance());
                entries.accept(ModItems.PROTOCOL_PROTECT.getDefaultInstance());
                entries.accept(ModItems.PROTOCOL_REPAIR.getDefaultInstance());
                entries.accept(ModItems.PROTOCOL_RESTOCK.getDefaultInstance());
                entries.accept(ModItems.PROTOCOL_ABSORB.getDefaultInstance());
            });
    public static final Item MAGNET = registerItem("magnet", Item::new, defaultProperties());
    public static final Item CHANGEABLE_PICKAXE_FORTUNE = registerItem("changeable_pickaxe_fortune", ChangeablePickaxeItem::new, defaultProperties());
    public static final Item CHANGEABLE_PICKAXE_SILK_TOUCH = registerItem("changeable_pickaxe_silk_touch", ChangeablePickaxeItem::new, defaultProperties());
    public static final Item AMETHYST_PICKAXE = registerItem("amethyst_pickaxe", properties -> new PickaxeItem(ModTiers.AMETHYST, 1, -2.8f, properties) {
        @Override
        public @NotNull ItemStack getDefaultInstance() {
            ItemStack stack = super.getDefaultInstance();
            stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
            return stack;
        }
    }, defaultProperties());
    public static final Item AMETHYST_AXE = registerItem("amethyst_axe", properties -> new AxeItem(ModTiers.AMETHYST, 1, -2.8f, properties){
        @Override
        public @NotNull ItemStack getDefaultInstance() {
            ItemStack stack = super.getDefaultInstance();
            stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
            stack.enchant(Enchantments.MOB_LOOTING, 3);
            return stack;
        }
    }, defaultProperties());
    public static final Item AMETHYST_HOE = registerItem("amethyst_hoe", properties -> new HoeItem(ModTiers.AMETHYST, 1, -2.8f, properties){
        @Override
        public @NotNull ItemStack getDefaultInstance() {
            ItemStack stack = super.getDefaultInstance();
            stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
            return stack;
        }
    }, defaultProperties());
    public static final Item AMETHYST_SWORD = registerItem("amethyst_sword", properties -> new SwordItem(ModTiers.AMETHYST, 1, -2.8f, properties){
        @Override
        public @NotNull ItemStack getDefaultInstance() {
            ItemStack stack = super.getDefaultInstance();
            stack.enchant(Enchantments.MOB_LOOTING, 3);
            return stack;
        }
    }, defaultProperties());
    public static final Item AMETHYST_SHOVEL = registerItem("amethyst_shovel", properties -> new ShovelItem(ModTiers.AMETHYST, 1, -2.8f, properties){
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
    public static final CreativeModeTab.Builder ANVILCRAFT_MACHINE = createItemGroup("machine", FabricItemGroup::builder)
            .icon(() -> new ItemStack(Items.ANVIL))
            .displayItems((ctx, entries) -> {
                entries.accept(Items.ANVIL.getDefaultInstance());
                entries.accept(Items.CHIPPED_ANVIL.getDefaultInstance());
                entries.accept(Items.DAMAGED_ANVIL.getDefaultInstance());
            });

    public static CreativeModeTab.Builder createItemGroup(String id, Supplier<CreativeModeTab.Builder> itemGroupBuilder) {
        CreativeModeTab.Builder builder = itemGroupBuilder.get();
        builder.title(Component.translatable("itemGroup.anvilcraft." + id));
        ITEM_GROUP_MAP.put(id, builder);
        return builder;
    }

    public static Item registerItem(String id, Function<Item.Properties, Item> itemCreator, Item.Properties properties) {
        Item item = itemCreator.apply(properties);
        ITEM_MAP.put(id, item);
        return item;
    }

    public static Item.Properties defaultProperties() {
        return new Item.Properties();
    }
}
