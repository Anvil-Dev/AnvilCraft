package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.jetbrains.annotations.NotNull;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

@SuppressWarnings("unused")
public class ModItemGroups {
    public static final RegistryEntry<CreativeModeTab> ANVILCRAFT_ITEM = REGISTRATE
        .defaultCreativeTab("item", builder -> builder
            .icon(ModItems.MAGNET_INGOT::asStack)
            .displayItems((ctx, entries) -> {
                entries.accept(ModItems.GUIDE_BOOK.get().getDefaultInstance());
                entries.accept(ModItems.MAGNET.get().getDefaultInstance());
                entries.accept(ModItems.GEODE.get().getDefaultInstance());
                entries.accept(ModItems.AMETHYST_PICKAXE.get().getDefaultInstance());
                entries.accept(ModItems.AMETHYST_AXE.get().getDefaultInstance());
                entries.accept(ModItems.AMETHYST_SHOVEL.get().getDefaultInstance());
                entries.accept(ModItems.AMETHYST_HOE.get().getDefaultInstance());
                entries.accept(ModItems.AMETHYST_SWORD.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_PICKAXE.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_AXE.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_SHOVEL.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_HOE.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_SWORD.get().getDefaultInstance());
                entries.accept(ModItems.ANVIL_HAMMER.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_ANVIL_HAMMER.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE.get().getDefaultInstance());
                entries.accept(ModItems.CREAM.get().getDefaultInstance());
                entries.accept(ModItems.FLOUR.get().getDefaultInstance());
                entries.accept(ModItems.DOUGH.get().getDefaultInstance());
                entries.accept(ModItems.CHOCOLATE.get().getDefaultInstance());
                entries.accept(ModItems.CHOCOLATE_BLACK.get().getDefaultInstance());
                entries.accept(ModItems.CHOCOLATE_WHITE.get().getDefaultInstance());
                entries.accept(ModItems.CREAMY_BREAD_ROLL.get().getDefaultInstance());
                entries.accept(ModItems.BEEF_MUSHROOM_STEW_RAW.get().getDefaultInstance());
                entries.accept(ModItems.BEEF_MUSHROOM_STEW.get().getDefaultInstance());
                entries.accept(ModItems.UTUSAN_RAW.get().getDefaultInstance());
                entries.accept(ModItems.UTUSAN.get().getDefaultInstance());
                entries.accept(ModItems.MAGNET_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.CURSED_GOLD_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.CURSED_GOLD_NUGGET.get().getDefaultInstance());
                entries.accept(Items.COPPER_INGOT.getDefaultInstance());
                entries.accept(ModItems.COPPER_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.ZINC_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.ZINC_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.TIN_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.TIN_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.TITANIUM_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.TITANIUM_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.TUNGSTEN_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.TUNGSTEN_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.LEAD_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.LEAD_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.SILVER_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.SILVER_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.URANIUM_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.URANIUM_NUGGET.get().getDefaultInstance());
                entries.accept(ModItems.BRONZE_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.BRASS_INGOT.get().getDefaultInstance());
                entries.accept(ModItems.NETHERITE_CRYSTAL_NUCLEUS.get().getDefaultInstance());
                entries.accept(ModItems.LIME_POWDER.get().getDefaultInstance());
                entries.accept(ModItems.TOPAZ.get().getDefaultInstance());
                entries.accept(ModItems.RUBY.get().getDefaultInstance());
                entries.accept(ModItems.SAPPHIRE.get().getDefaultInstance());
                entries.accept(ModItems.RESIN.get().getDefaultInstance());
                entries.accept(ModItems.AMBER.get().getDefaultInstance());
                entries.accept(ModItems.HARDEND_RESIN.get().getDefaultInstance());
                entries.accept(ModItems.WOOD_FIBER.get().getDefaultInstance());
                entries.accept(ModItems.CIRCUIT_BOARD.get().getDefaultInstance());
                entries.accept(ModItems.SPONGE_GEMMULE.get().getDefaultInstance());
                entries.accept(ModItems.SEA_HEART_SHELL_SHARD.get().getDefaultInstance());
                entries.accept(ModItems.SEA_HEART_SHELL.get().getDefaultInstance());
                entries.accept(ModItems.PRISMARINE_CLUSTER.get().getDefaultInstance());
                entries.accept(ModItems.PRISMARINE_BLADE.get().getDefaultInstance());
                entries.accept(ModItems.COCOA_LIQUOR.get().getDefaultInstance());
                entries.accept(ModItems.COCOA_BUTTER.get().getDefaultInstance());
                entries.accept(ModItems.COCOA_POWDER.get().getDefaultInstance());
                entries.accept(ModItems.CAPACITOR.get().getDefaultInstance());
                entries.accept(ModItems.CAPACITOR_EMPTY.get().getDefaultInstance());
                entries.accept(ModItems.MAGNETOELECTRIC_CORE.get().getDefaultInstance());
                entries.accept(ModItems.CRAB_CLAW.asStack());
                entries.accept(ModItemGroups.createMaxLevelBook(ModEnchantments.FELLING));
                entries.accept(ModItemGroups.createMaxLevelBook(ModEnchantments.HARVEST));
                entries.accept(ModItemGroups.createMaxLevelBook(ModEnchantments.BEHEADING));
            })
            .build()
        )
        .register();

