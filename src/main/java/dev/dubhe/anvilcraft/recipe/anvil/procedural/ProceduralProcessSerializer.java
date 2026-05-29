package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class ProceduralProcessSerializer implements RecipeSerializer<ProceduralProcessRecipe> {

    private static final MapCodec<ProceduralProcessRecipe> CODEC =
        RecordCodecBuilder.mapCodec(ins -> ins.group(
                BlockStatePredicate.CODEC.fieldOf("initial_block").forGetter(ProceduralProcessRecipe::getInitialBlock),
                ProceduralProcessStep.CODEC.listOf().fieldOf("steps").forGetter(ProceduralProcessRecipe::getSteps),
                ChanceBlockState.CODEC.fieldOf("result_block").forGetter(ProceduralProcessRecipe::getResultBlock),
                ItemStack.CODEC.fieldOf("icon").forGetter(ProceduralProcessRecipe::getIcon),
                Codec.INT.fieldOf("loop").forGetter(ProceduralProcessRecipe::getLoop),
                ProceduralProcessStep.CODEC
                    .optionalFieldOf("multiple_loop_first_step")
                    .forGetter(ProceduralProcessRecipe::getMultiLoopFirstStep)
                )
            .apply(ins, ProceduralProcessRecipe::new)
        );

    private static final StreamCodec<RegistryFriendlyByteBuf, ProceduralProcessRecipe> STREAM_CODEC =
        StreamCodec.composite(
            BlockStatePredicate.STREAM_CODEC,
            ProceduralProcessRecipe::getInitialBlock,
            ProceduralProcessStep.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ProceduralProcessRecipe::getSteps,
            ChanceBlockState.STREAM_CODEC,
            ProceduralProcessRecipe::getResultBlock,
            ItemStack.STREAM_CODEC,
            ProceduralProcessRecipe::getIcon,
            ByteBufCodecs.INT,
            ProceduralProcessRecipe::getLoop,
            ByteBufCodecs.optional(ProceduralProcessStep.STREAM_CODEC),
            ProceduralProcessRecipe::getMultiLoopFirstStep,
            ProceduralProcessRecipe::new
        );

    @Override
    public @NotNull MapCodec<ProceduralProcessRecipe> codec() {
        return CODEC;
    }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, ProceduralProcessRecipe> streamCodec() {
        return STREAM_CODEC;
    }

}
