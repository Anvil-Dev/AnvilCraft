package dev.dubhe.anvilcraft.integration.polymorph;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;

public final class StoragePolymorphBridge {
    private static final String API_CLASS = "com.illusivesoulworks.polymorph.api.PolymorphApi";
    private static final String MANAGER_CLASS = "com.illusivesoulworks.polymorph.api.common.base.IPolymorphRecipeManager";
    private static final String PLAYER_DATA_CLASS = "com.illusivesoulworks.polymorph.api.common.capability.IPlayerRecipeData";
    private static final String NETWORK_CLASS = "com.illusivesoulworks.polymorph.api.common.base.IPolymorphNetwork";
    private static final String WIDGETS_CLASS = "com.illusivesoulworks.polymorph.api.client.PolymorphWidgets";
    private static final String WIDGET_FACTORY_CLASS = "com.illusivesoulworks.polymorph.api.client.PolymorphWidgets$IRecipesWidgetFactory";
    private static final String WIDGET_CLASS = "com.illusivesoulworks.polymorph.api.client.base.IRecipesWidget";

    private static volatile boolean resolved;
    private static volatile @Nullable Api api;
    private static volatile boolean widgetSuppressed;

    private StoragePolymorphBridge() {
    }

    public static boolean isLoaded() {
        return StoragePolymorphBridge.resolve() != null;
    }


