package dev.dubhe.anvilcraft.network;


import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.api.recipe.AnvilRecipeManager;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import dev.dubhe.anvilcraft.init.ModNetworks;
import dev.emi.emi.runtime.EmiReloadManager;
import lombok.Getter;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.common.registry.ReloadStage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ClientRecipeManagerSyncPack implements Packet {
    private final List<AnvilRecipe> anvilRecipes;

    private boolean isLoaded(String clazz) {
        return ClientRecipeManagerSyncPack.class.getClassLoader().getResource(clazz) != null;
    }

    /**
     * 电网同步
     */
    public ClientRecipeManagerSyncPack(List<AnvilRecipe> anvilRecipeList) {
        this.anvilRecipes = anvilRecipeList;
    }

    /**
     * @param buf 缓冲区
     */
    public ClientRecipeManagerSyncPack(@NotNull FriendlyByteBuf buf) {
        int index = buf.readInt();
        ArrayList<AnvilRecipe> anvilRecipes = new ArrayList<>();
        for (int i = 0; i < index; i++) {
            AnvilRecipe recipe = new AnvilRecipe(buf.readResourceLocation(), buf.readItem());
            recipe.setAnvilRecipeType(AnvilRecipeType.valueOf(buf.readUtf().toUpperCase()));
            int size;
            size = buf.readVarInt();
            for (int j = 0; j < size; j++) {
                recipe.addPredicates(RecipePredicate.fromNetwork(buf));
            }
            size = buf.readVarInt();
            for (int j = 0; j < size; j++) {
                recipe.addOutcomes(RecipeOutcome.fromNetwork(buf));
            }
            anvilRecipes.add(recipe);
        }
        this.anvilRecipes = anvilRecipes;
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.CLIENT_RECIPE_MANAGER_SYNC;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeInt(anvilRecipes.size());
        for (AnvilRecipe recipe : anvilRecipes) {
            buf.writeResourceLocation(recipe.getId());
            buf.writeItem(recipe.getIcon());
            buf.writeUtf(recipe.getAnvilRecipeType().toString());
            buf.writeVarInt(recipe.getPredicates().size());
            for (RecipePredicate predicate : recipe.getPredicates()) {
                predicate.toNetwork(buf);
            }
            buf.writeVarInt(recipe.getOutcomes().size());
            for (RecipeOutcome outcome : recipe.getOutcomes()) {
                outcome.toNetwork(buf);
            }
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handler() {
        Minecraft.getInstance().execute(() -> AnvilRecipeManager.setAnvilRecipeList(this.anvilRecipes));
        if (this.isLoaded("me/shedaniel/rei/impl/client/gui/screen/DefaultDisplayViewingScreen.class")) {
            REIRuntime.getInstance().startReload(ReloadStage.START);
        }
        if (this.isLoaded("dev/emi/emi/api/EmiPlugin.class")) {
            EmiReloadManager.reload();
        }
    }
}
