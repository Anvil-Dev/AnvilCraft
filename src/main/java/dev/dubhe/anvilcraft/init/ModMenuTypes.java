package dev.dubhe.anvilcraft.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.MenuEntry;
import dev.dubhe.anvilcraft.client.gui.screen.ActiveSilencerScreen;
import dev.dubhe.anvilcraft.client.gui.screen.AdvancedComparatorScreen;
import dev.dubhe.anvilcraft.client.gui.screen.BatchCrafterScreen;
import dev.dubhe.anvilcraft.client.gui.screen.BatchCutterScreen;
import dev.dubhe.anvilcraft.client.gui.screen.ChuteScreen;
import dev.dubhe.anvilcraft.client.gui.screen.EmberAnvilScreen;
import dev.dubhe.anvilcraft.client.gui.screen.EmberGrindstoneScreen;
import dev.dubhe.anvilcraft.client.gui.screen.EmberSmithingScreen;
import dev.dubhe.anvilcraft.client.gui.screen.EnergyWeaponMakeScreen;
import dev.dubhe.anvilcraft.client.gui.screen.ExpCollectorScreen;
import dev.dubhe.anvilcraft.client.gui.screen.FilterScreen;
import dev.dubhe.anvilcraft.client.gui.screen.FrostAnvilScreen;
import dev.dubhe.anvilcraft.client.gui.screen.FrostGrindstoneScreen;
import dev.dubhe.anvilcraft.client.gui.screen.FrostSmithingScreen;
import dev.dubhe.anvilcraft.client.gui.screen.ItemCollectorScreen;
import dev.dubhe.anvilcraft.client.gui.screen.ItemDetectorScreen;
import dev.dubhe.anvilcraft.client.gui.screen.JewelCraftingScreen;
import dev.dubhe.anvilcraft.client.gui.screen.MagneticChuteScreen;
import dev.dubhe.anvilcraft.client.gui.screen.PulseGeneratorScreen;
import dev.dubhe.anvilcraft.client.gui.screen.RoyalAnvilScreen;
import dev.dubhe.anvilcraft.client.gui.screen.RoyalGrindstoneScreen;
import dev.dubhe.anvilcraft.client.gui.screen.RoyalSmithingScreen;
import dev.dubhe.anvilcraft.client.gui.screen.SliderScreen;
import dev.dubhe.anvilcraft.client.gui.screen.SmartBlockPlacerScreen;
import dev.dubhe.anvilcraft.client.gui.screen.StructureScannerScreen;
import dev.dubhe.anvilcraft.client.gui.screen.StructureToolScreen;
import dev.dubhe.anvilcraft.client.gui.screen.TeslaTowerScreen;
import dev.dubhe.anvilcraft.client.gui.screen.TranscendenceAnvilScreen;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;
import dev.dubhe.anvilcraft.inventory.AdvancedComparatorMenu;
import dev.dubhe.anvilcraft.inventory.BatchCrafterMenu;
import dev.dubhe.anvilcraft.inventory.BatchCutterMenu;
import dev.dubhe.anvilcraft.inventory.ChuteMenu;
import dev.dubhe.anvilcraft.inventory.EmberAnvilMenu;
import dev.dubhe.anvilcraft.inventory.EmberGrindstoneMenu;
import dev.dubhe.anvilcraft.inventory.EmberSmithingMenu;
import dev.dubhe.anvilcraft.inventory.EnergyWeaponMakeMenu;
import dev.dubhe.anvilcraft.inventory.ExpCollectorMenu;
import dev.dubhe.anvilcraft.inventory.FilterMenu;
import dev.dubhe.anvilcraft.inventory.FrostAnvilMenu;
import dev.dubhe.anvilcraft.inventory.FrostGrindstoneMenu;
import dev.dubhe.anvilcraft.inventory.FrostSmithingMenu;
import dev.dubhe.anvilcraft.inventory.ItemCollectorMenu;
import dev.dubhe.anvilcraft.inventory.ItemDetectorMenu;
import dev.dubhe.anvilcraft.inventory.JewelCraftingMenu;
import dev.dubhe.anvilcraft.inventory.MagneticChuteMenu;
import dev.dubhe.anvilcraft.inventory.PulseGeneratorMenu;
import dev.dubhe.anvilcraft.inventory.RoyalAnvilMenu;
import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu;
import dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu;
import dev.dubhe.anvilcraft.inventory.SliderMenu;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.inventory.StructureToolMenu;
import dev.dubhe.anvilcraft.inventory.TeslaTowerMenu;
import dev.dubhe.anvilcraft.inventory.TranscendenceAnvilMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRUM;

