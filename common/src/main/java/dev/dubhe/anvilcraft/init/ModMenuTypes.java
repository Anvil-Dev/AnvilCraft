package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.MenuEntry;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.ActiveSilencerScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.AutoCrafterScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.BatchCrafterScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.ChuteScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.EmberAnvilScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.EmberGrindstoneScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.EmberSmithingScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.ItemCollectorScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.MagneticChuteScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.RoyalAnvilScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.RoyalGrindstoneScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.RoyalSmithingScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.SliderScreen;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;
import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
import dev.dubhe.anvilcraft.inventory.BatchCrafterMenu;
import dev.dubhe.anvilcraft.inventory.ChuteMenu;
import dev.dubhe.anvilcraft.inventory.EmberAnvilMenu;
import dev.dubhe.anvilcraft.inventory.EmberGrindstoneMenu;
import dev.dubhe.anvilcraft.inventory.EmberSmithingMenu;
import dev.dubhe.anvilcraft.inventory.ItemCollectorMenu;
import dev.dubhe.anvilcraft.inventory.MagneticChuteMenu;
import dev.dubhe.anvilcraft.inventory.RoyalAnvilMenu;
import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu;
import dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu;
import dev.dubhe.anvilcraft.inventory.SliderMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATOR;

public class ModMenuTypes {

    @SuppressWarnings("DataFlowIssue")
    public static final MenuEntry<AutoCrafterMenu> AUTO_CRAFTER = REGISTRATOR
            .menu("auto_crafter", AutoCrafterMenu::new, () -> AutoCrafterScreen::new)
            .register();

    @SuppressWarnings("DataFlowIssue")
    public static final MenuEntry<BatchCrafterMenu> BATCH_CRAFTER = REGISTRATOR
            .menu("batch_crafter", BatchCrafterMenu::new, () -> BatchCrafterScreen::new)
            .register();

    @SuppressWarnings("DataFlowIssue")
    public static final MenuEntry<ChuteMenu> CHUTE = REGISTRATOR
            .menu("chute", ChuteMenu::new, () -> ChuteScreen::new)
            .register();

    @SuppressWarnings("DataFlowIssue")
    public static final MenuEntry<MagneticChuteMenu> MAGNETIC_CHUTE = REGISTRATOR
            .menu("magnetic_chute", MagneticChuteMenu::new, () -> MagneticChuteScreen::new)
            .register();
    public static final MenuEntry<RoyalGrindstoneMenu> ROYAL_GRINDSTONE = REGISTRATOR
            .menu(
                    "royal_grindstone",
                    (type, id, inv) -> new RoyalGrindstoneMenu(type, id, inv),
                    () -> RoyalGrindstoneScreen::new
            )
            .register();
    public static final MenuEntry<RoyalAnvilMenu> ROYAL_ANVIL = REGISTRATOR
            .menu(
                    "royal_anvil",
                    (type, id, inv) -> new RoyalAnvilMenu(id, inv),
                    () -> RoyalAnvilScreen::new
            )
            .register();
    public static final MenuEntry<RoyalSmithingMenu> ROYAL_SMITHING = REGISTRATOR
            .menu(
                    "royal_smithing_table",
                    (type, id, inv) -> new RoyalSmithingMenu(type, id, inv),
                    () -> RoyalSmithingScreen::new
            )
            .register();
    public static final MenuEntry<SliderMenu> SLIDER = REGISTRATOR
            .menu(
                    "slider",
                    (menuType, containerId, inventory) -> new SliderMenu(menuType, containerId),
                    () -> SliderScreen::new
            )
            .register();
    public static final MenuEntry<ItemCollectorMenu> ITEM_COLLECTOR = REGISTRATOR
            .menu("item_collector", ItemCollectorMenu::new, () -> ItemCollectorScreen::new)
            .register();

    public static final MenuEntry<ActiveSilencerMenu> ACTIVE_SILENCER = REGISTRATOR
            .menu("active_silencer", ActiveSilencerMenu::new, () -> ActiveSilencerScreen::new)
            .register();
    public static final MenuEntry<EmberAnvilMenu> EMBER_ANVIL = REGISTRATOR
            .menu(
                    "ember_anvil",
                    (type, id, inv) -> new EmberAnvilMenu(id, inv),
                    () -> EmberAnvilScreen::new
            )
            .register();
    public static final MenuEntry<EmberGrindstoneMenu> EMBER_GRINDSTONE = REGISTRATOR
            .menu(
                    "ember_grindstone",
                    (type, id, inv) -> new EmberGrindstoneMenu(type, id, inv),
                    () -> EmberGrindstoneScreen::new
            )
            .register();
    public static final MenuEntry<EmberSmithingMenu> EMBER_SMITHING = REGISTRATOR
            .menu(
                    "ember_smithing_table",
                    (type, id, inv) -> new EmberSmithingMenu(type, id, inv),
                    () -> EmberSmithingScreen::new
            )
            .register();

    public static void register() {
    }

    @ExpectPlatform
    public static void open(ServerPlayer player, MenuProvider provider) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void open(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        throw new AssertionError();
    }
}