    public static void suppressWidget(Class<?> screenClass) {
        if (StoragePolymorphBridge.widgetSuppressed) {
            return;
        }
        StoragePolymorphBridge.widgetSuppressed = true;
        try {
            Class<?> widgetsClass = Class.forName(StoragePolymorphBridge.WIDGETS_CLASS);
            Class<?> factoryClass = Class.forName(StoragePolymorphBridge.WIDGET_FACTORY_CLASS);
            Class<?> widgetClass = Class.forName(StoragePolymorphBridge.WIDGET_CLASS);
            Object widgets = widgetsClass.getMethod("getInstance").invoke(null);
            Object widget = Proxy.newProxyInstance(
                widgetClass.getClassLoader(),
                new Class<?>[]{widgetClass},
                StoragePolymorphBridge::defaultValue
            );
            Object factory = Proxy.newProxyInstance(
                factoryClass.getClassLoader(),
                new Class<?>[]{factoryClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("createWidget") && args != null && args.length == 1
                        && screenClass.isInstance(args[0])) {
                        return widget;
                    }
                    return StoragePolymorphBridge.defaultValue(proxy, method, args);
                }
            );
            widgetsClass.getMethod("registerWidget", factoryClass).invoke(widgets, factory);
        } catch (ClassNotFoundException | NoSuchMethodException error) {
            AnvilCraft.LOGGER.debug("Polymorph widget API is unavailable, keeping its own recipe widget", error);
        } catch (InvocationTargetException error) {
            AnvilCraft.LOGGER.warn("Could not suppress the Polymorph recipe widget", error.getCause());
        } catch (IllegalAccessException error) {
            AnvilCraft.LOGGER.warn("Could not suppress the Polymorph recipe widget", error);
        }
    }

    private static @Nullable Object defaultValue(Object proxy, Method method, Object @Nullable [] args) {
        Class<?> type = method.getReturnType();
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == float.class) {
            return 0F;
        }
        return switch (method.getName()) {
            case "toString" -> "StorageScreenPolymorphWidget";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
            default -> null;
        };
    }

    public static @Nullable RecipeHolder<?> playerRecipe(
        Player player,
        AbstractContainerMenu menu,
        CraftingInput input,
        Level level
    ) {
        Api api = StoragePolymorphBridge.resolve();
        if (api == null) {
            return null;
        }
        try {
            Object instance = api.getInstance().invoke(null);
            Object manager = api.getRecipeManager().invoke(instance);
            Object result = api.getPlayerRecipe().invoke(
                manager,
                menu,
                RecipeType.CRAFTING,
                input,
                level,
                player
            );
            if (!(result instanceof Optional<?> optional) || optional.isEmpty()) {
                return null;
            }
            return optional.get() instanceof RecipeHolder<?> holder ? holder : null;
        } catch (InvocationTargetException error) {
            StoragePolymorphBridge.disable("resolve the player crafting recipe", error.getCause());
            return null;
        } catch (IllegalAccessException error) {
            StoragePolymorphBridge.disable("resolve the player crafting recipe", error);
            return null;
        }
    }

    public static @Nullable RecipeHolder<?> selectedRecipe(Player player) {
        Api api = StoragePolymorphBridge.resolve();
        if (api == null) {
            return null;
        }
        try {
            Object instance = api.getInstance().invoke(null);
            Object playerData = api.getPlayerRecipeData().invoke(instance, player);
            return api.getSelectedRecipe().invoke(playerData) instanceof RecipeHolder<?> holder ? holder : null;
        } catch (InvocationTargetException error) {
            StoragePolymorphBridge.disable("read the selected crafting recipe", error.getCause());
            return null;
        } catch (IllegalAccessException error) {
            StoragePolymorphBridge.disable("read the selected crafting recipe", error);
            return null;
        }
    }


    public static boolean select(Level level, Player player, Identifier recipeId) {
        Api api = StoragePolymorphBridge.resolve();
        if (api == null) {
            return false;
        }
        RecipeHolder<?> selected = RecipesRecord.getRecipes(level)
            .byKey(ResourceKey.create(Registries.RECIPE, recipeId));
        if (selected == null) {
            return false;
        }
        try {
            Object instance = api.getInstance().invoke(null);
            Object playerData = api.getPlayerRecipeData().invoke(instance, player);
            api.selectRecipe().invoke(playerData, selected);
            Object network = api.getNetwork().invoke(instance);
            api.selectRecipeC2S().invoke(network, recipeId);
            return true;
        } catch (InvocationTargetException error) {
            StoragePolymorphBridge.disable("select the storage crafting recipe", error.getCause());
            return false;
        } catch (IllegalAccessException error) {
            StoragePolymorphBridge.disable("select the storage crafting recipe", error);
            return false;
        }
    }

    private static synchronized @Nullable Api resolve() {
        if (StoragePolymorphBridge.resolved) {
            return StoragePolymorphBridge.api;
        }
        StoragePolymorphBridge.resolved = true;
        try {
            Class<?> apiClass = Class.forName(StoragePolymorphBridge.API_CLASS);
            Class<?> managerClass = Class.forName(StoragePolymorphBridge.MANAGER_CLASS);
            Class<?> playerDataClass = Class.forName(StoragePolymorphBridge.PLAYER_DATA_CLASS);
            Class<?> networkClass = Class.forName(StoragePolymorphBridge.NETWORK_CLASS);
            StoragePolymorphBridge.api = new Api(
                apiClass.getMethod("getInstance"),
                apiClass.getMethod("getRecipeManager"),
                apiClass.getMethod("getPlayerRecipeData", Player.class),
                apiClass.getMethod("getNetwork"),
                managerClass.getMethod(
                    "getPlayerRecipe",
                    AbstractContainerMenu.class,
                    RecipeType.class,
                    RecipeInput.class,
                    Level.class,
                    Player.class
                ),
                playerDataClass.getMethod("selectRecipe", RecipeHolder.class),
                playerDataClass.getMethod("getSelectedRecipe"),
                networkClass.getMethod("sendPlayerRecipeSelectionC2S", Identifier.class)
            );
        } catch (ClassNotFoundException | NoSuchMethodException error) {
            AnvilCraft.LOGGER.debug(
                "Polymorph API is unavailable, storage crafting keeps the first matching recipe",
                error
            );
            StoragePolymorphBridge.api = null;
        }
        return StoragePolymorphBridge.api;
    }

    private static void disable(String action, Throwable error) {
        StoragePolymorphBridge.api = null;
        StoragePolymorphBridge.resolved = true;
        AnvilCraft.LOGGER.error("Disabled the Polymorph integration while trying to {}", action, error);
    }

    private record Api(
        Method getInstance,
        Method getRecipeManager,
        Method getPlayerRecipeData,
        Method getNetwork,
        Method getPlayerRecipe,
        Method selectRecipe,
        Method getSelectedRecipe,
        Method selectRecipeC2S
    ) {
    }
}