    public static final RegistryEntry<CreativeModeTab> ANVILCRAFT_BLOCK = REGISTRATE
        .defaultCreativeTab("block", builder -> builder
            .icon(ModBlocks.MAGNET_BLOCK::asStack)
            .displayItems((ctx, entries) -> {
                entries.accept(Items.IRON_TRAPDOOR.getDefaultInstance());
                entries.accept(Items.CAULDRON.getDefaultInstance());
                entries.accept(Items.CAMPFIRE.getDefaultInstance());
                entries.accept(Items.STONECUTTER.getDefaultInstance());
                entries.accept(Items.SCAFFOLDING.getDefaultInstance());
                entries.accept(ModBlocks.STAMPING_PLATFORM.asStack());
                entries.accept(ModBlocks.CORRUPTED_BEACON.asStack());
                entries.accept(Items.ANVIL.getDefaultInstance());
                entries.accept(Items.CHIPPED_ANVIL.getDefaultInstance());
                entries.accept(Items.DAMAGED_ANVIL.getDefaultInstance());
                entries.accept(ModBlocks.ROYAL_ANVIL.asStack());
                entries.accept(ModBlocks.ROYAL_GRINDSTONE.asStack());
                entries.accept(ModBlocks.ROYAL_SMITHING_TABLE.asStack());
                entries.accept(ModBlocks.CREATIVE_GENERATOR.asStack());
                entries.accept(ModBlocks.HEATER.asStack());
                entries.accept(ModBlocks.TRANSMISSION_POLE.asStack());
                entries.accept(ModBlocks.REMOTE_TRANSMISSION_POLE.asStack());
                entries.accept(ModBlocks.CHARGE_COLLECTOR.asStack());
                entries.accept(ModBlocks.LOAD_MONITOR.asStack());
                entries.accept(ModBlocks.POWER_CONVERTER_SMALL.asStack());
                entries.accept(ModBlocks.POWER_CONVERTER_MIDDLE.asStack());
                entries.accept(ModBlocks.POWER_CONVERTER_BIG.asStack());
                entries.accept(ModBlocks.PIEZOELECTRIC_CRYSTAL.asStack());
                entries.accept(ModBlocks.BLOCK_PLACER.asStack());
                entries.accept(ModBlocks.BLOCK_DEVOURER.asStack());
                entries.accept(ModBlocks.OVERSEER_BLOCK.asStack());
                entries.accept(ModBlocks.JEWEL_CRAFTING_TABLE.asStack());
                entries.accept(ModBlocks.MAGNET_BLOCK.asStack());
                entries.accept(ModBlocks.HOLLOW_MAGNET_BLOCK.asStack());
                entries.accept(ModBlocks.FERRITE_CORE_MAGNET_BLOCK.asStack());
                entries.accept(ModBlocks.AUTO_CRAFTER.asStack());
                entries.accept(ModBlocks.CHUTE.asStack());
                entries.accept(ModBlocks.ROYAL_STEEL_BLOCK.asStack());
                entries.accept(ModBlocks.SMOOTH_ROYAL_STEEL_BLOCK.asStack());
                entries.accept(ModBlocks.CUT_ROYAL_STEEL_BLOCK.asStack());
                entries.accept(ModBlocks.CUT_ROYAL_STEEL_SLAB.asStack());
                entries.accept(ModBlocks.CUT_ROYAL_STEEL_STAIRS.asStack());
                entries.accept(ModBlocks.CURSED_GOLD_BLOCK.asStack());
                entries.accept(ModBlocks.ZINC_BLOCK.asStack());
                entries.accept(ModBlocks.TIN_BLOCK.asStack());
                entries.accept(ModBlocks.TITANIUM_BLOCK.asStack());
                entries.accept(ModBlocks.TUNGSTEN_BLOCK.asStack());
                entries.accept(ModBlocks.LEAD_BLOCK.asStack());
                entries.accept(ModBlocks.SILVER_BLOCK.asStack());
                entries.accept(ModBlocks.URANIUM_BLOCK.asStack());
                entries.accept(ModBlocks.BRONZE_BLOCK.asStack());
                entries.accept(ModBlocks.BRASS_BLOCK.asStack());
                entries.accept(ModBlocks.TOPAZ_BLOCK.asStack());
                entries.accept(ModBlocks.RUBY_BLOCK.asStack());
                entries.accept(ModBlocks.SAPPHIRE_BLOCK.asStack());
                entries.accept(ModBlocks.RESIN_BLOCK.asStack());
                entries.accept(ModBlocks.AMBER_BLOCK.asStack());
                entries.accept(ModBlocks.CRAB_TRAP.asStack());
                entries.accept(ModBlocks.NETHER_DUST.asStack());
                entries.accept(ModBlocks.END_DUST.asStack());
                entries.accept(ModBlocks.CINERITE.asStack());
                entries.accept(ModBlocks.QUARTZ_SAND.asStack());
                entries.accept(ModBlocks.DEEPSLATE_CHIPS.asStack());
                entries.accept(ModBlocks.BLACK_SAND.asStack());
                entries.accept(ModBlocks.TEMPERING_GLASS.asStack());
                entries.accept(ModBlocks.ITEM_COLLECTOR.asStack());
            })
            .build()
        )
        .register();

    public static void register() {
    }

    @SuppressWarnings("SameParameterValue")
    private static @NotNull ItemStack createMaxLevelBook(@NotNull RegistryEntry<? extends Enchantment> enchantment) {
        return EnchantedBookItem.createForEnchantment(
            new EnchantmentInstance(enchantment.get(), enchantment.get().getMaxLevel())
        );
    }
}
