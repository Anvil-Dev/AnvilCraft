package dev.dubhe.anvilcraft.data.recipe.anvil.predicate;

import com.google.gson.JsonObject;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContext;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Getter
public class HasBlockIngredient extends HasBlock {
    private final String type = "has_block_ingredient";

    public HasBlockIngredient(Vec3 offset, HasBlock.ModBlockPredicate matchBlock) {
        super(offset, matchBlock);
    }

    public HasBlockIngredient(JsonObject serializedRecipe) {
        super(serializedRecipe);
    }

    public HasBlockIngredient(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public boolean process(@NotNull AnvilCraftingContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getPos();
        Vec3 vec3 = pos.getCenter().add(this.offset);
        BlockPos blockPos = BlockPos.containing(vec3.x, vec3.y, vec3.z);
        return level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
    }
}
