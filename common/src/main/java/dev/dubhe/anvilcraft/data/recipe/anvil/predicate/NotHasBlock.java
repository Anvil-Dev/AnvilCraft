package dev.dubhe.anvilcraft.data.recipe.anvil.predicate;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import com.google.gson.JsonObject;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class NotHasBlock extends HasBlock {
    private final String type = "not_has_block";

    @Override
    public boolean matches(@NotNull AnvilCraftingContainer container) {
        return !super.matches(container);
    }

    public NotHasBlock(Vec3 offset, ModBlockPredicate matchBlock) {
        super(offset, matchBlock);
    }

    public NotHasBlock(JsonObject serializedRecipe) {
        super(serializedRecipe);
    }

    public NotHasBlock(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }
}
