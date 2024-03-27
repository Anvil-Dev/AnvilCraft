package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

public class ModMenuTypes {
    public static final MenuType<AutoCrafterMenu> AUTO_CRAFTER = ModMenuTypes.register("auto_crafter", AutoCrafterMenu::new);

    public static void register() {
    }

    private static <T extends AbstractContainerMenu> @NotNull MenuType<T> register(@NotNull String key, @NotNull MenuType.MenuSupplier<T> factory) {
        return Registry.register(BuiltInRegistries.MENU, key, new MenuType<>(factory, FeatureFlags.VANILLA_SET));
    }
}