public class ModMenuTypes {
    public static final MenuEntry<BatchCrafterMenu> BATCH_CRAFTER = REGISTRUM
        .menu("batch_crafter", BatchCrafterMenu::new, () -> BatchCrafterScreen::new)
        .register();
    public static final MenuEntry<BatchCutterMenu> BATCH_CUTTER = REGISTRUM
        .menu("batch_cutter", BatchCutterMenu::new, () -> BatchCutterScreen::new)
        .register();

    public static final MenuEntry<ChuteMenu> CHUTE =
        REGISTRUM.menu("chute", ChuteMenu::new, () -> ChuteScreen::new).register();

    public static final MenuEntry<MagneticChuteMenu> MAGNETIC_CHUTE = REGISTRUM
        .menu("magnetic_chute", MagneticChuteMenu::new, () -> MagneticChuteScreen::new)
        .register();

    public static final MenuEntry<RoyalGrindstoneMenu> ROYAL_GRINDSTONE = REGISTRUM
        .menu(
            "royal_grindstone",
            (type, id, inv) -> new RoyalGrindstoneMenu(type, id, inv),
            () -> RoyalGrindstoneScreen::new)
        .register();
    public static final MenuEntry<RoyalAnvilMenu> ROYAL_ANVIL = REGISTRUM
        .menu("royal_anvil", (type, id, inv) -> new RoyalAnvilMenu(id, inv), () -> RoyalAnvilScreen::new)
        .register();
    public static final MenuEntry<RoyalSmithingMenu> ROYAL_SMITHING = REGISTRUM
        .menu(
            "royal_smithing_table",
            (type, id, inv) -> new RoyalSmithingMenu(type, id, inv),
            () -> RoyalSmithingScreen::new)
        .register();
    public static final MenuEntry<SliderMenu> SLIDER = REGISTRUM
        .menu(
            "slider",
            (menuType, containerId, inventory) -> new SliderMenu(menuType, containerId),
            () -> SliderScreen::new)
        .register();
    public static final MenuEntry<ItemCollectorMenu> ITEM_COLLECTOR = REGISTRUM
        .menu("item_collector", ItemCollectorMenu::new, () -> ItemCollectorScreen::new)
        .register();
    public static final MenuEntry<ItemDetectorMenu> ITEM_DETECTOR = REGISTRUM
        .menu("item_detector", ItemDetectorMenu::new, () -> ItemDetectorScreen::new)
        .register();
    public static final MenuEntry<FilterMenu> FILTER = REGISTRUM
        .menu("filter", FilterMenu::new, () -> FilterScreen::new)
        .register();

    public static final MenuEntry<ActiveSilencerMenu> ACTIVE_SILENCER = REGISTRUM
        .menu("active_silencer", ActiveSilencerMenu::new, () -> ActiveSilencerScreen::new)
        .register();
    public static final MenuEntry<EmberAnvilMenu> EMBER_ANVIL = REGISTRUM
        .menu("ember_anvil", (type, id, inv) -> new EmberAnvilMenu(id, inv), () -> EmberAnvilScreen::new)
        .register();
    public static final MenuEntry<EmberGrindstoneMenu> EMBER_GRINDSTONE = REGISTRUM
        .menu(
            "ember_grindstone",
            (type, id, inv) -> new EmberGrindstoneMenu(type, id, inv),
            () -> EmberGrindstoneScreen::new)
        .register();
    public static final MenuEntry<EmberSmithingMenu> EMBER_SMITHING = REGISTRUM
        .menu(
            "ember_smithing_table",
            (type, id, inv) -> new EmberSmithingMenu(type, id, inv),
            () -> EmberSmithingScreen::new)
        .register();
    public static final MenuEntry<StructureToolMenu> STRUCTURE_TOOL = REGISTRUM
        .menu("structure_tool", StructureToolMenu::new, () -> StructureToolScreen::new)
        .register();

