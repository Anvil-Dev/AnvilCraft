package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

@SuppressWarnings("unused")
public class ModItemGroups {
    public static final RegistryEntry<CreativeModeTab> ANVILCRAFT_ITEM = REGISTRATE
            .defaultCreativeTab("item", builder -> builder
                    .icon(ModItems.MAGNET_INGOT::asStack)
                    .displayItems((ctx, entries) -> {
                        entries.accept(ModItems.MAGNET.asStack());
                        entries.accept(ModItems.AMETHYST_PICKAXE.asStack());
                        entries.accept(ModItems.ROYAL_STEEL_PICKAXE.asStack());
                        entries.accept(ModItems.CREAM.asStack());
                        entries.accept(ModItems.FLOUR.asStack());
                        entries.accept(ModItems.DOUGH.asStack());
                        entries.accept(ModItems.CHOCOLATE.asStack());
                        entries.accept(ModItems.CHOCOLATE_BLACK.asStack());
                        entries.accept(ModItems.CHOCOLATE_WHITE.asStack());
                        entries.accept(ModItems.CREAMY_BREAD_ROLL.asStack());
                        entries.accept(ModItems.BEEF_MUSHROOM_STEW_RAW.asStack());
                        entries.accept(ModItems.BEEF_MUSHROOM_STEW.asStack());
                        entries.accept(ModItems.UTUSAN_RAW.asStack());
                        entries.accept(ModItems.UTUSAN.asStack());
                        entries.accept(ModItems.MAGNET_INGOT.asStack());
                        entries.accept(ModItems.ROYAL_STEEL_INGOT.asStack());
                        entries.accept(ModItems.ROYAL_STEEL_NUGGET.asStack());
                        entries.accept(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE.asStack());
                        entries.accept(ModItems.CURSED_GOLD_INGOT.asStack());
                        entries.accept(ModItems.CURSED_GOLD_NUGGET.asStack());
                        entries.accept(ModItems.SPONGE_GEMMULE.asStack());
                        entries.accept(ModItems.COCOA_LIQUOR.asStack());
                        entries.accept(ModItems.COCOA_BUTTER.asStack());
                        entries.accept(ModItems.COCOA_POWDER.asStack());
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
                        entries.accept(ModBlocks.STAMPING_PLATFORM.asStack());
                        entries.accept(Items.ANVIL.getDefaultInstance());
                        entries.accept(Items.CHIPPED_ANVIL.getDefaultInstance());
                        entries.accept(Items.DAMAGED_ANVIL.getDefaultInstance());
                        entries.accept(ModBlocks.ROYAL_ANVIL.asStack());
                        entries.accept(ModBlocks.ROYAL_GRINDSTONE.asStack());
                        entries.accept(ModBlocks.ROYAL_SMITHING_TABLE.asStack());
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
                    })
                    .build()
            )
            .register();
    public static void register() {
    }
}
