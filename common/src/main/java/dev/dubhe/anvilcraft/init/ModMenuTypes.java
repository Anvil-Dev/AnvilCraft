package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.builders.MenuBuilder;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.*;
import dev.dubhe.anvilcraft.inventory.*;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

public class ModMenuTypes {
    public static final MenuBuilder<AutoCrafterMenu, AutoCrafterScreen, Registrate> AUTO_CRAFTER = REGISTRATE.menu("auto_crafter", AutoCrafterMenu::clientOf, () -> AutoCrafterScreen::new);
    public static final MenuBuilder<ChuteMenu, ChuteScreen, Registrate> CHUTE = REGISTRATE.menu("chute", (type, id, inventory) -> new ChuteMenu(type, id, inventory), () -> ChuteScreen::new);
    public static final MenuBuilder<RoyalGrindstoneMenu, RoyalGrindstoneScreen, Registrate> ROYAL_GRINDSTONE = REGISTRATE.menu("royal_grindstone", (type, id, inventory) -> new RoyalGrindstoneMenu(type, id, inventory), () -> RoyalGrindstoneScreen::new);
    public static final MenuBuilder<RoyalAnvilMenu, RoyalAnvilScreen, Registrate> ROYAL_ANVIL = REGISTRATE.menu("royal_anvil", (type, id, inventory) -> new RoyalAnvilMenu(id, inventory), () -> RoyalAnvilScreen::new);
    public static final MenuBuilder<RoyalSmithingMenu, RoyalSmithingScreen, Registrate> ROYAL_SMITHING = REGISTRATE.menu("royal_smithing_table", (type, id, inventory) -> new RoyalSmithingMenu(type, id, inventory), () -> RoyalSmithingScreen::new);

    public static void register() {
    }
}