    public static final MenuEntry<JewelCraftingMenu> JEWEL_CRAFTING = REGISTRUM
        .menu("jewel_crafting", (type, id, inv) -> new JewelCraftingMenu(type, id, inv), () -> JewelCraftingScreen::new)
        .register();

    public static final MenuEntry<TeslaTowerMenu> TESLA_TOWER = REGISTRUM
        .menu("tesla_tower", TeslaTowerMenu::new, () -> TeslaTowerScreen::new)
        .register();

    public static final MenuEntry<PulseGeneratorMenu> PULSE_GENERATOR = REGISTRUM
        .menu("pulse_generator", PulseGeneratorMenu::new, () -> PulseGeneratorScreen::new)
        .register();

    public static final MenuEntry<SmartBlockPlacerMenu> SMART_BLOCK_PLACER = REGISTRUM
        .menu("smart_block_placer", SmartBlockPlacerMenu::new, () -> SmartBlockPlacerScreen::new)
        .register();

    public static final MenuEntry<AdvancedComparatorMenu> ADVANCED_COMPARATOR = REGISTRUM
        .menu("advanced_comparator", AdvancedComparatorMenu::new, () -> AdvancedComparatorScreen::new)
        .register();

    public static final MenuEntry<TranscendenceAnvilMenu> TRANSCENDENCE_ANVIL = REGISTRUM
        .menu("transcendence_anvil", (type, id, inv) -> new TranscendenceAnvilMenu(id, inv), () -> TranscendenceAnvilScreen::new)
        .register();

    public static final MenuEntry<FrostAnvilMenu> FROST_ANVIL = REGISTRUM
        .menu("frost_anvil", (type, id, inv) -> new FrostAnvilMenu(id, inv), () -> FrostAnvilScreen::new)
        .register();
    public static final MenuEntry<FrostGrindstoneMenu> FROST_GRINDSTONE = REGISTRUM
        .menu(
            "frost_grindstone",
            (type, id, inv) -> new FrostGrindstoneMenu(type, id, inv),
            () -> FrostGrindstoneScreen::new)
        .register();
    public static final MenuEntry<FrostSmithingMenu> FROST_SMITHING = REGISTRUM
        .menu(
            "frost_smithing_table",
            (type, id, inv) -> new FrostSmithingMenu(type, id, inv),
            () -> FrostSmithingScreen::new)
        .register();
    public static final MenuEntry<EnergyWeaponMakeMenu> ENERGY_WEAPON_MAKE = REGISTRUM
        .menu("energy_weapon_make", EnergyWeaponMakeMenu::new, () -> EnergyWeaponMakeScreen::new).register();

    public static final MenuEntry<StructureScannerMenu> STRUCTURE_SCANNER = REGISTRUM
        .menu("structure_scanner", (type, id, inv, buf) -> new StructureScannerMenu(type, id, inv, buf), () -> StructureScannerScreen::new)
        .register();

    public static final MenuEntry<ExpCollectorMenu> EXP_COLLECTOR = REGISTRUM
        .menu("exp_collector", ExpCollectorMenu::new, () -> ExpCollectorScreen::new)
        .register();

    public static void register() {
    }

    public static void open(ServerPlayer player, MenuProvider provider) {
        player.openMenu(provider);
    }

    public static void open(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        player.openMenu(provider, pos);
    }
}
