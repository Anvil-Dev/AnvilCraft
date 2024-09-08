package dev.dubhe.anvilcraft.network;


import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.recipe.AnvilRecipeManager;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;

import lombok.Getter;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ClientRecipeManagerSyncPack implements CustomPacketPayload {
    public static final Type<ClientRecipeManagerSyncPack> TYPE =
        new Type<>(AnvilCraft.of("client_recipe_manager_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientRecipeManagerSyncPack> STREAM_CODEC =
        StreamCodec.ofMember(
            ClientRecipeManagerSyncPack::encode, ClientRecipeManagerSyncPack::new
        );
    public static final IPayloadHandler<ClientRecipeManagerSyncPack> HANDLER = ClientRecipeManagerSyncPack::clientHandler;


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
    public ClientRecipeManagerSyncPack(@NotNull RegistryFriendlyByteBuf buf) {
        int index = buf.readInt();
        ArrayList<AnvilRecipe> anvilRecipes = new ArrayList<>();
        for (int i = 0; i < index; i++) {
            AnvilRecipe recipe = new AnvilRecipe(
                buf.readResourceLocation(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
            );
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

    /**
     *
     */
    public void encode(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeInt(anvilRecipes.size());
        for (AnvilRecipe recipe : anvilRecipes) {
            buf.writeResourceLocation(recipe.getId());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.getIcon());
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
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     *
     */
    public static void clientHandler(ClientRecipeManagerSyncPack data, IPayloadContext context) {
        context.enqueueWork(() -> AnvilRecipeManager.setAnvilRecipeList(data.anvilRecipes));
        // TODO: REI EMI
        if (data.isLoaded("me/shedaniel/rei/impl/client/gui/screen/DefaultDisplayViewingScreen.class")) {
//            REIRuntime.getInstance().startReload(ReloadStage.START);
        }
        if (data.isLoaded("dev/emi/emi/api/EmiPlugin.class")) {
//            EmiReloadManager.reload();
        }
    }
}
