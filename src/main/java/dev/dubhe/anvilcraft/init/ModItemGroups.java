package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class ModItemGroups {
    private static final Map<String, CreativeModeTab.Builder> ITEM_GROUP_MAP = new HashMap<>();
    public static final CreativeModeTab.Builder ANVILCRAFT_ITEM = createItemGroup("item", FabricItemGroup::builder)
            .icon(() -> new ItemStack(ModItems.MAGNET_INGOT))
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
                entries.accept(ModItems.MAGNET_INGOT.getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_INGOT.getDefaultInstance());
                entries.accept(ModItems.DEBRIS_SCRAP.getDefaultInstance());
                entries.accept(ModItems.NETHER_STAR_SHARD.getDefaultInstance());
                entries.accept(ModItems.NETHERITE_CORE.getDefaultInstance());
                entries.accept(ModItems.NETHERITE_COIL.getDefaultInstance());
                entries.accept(ModItems.ELYTRA_FRAME.getDefaultInstance());
                entries.accept(ModItems.ELYTRA_MEMBRANE.getDefaultInstance());
                entries.accept(ModItems.SEED_OF_THE_SEA.getDefaultInstance());
                entries.accept(ModItems.FRUIT_OF_THE_SEA.getDefaultInstance());
                entries.accept(ModItems.KERNEL_OF_THE_SEA.getDefaultInstance());
                entries.accept(ModItems.TEAR_OF_THE_SEA.getDefaultInstance());
                entries.accept(ModItems.BLADE_OF_THE_SEA.getDefaultInstance());
                entries.accept(ModItems.BARK.getDefaultInstance());
                entries.accept(ModItems.PULP.getDefaultInstance());
                entries.accept(ModItems.SPONGE_GEMMULE.getDefaultInstance());
                entries.accept(ModItems.COCOA_LIQUOR.getDefaultInstance());
                entries.accept(ModItems.COCOA_BUTTER.getDefaultInstance());
                entries.accept(ModItems.COCOA_POWDER.getDefaultInstance());
            });
    public static final CreativeModeTab.Builder ANVILCRAFT_BLOCK = createItemGroup("block", FabricItemGroup::builder)
            .icon(() -> new ItemStack(ModItems.MAGNET_BLOCK))
            .displayItems((ctx, entries) -> {
                entries.accept(Items.ANVIL.getDefaultInstance());
                entries.accept(Items.CHIPPED_ANVIL.getDefaultInstance());
                entries.accept(Items.DAMAGED_ANVIL.getDefaultInstance());
                entries.accept(ModItems.ROYAL_ANVIL.getDefaultInstance());
                entries.accept(ModItems.MAGNET_BLOCK.getDefaultInstance());
                entries.accept(ModItems.HOLLOW_MAGNET_BLOCK.getDefaultInstance());
                entries.accept(ModItems.FERRITE_CORE_MAGNET_BLOCK.getDefaultInstance());
                entries.accept(ModItems.INTERACT_MACHINE.getDefaultInstance());
                entries.accept(ModItems.AUTO_CRAFTER.getDefaultInstance());
                entries.accept(ModItems.ROYAL_STEEL_BLOCK.getDefaultInstance());
                entries.accept(ModItems.SMOOTH_ROYAL_STEEL_BLOCK.getDefaultInstance());
                entries.accept(ModItems.CUT_ROYAL_STEEL_BLOCK.getDefaultInstance());
                entries.accept(ModItems.CUT_ROYAL_STEEL_SLAB.getDefaultInstance());
                entries.accept(ModItems.CUT_ROYAL_STEEL_STAIRS.getDefaultInstance());
            });

    private static CreativeModeTab.@NotNull Builder createItemGroup(String id, @NotNull Supplier<CreativeModeTab.Builder> itemGroupBuilder) {
        CreativeModeTab.Builder builder = itemGroupBuilder.get();
        builder.title(Component.translatable("itemGroup.anvilcraft." + id));
        ITEM_GROUP_MAP.put(id, builder);
        return builder;
    }

    public static void register() {
        for (Map.Entry<String, CreativeModeTab.Builder> entry : ModItemGroups.ITEM_GROUP_MAP.entrySet()) {
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, AnvilCraft.of(entry.getKey()), entry.getValue().build());
        }
    }
}
