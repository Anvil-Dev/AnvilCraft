package dev.dubhe.anvilcraft.data.recipe.anvil.outcome;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContainer;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

@Getter
public class DamageAnvil implements RecipeOutcome {
    private final String type = "damage_anvil";

    public DamageAnvil() {
    }

    public DamageAnvil(FriendlyByteBuf buffer) {
    }

    public DamageAnvil(JsonObject serializedRecipe) {
    }

    @Override
    public boolean process(@NotNull AnvilCraftingContainer container) {
        container.setAnvilDamage(true);
        return true;
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeUtf(this.getType());
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", this.getType());
        return object;
    }
}
