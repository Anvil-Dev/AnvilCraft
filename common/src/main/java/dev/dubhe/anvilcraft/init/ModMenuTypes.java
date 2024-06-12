package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.client.gui.screen.inventory.ActiveSilencerScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.AutoCrafterScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.ChuteScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.ItemCollectorScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.RoyalAnvilScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.RoyalGrindstoneScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.RoyalSmithingScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.SliderScreen;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;
import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
import dev.dubhe.anvilcraft.inventory.ChuteMenu;
import dev.dubhe.anvilcraft.inventory.ItemCollectorMenu;
import dev.dubhe.anvilcraft.inventory.RoyalAnvilMenu;
import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu;
import dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu;
import dev.dubhe.anvilcraft.inventory.SliderMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

import com.tterrag.registrate.util.entry.MenuEntry;
import dev.architectury.injectables.annotations.ExpectPlatform;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

public class ModMenuTypes {
    @SuppressWarnings("DataFlowIssue")
    public static final MenuEntry<AutoCrafterMenu> AUTO_CRAFTER = REGISTRATE
            .menu("auto_crafter", AutoCrafterMenu::new, () -> AutoCrafterScreen::new)
            .register();

    @SuppressWarnings("DataFlowIssue")
    public static final MenuEntry<ChuteMenu> CHUTE =
            REGISTRATE.menu("chute", ChuteMenu::new, () -> ChuteScreen::new).register();

    public static final MenuEntry<RoyalGrindstoneMenu> ROYAL_GRINDSTONE = REGISTRATE
            .menu(
                    "royal_grindstone",
                    (type, id, inv) -> new RoyalGrindstoneMenu(type, id, inv),
                    () -> RoyalGrindstoneScreen::new)
            .register();
    public static final MenuEntry<RoyalAnvilMenu> ROYAL_ANVIL = REGISTRATE
            .menu(
                    "royal_anvil",
                    (type, id, inv) -> new RoyalAnvilMenu(id, inv),
                    () -> RoyalAnvilScreen::new)
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

    public static void register() {}

    @ExpectPlatform
    public static void open(ServerPlayer player, MenuProvider provider) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void open(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        throw new AssertionError();
    }
}
