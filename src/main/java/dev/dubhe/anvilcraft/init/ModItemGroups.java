package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

@SuppressWarnings("unused")
public class ModItemGroups {
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> ANVILCRAFT_TOOL = REGISTRATE
        .defaultCreativeTab("tools", builder -> builder
                .icon(ModItems.MAGNET::asStack)
                .displayItems((ctx, entries) -> {})
                .title(REGISTRATE.addLang("itemGroup", AnvilCraft.of("tools"), "AnvilCraft: Utilities"))
                .build()
        )
        .register();


    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> ANVILCRAFT_INGREDIENTS = REGISTRATE
        .defaultCreativeTab("ingredients", builder -> builder
            .icon(ModItems.MAGNET_INGOT::asStack)
            .displayItems((ctx, entries) -> {})
            .title(REGISTRATE.addLang("itemGroup", AnvilCraft.of("ingredients"), "AnvilCraft: Ingredients"))
            .build()
        )
        .register();

    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> ANVILCRAFT_FUNCTION_BLOCK = REGISTRATE
        .defaultCreativeTab("functional_block", builder -> builder
            .icon(ModBlocks.MAGNET_BLOCK::asStack)
            .displayItems((ctx, entries) -> {
                entries.accept(Items.IRON_TRAPDOOR.getDefaultInstance());
                entries.accept(Items.CAULDRON.getDefaultInstance());
                entries.accept(Items.CAMPFIRE.getDefaultInstance());
                entries.accept(Items.STONECUTTER.getDefaultInstance());
                entries.accept(Items.SCAFFOLDING.getDefaultInstance());
                entries.accept(Items.ANVIL.getDefaultInstance());
                entries.accept(Items.CHIPPED_ANVIL.getDefaultInstance());
                entries.accept(Items.DAMAGED_ANVIL.getDefaultInstance());
            })
            .title(REGISTRATE.addLang("itemGroup",
                AnvilCraft.of("functional_block"),
                "AnvilCraft: Functional Block"))
            .build()
        )
        .register();

    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> ANVILCRAFT_BUILD_BLOCK = REGISTRATE
        .defaultCreativeTab("building_block", builder -> builder
            .icon(ModBlocks.REINFORCED_CONCRETE_BLACK::asStack)
            .displayItems((ctx, entries) -> {})
            .title(REGISTRATE.addLang("itemGroup",
                AnvilCraft.of("building_block"),
                "AnvilCraft: Building Block"))
            .build()
        )
        .register();


    public static void register() {

    }
}
