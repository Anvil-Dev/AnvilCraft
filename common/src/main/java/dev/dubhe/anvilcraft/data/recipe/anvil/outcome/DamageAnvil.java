package dev.dubhe.anvilcraft.data.recipe.anvil.outcome;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContainer;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@SuppressWarnings("unused")
public class DamageAnvil implements RecipeOutcome {
    private final String type = "damage_anvil";
    private final double chance;

    public DamageAnvil(double chance) {
        this.chance = chance;
    }

    public DamageAnvil() {
        this(1.0);
    }

    public DamageAnvil(@NotNull FriendlyByteBuf buffer) {
        this(buffer.readDouble());
    }

    /**
     * 破坏铁砧
     *
     * @param serializedRecipe 序列化配方
     */
    public DamageAnvil(@NotNull JsonObject serializedRecipe) {
        if (serializedRecipe.has("chance")) {
            this.chance = GsonHelper.getAsDouble(serializedRecipe, "chance");
        } else this.chance = 1.0;
    }

    @Override
    public boolean process(@NotNull AnvilCraftingContainer container) {
        container.setAnvilDamage(true);
        return true;
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeUtf(this.getType());
        buffer.writeDouble(this.chance);
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", this.getType());
        object.addProperty("chance", this.chance);
        return object;
    }
}
