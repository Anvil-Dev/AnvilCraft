package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.MenuEntry;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.AutoCrafterScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.ChuteScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.RoyalAnvilScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.RoyalGrindstoneScreen;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.RoyalSmithingScreen;
import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
import dev.dubhe.anvilcraft.inventory.ChuteMenu;
import dev.dubhe.anvilcraft.inventory.RoyalAnvilMenu;
import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu;
import dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

public class ModMenuTypes {
    @SuppressWarnings("DataFlowIssue")
    public static final MenuEntry<AutoCrafterMenu> AUTO_CRAFTER = REGISTRATE
            .menu("auto_crafter", AutoCrafterMenu::new, () -> AutoCrafterScreen::new)
            .register();
    @SuppressWarnings("DataFlowIssue")
    public static final MenuEntry<ChuteMenu> CHUTE = REGISTRATE
            .menu("chute", ChuteMenu::new, () -> ChuteScreen::new)
            .register();
    public static final MenuEntry<RoyalGrindstoneMenu> ROYAL_GRINDSTONE = REGISTRATE
            .menu("royal_grindstone", (type, id, inventory) ->
                new RoyalGrindstoneMenu(type, id, inventory), () -> RoyalGrindstoneScreen::new)
            .register();
    public static final MenuEntry<RoyalAnvilMenu> ROYAL_ANVIL = REGISTRATE
            .menu("royal_anvil", (type, id, inventory) ->
                new RoyalAnvilMenu(id, inventory), () -> RoyalAnvilScreen::new)
            .register();
    public static final MenuEntry<RoyalSmithingMenu> ROYAL_SMITHING = REGISTRATE
            .menu("royal_smithing_table", (type, id, inventory) ->
                new RoyalSmithingMenu(type, id, inventory), () -> RoyalSmithingScreen::new)
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
