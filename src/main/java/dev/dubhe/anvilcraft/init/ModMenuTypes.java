package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.client.gui.screen.ActiveSilencerScreen;
import dev.dubhe.anvilcraft.client.gui.screen.BatchCrafterScreen;
import dev.dubhe.anvilcraft.client.gui.screen.ChuteScreen;
import dev.dubhe.anvilcraft.client.gui.screen.EmberAnvilScreen;
import dev.dubhe.anvilcraft.client.gui.screen.EmberGrindstoneScreen;
import dev.dubhe.anvilcraft.client.gui.screen.EmberSmithingScreen;
import dev.dubhe.anvilcraft.client.gui.screen.ItemCollectorScreen;
import dev.dubhe.anvilcraft.client.gui.screen.JewelCraftingScreen;
import dev.dubhe.anvilcraft.client.gui.screen.MagneticChuteScreen;
import dev.dubhe.anvilcraft.client.gui.screen.RoyalAnvilScreen;
import dev.dubhe.anvilcraft.client.gui.screen.RoyalGrindstoneScreen;
import dev.dubhe.anvilcraft.client.gui.screen.RoyalSmithingScreen;
import dev.dubhe.anvilcraft.client.gui.screen.SliderScreen;
import dev.dubhe.anvilcraft.client.gui.screen.StructureToolScreen;
import dev.dubhe.anvilcraft.client.gui.screen.TeslaTowerScreen;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;
import dev.dubhe.anvilcraft.inventory.BatchCrafterMenu;
import dev.dubhe.anvilcraft.inventory.ChuteMenu;
import dev.dubhe.anvilcraft.inventory.EmberAnvilMenu;
import dev.dubhe.anvilcraft.inventory.EmberGrindstoneMenu;
import dev.dubhe.anvilcraft.inventory.EmberSmithingMenu;
import dev.dubhe.anvilcraft.inventory.ItemCollectorMenu;
import dev.dubhe.anvilcraft.inventory.JewelCraftingMenu;
import dev.dubhe.anvilcraft.inventory.MagneticChuteMenu;
import dev.dubhe.anvilcraft.inventory.RoyalAnvilMenu;
import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu;
import dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu;
import dev.dubhe.anvilcraft.inventory.SliderMenu;
import dev.dubhe.anvilcraft.inventory.StructureToolMenu;

import dev.dubhe.anvilcraft.inventory.TeslaTowerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

import com.tterrag.registrate.util.entry.MenuEntry;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

@SuppressWarnings("DataFlowIssue")
public class ModMenuTypes {
    @SuppressWarnings("DataFlowIssue")
    public static final MenuEntry<BatchCrafterMenu> BATCH_CRAFTER = REGISTRATE
        .menu("batch_crafter", BatchCrafterMenu::new, () -> BatchCrafterScreen::new)
        .register();

    @SuppressWarnings("DataFlowIssue")
    public static final MenuEntry<ChuteMenu> CHUTE =
        REGISTRATE.menu("chute", ChuteMenu::new, () -> ChuteScreen::new).register();

    @SuppressWarnings("DataFlowIssue")
    public static final MenuEntry<MagneticChuteMenu> MAGNETIC_CHUTE = REGISTRATE
        .menu("magnetic_chute", MagneticChuteMenu::new, () -> MagneticChuteScreen::new)
        .register();

    public static final MenuEntry<RoyalGrindstoneMenu> ROYAL_GRINDSTONE = REGISTRATE
        .menu(
            "royal_grindstone",
            (type, id, inv) -> new RoyalGrindstoneMenu(type, id, inv),
            () -> RoyalGrindstoneScreen::new)
        .register();
    public static final MenuEntry<RoyalAnvilMenu> ROYAL_ANVIL = REGISTRATE
        .menu("royal_anvil", (type, id, inv) -> new RoyalAnvilMenu(id, inv), () -> RoyalAnvilScreen::new)
        .register();
    public static final MenuEntry<RoyalSmithingMenu> ROYAL_SMITHING = REGISTRATE
        .menu(
            "royal_smithing_table",
            (type, id, inv) -> new RoyalSmithingMenu(type, id, inv),
            () -> RoyalSmithingScreen::new)
        .register();
    public static final MenuEntry<SliderMenu> SLIDER = REGISTRATE
        .menu(
            "slider",
            (menuType, containerId, inventory) -> new SliderMenu(menuType, containerId),
            () -> SliderScreen::new)
        .register();
    public static final MenuEntry<ItemCollectorMenu> ITEM_COLLECTOR = REGISTRATE
        .menu("item_collector", ItemCollectorMenu::new, () -> ItemCollectorScreen::new)
        .register();

    public static final MenuEntry<ActiveSilencerMenu> ACTIVE_SILENCER = REGISTRATE
        .menu("active_silencer", ActiveSilencerMenu::new, () -> ActiveSilencerScreen::new)
        .register();
    public static final MenuEntry<EmberAnvilMenu> EMBER_ANVIL = REGISTRATE
        .menu("ember_anvil", (type, id, inv) -> new EmberAnvilMenu(id, inv), () -> EmberAnvilScreen::new)
        .register();
    public static final MenuEntry<EmberGrindstoneMenu> EMBER_GRINDSTONE = REGISTRATE
        .menu(
            "ember_grindstone",
            (type, id, inv) -> new EmberGrindstoneMenu(type, id, inv),
            () -> EmberGrindstoneScreen::new)
        .register();
    public static final MenuEntry<EmberSmithingMenu> EMBER_SMITHING = REGISTRATE
        .menu(
            "ember_smithing_table",
            (type, id, inv) -> new EmberSmithingMenu(type, id, inv),
            () -> EmberSmithingScreen::new)
        .register();
    public static final MenuEntry<StructureToolMenu> STRUCTURE_TOOL = REGISTRATE
        .menu("structure_tool", StructureToolMenu::new, () -> StructureToolScreen::new)
        .register();

    public static final MenuEntry<JewelCraftingMenu> JEWEL_CRAFTING = REGISTRATE
        .menu("jewel_crafting", (type, id, inv) -> new JewelCraftingMenu(type, id, inv), () -> JewelCraftingScreen::new)
        .register();

    public static final MenuEntry<TeslaTowerMenu> TESLA_TOWER = REGISTRATE
            .menu("tesla_tower", TeslaTowerMenu::new, () -> TeslaTowerScreen::new)
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
