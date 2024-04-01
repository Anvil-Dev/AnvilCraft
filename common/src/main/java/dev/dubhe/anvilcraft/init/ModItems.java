package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.ItemEntry;
import dev.dubhe.anvilcraft.item.CuredItem;
import dev.dubhe.anvilcraft.item.MagnetItem;
import dev.dubhe.anvilcraft.item.ModFoods;
import dev.dubhe.anvilcraft.item.ModTiers;
import dev.dubhe.anvilcraft.item.RoyalSteelUpgradeSmithingTemplateItem;
import dev.dubhe.anvilcraft.item.UtusanItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

public class ModItems {
    public static final ItemEntry<? extends Item> MAGNET = REGISTRATE
            .item("magnet", properties -> new MagnetItem(properties.durability(255)))
            .register();
    public static final ItemEntry<? extends Item> AMETHYST_PICKAXE = REGISTRATE
            .item("amethyst_pickaxe", properties -> new PickaxeItem(ModTiers.AMETHYST, 1, -2.8f, properties) {
                @Override
                public @NotNull ItemStack getDefaultInstance() {
                    ItemStack stack = super.getDefaultInstance();
                    stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
                    return stack;
                }

                @Override
                public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
                    super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
                    tooltipComponents.add(Component.translatable("item.anvilcraft.amethyst_pickaxe.tooltip").withStyle(ChatFormatting.GRAY));
                }
            })
            .register();
    public static final ItemEntry<? extends Item> AMETHYST_AXE = REGISTRATE
            .item("amethyst_axe", properties -> new AxeItem(ModTiers.AMETHYST, 7, -3.2f, properties) {
                @Override
                public @NotNull ItemStack getDefaultInstance() {
                    ItemStack stack = super.getDefaultInstance();
                    stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
                    stack.enchant(Enchantments.MOB_LOOTING, 3);
                    return stack;
                }
            })
            .register();
    public static final ItemEntry<? extends Item> AMETHYST_HOE = REGISTRATE
            .item("amethyst_hoe", properties -> new HoeItem(ModTiers.AMETHYST, -1, -2.0f, properties) {
                @Override
                public @NotNull ItemStack getDefaultInstance() {
                    ItemStack stack = super.getDefaultInstance();
                    stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
                    return stack;
                }
            })
            .register();
    public static final ItemEntry<? extends Item> AMETHYST_SWORD = REGISTRATE
            .item("amethyst_sword", properties -> new SwordItem(ModTiers.AMETHYST, 3, -2.4f, properties) {
                @Override
                public @NotNull ItemStack getDefaultInstance() {
                    ItemStack stack = super.getDefaultInstance();
                    stack.enchant(Enchantments.MOB_LOOTING, 3);
                    return stack;
                }
            })
            .register();
    public static final ItemEntry<? extends Item> AMETHYST_SHOVEL = REGISTRATE
            .item("amethyst_shovel", properties -> new ShovelItem(ModTiers.AMETHYST, 1.5f, -3.0f, properties) {
                @Override
                public @NotNull ItemStack getDefaultInstance() {
                    ItemStack stack = super.getDefaultInstance();
                    stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
                    return stack;
                }
            })
            .register();
    public static final ItemEntry<? extends Item> COCOA_LIQUOR = REGISTRATE
            .item("cocoa_liquor", Item::new)
            .register();
    public static final ItemEntry<? extends Item> COCOA_BUTTER = REGISTRATE
            .item("cocoa_butter", Item::new)
            .register();
    public static final ItemEntry<? extends Item> COCOA_POWDER = REGISTRATE
            .item("cocoa_powder", Item::new)
            .register();
    public static final ItemEntry<? extends Item> CREAM = REGISTRATE
            .item("cream", Item::new)
            .register();
    public static final ItemEntry<? extends Item> FLOUR = REGISTRATE
            .item("flour", Item::new)
            .register();
    public static final ItemEntry<? extends Item> DOUGH = REGISTRATE
            .item("dough", Item::new)
            .register();
    public static final ItemEntry<? extends Item> CHOCOLATE = REGISTRATE
            .item("chocolate", properties -> new Item(properties.food(ModFoods.CHOCOLATE)))
            .register();
    public static final ItemEntry<? extends Item> CHOCOLATE_BLACK = REGISTRATE
            .item("chocolate_black", p -> new Item(p.food(ModFoods.CHOCOLATE_BLACK)))
            .register();
    public static final ItemEntry<? extends Item> CHOCOLATE_WHITE = REGISTRATE
            .item("chocolate_white", p -> new Item(p.food(ModFoods.CHOCOLATE_WHITE)))
            .register();
    public static final ItemEntry<? extends Item> CREAMY_BREAD_ROLL = REGISTRATE
            .item("creamy_bread_roll", p -> new Item(p.food(ModFoods.CREAMY_BREAD_ROLL)))
            .register();
    public static final ItemEntry<? extends Item> BEEF_MUSHROOM_STEW_RAW = REGISTRATE
            .item("beef_mushroom_stew_raw", Item::new).register();
    public static final ItemEntry<? extends Item> BEEF_MUSHROOM_STEW = REGISTRATE
            .item("beef_mushroom_stew", p -> new Item(p.food(ModFoods.BEEF_MUSHROOM_STEW)))
            .register();
    public static final ItemEntry<? extends Item> UTUSAN_RAW = REGISTRATE
            .item("utusan_raw", Item::new).register();
    public static final ItemEntry<? extends Item> UTUSAN = REGISTRATE
            .item("utusan", UtusanItem::new).register();
    public static final ItemEntry<? extends Item> MAGNET_INGOT = REGISTRATE
            .item("magnet_ingot", Item::new).register();
    public static final ItemEntry<? extends Item> DEBRIS_SCRAP = REGISTRATE
            .item("debris_scrap", Item::new).register();
    public static final ItemEntry<? extends Item> NETHER_STAR_SHARD = REGISTRATE
            .item("nether_star_shard", Item::new).register();
    public static final ItemEntry<? extends Item> NETHERITE_CORE = REGISTRATE
            .item("netherite_core", Item::new).register();
    public static final ItemEntry<? extends Item> NETHERITE_COIL = REGISTRATE
            .item("netherite_coil", Item::new).register();
    public static final ItemEntry<? extends Item> ELYTRA_FRAME = REGISTRATE
            .item("elytra_frame", Item::new).register();
    public static final ItemEntry<? extends Item> ELYTRA_MEMBRANE = REGISTRATE
            .item("elytra_membrane", Item::new).register();
    public static final ItemEntry<? extends Item> SEED_OF_THE_SEA = REGISTRATE
            .item("seed_of_the_sea", Item::new).register();
    public static final ItemEntry<? extends Item> FRUIT_OF_THE_SEA = REGISTRATE
            .item("fruit_of_the_sea", Item::new).register();
    public static final ItemEntry<? extends Item> KERNEL_OF_THE_SEA = REGISTRATE
            .item("kernel_of_the_sea", Item::new).register();
    public static final ItemEntry<? extends Item> TEAR_OF_THE_SEA = REGISTRATE
            .item("tear_of_the_sea", Item::new).register();
    public static final ItemEntry<? extends Item> BLADE_OF_THE_SEA = REGISTRATE
            .item("blade_of_the_sea", Item::new).register();
    public static final ItemEntry<? extends Item> BARK = REGISTRATE
            .item("bark", Item::new).register();
    public static final ItemEntry<? extends Item> PULP = REGISTRATE
            .item("pulp", Item::new).register();
    public static final ItemEntry<? extends Item> SPONGE_GEMMULE = REGISTRATE
            .item("sponge_gemmule", Item::new).register();
    // 皇家钢系
    public static final ItemEntry<? extends Item> ROYAL_STEEL_INGOT = REGISTRATE
            .item("royal_steel_ingot", Item::new).register();
    public static final ItemEntry<? extends Item> ROYAL_STEEL_NUGGET = REGISTRATE
            .item("royal_steel_nugget", Item::new).register();
    public static final ItemEntry<? extends Item> ROYAL_STEEL_PICKAXE = REGISTRATE
            .item("royal_steel_pickaxe", properties -> new PickaxeItem(Tiers.DIAMOND, 1, -2.8f, properties.durability(2559)) {
                @Override
                public @NotNull ItemStack getDefaultInstance() {
                    return super.getDefaultInstance();
                }
            })
            .register();
    public static final ItemEntry<? extends Item> ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE = REGISTRATE
            .item("royal_steel_upgrade_smithing_template", RoyalSteelUpgradeSmithingTemplateItem::new).register();

    // 诅咒黄金系
    public static final ItemEntry<? extends Item> CURSED_GOLD_INGOT = REGISTRATE
            .item("cursed_gold_ingot", CuredItem::new).register();
    public static final ItemEntry<? extends Item> CURSED_GOLD_NUGGET = REGISTRATE
            .item("cursed_gold_nugget", CuredItem::new).register();

    public static void register() {
    }
}
