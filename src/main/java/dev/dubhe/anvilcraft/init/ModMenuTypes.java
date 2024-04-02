package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

public class ModMenuTypes {
    public static final MenuType<AutoCrafterMenu> AUTO_CRAFTER = register("auto_crafter", AutoCrafterMenu::clientOf);
    public static final MenuType<RoyalGrindstoneMenu> ROYAL_GRINDSTONE = register("royal_grindstone", RoyalGrindstoneMenu::new);
    public static final MenuType<ChuteMenu> CHUTE = ModMenuTypes.register("chute", ChuteMenu::new);
    public static final MenuType<RoyalAnvilMenu> ROYAL_ANVIL = ModMenuTypes.register("royal_anvil", RoyalAnvilMenu::new);
    public static final MenuType<RoyalSmithingMenu> ROYAL_SMITHING = ModMenuTypes.register("royal_smithing_table", RoyalSmithingMenu::new);

    public static void register() {
    }

    private static <T extends AbstractContainerMenu> @NotNull MenuType<T> register(@NotNull String key, @NotNull MenuType.MenuSupplier<T> factory) {
        return Registry.register(BuiltInRegistries.MENU, AnvilCraft.of(key), new MenuType<>(factory, FeatureFlags.VANILLA_SET));
    }
}
