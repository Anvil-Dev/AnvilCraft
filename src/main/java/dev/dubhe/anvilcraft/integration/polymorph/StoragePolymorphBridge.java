package dev.dubhe.anvilcraft.integration.polymorph;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.resources.ResourceLocation;
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
import java.util.Optional;

public final class StoragePolymorphBridge {
    private static final String API_CLASS = "com.illusivesoulworks.polymorph.api.PolymorphApi";
    private static final String MANAGER_CLASS = "com.illusivesoulworks.polymorph.api.common.base.IPolymorphRecipeManager";
    private static final String PLAYER_DATA_CLASS = "com.illusivesoulworks.polymorph.api.common.capability.IPlayerRecipeData";
    private static final String NETWORK_CLASS = "com.illusivesoulworks.polymorph.api.common.base.IPolymorphNetwork";

    private static volatile boolean resolved;
    private static volatile @Nullable Api api;

    private StoragePolymorphBridge() {
    }

    public static boolean isLoaded() {
        return StoragePolymorphBridge.resolve() != null;
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

    public static boolean select(Level level, Player player, ResourceLocation recipeId) {
        Api api = StoragePolymorphBridge.resolve();
        if (api == null) {
            return false;
        }
        Optional<? extends RecipeHolder<?>> selected = level.getRecipeManager().byKey(recipeId);
        if (selected.isEmpty()) {
            return false;
        }
        try {
            Object instance = api.getInstance().invoke(null);
            Object playerData = api.getPlayerRecipeData().invoke(instance, player);
            api.selectRecipe().invoke(playerData, selected.get());
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
                networkClass.getMethod("sendPlayerRecipeSelectionC2S", ResourceLocation.class)
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

    /** 解析后的反射入口，整体替换即可完成降级，避免逐个字段判空。 */
    private record Api(
        Method getInstance,
        Method getRecipeManager,
        Method getPlayerRecipeData,
        Method getNetwork,
        Method getPlayerRecipe,
        Method selectRecipe,
        Method selectRecipeC2S
    ) {
    }
}
