package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.Optional;

public class ProceduralProcessSerializer {

    public static final MapCodec<ProceduralProcessRecipe> CODEC =
        RecordCodecBuilder.mapCodec(ins -> ins.group(
                    BlockStatePredicate.CODEC.fieldOf("initial_block").forGetter(ProceduralProcessRecipe::getInitialBlock),
                    ProceduralProcessStep.CODEC.listOf().fieldOf("steps").forGetter(ProceduralProcessRecipe::getSteps),
                    ChanceBlockState.CODEC.fieldOf("result_block").forGetter(ProceduralProcessRecipe::getResultBlock),
                    ItemStack.CODEC.optionalFieldOf("icon")
                        .forGetter(r -> r.getIcon().isEmpty() ? Optional.empty() : Optional.of(r.getIcon())),
                    Codec.INT.fieldOf("loop").forGetter(ProceduralProcessRecipe::getLoop),
                    Identifier.CODEC.optionalFieldOf("displayed_model").forGetter(ProceduralProcessRecipe::getDisplayedModel),
                    ProceduralProcessStep.CODEC
                        .optionalFieldOf("multiple_loop_first_step")
                        .forGetter(ProceduralProcessRecipe::getMultiLoopFirstStep)
                )
                .apply(
                    ins, (initialBlock, steps, resultBlock, icon, loop, displayedModel, multiLoopFirstStep) ->
                        new ProceduralProcessRecipe(
                            initialBlock,
                            steps,
                            resultBlock,
                            icon.orElse(ItemStack.EMPTY),
                            loop,
                            displayedModel,
                            multiLoopFirstStep
                        )
                )
        );

    public static final StreamCodec<RegistryFriendlyByteBuf, ProceduralProcessRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ProceduralProcessRecipe recipe) {
            BlockStatePredicate.STREAM_CODEC.encode(buffer, recipe.getInitialBlock());
            ProceduralProcessStep.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, recipe.getSteps());
            ChanceBlockState.STREAM_CODEC.encode(buffer, recipe.getResultBlock());
            ItemStack.STREAM_CODEC.encode(buffer, recipe.getIcon());
            buffer.writeVarInt(recipe.getLoop());
            ByteBufCodecs.optional(Identifier.STREAM_CODEC).encode(buffer, recipe.getDisplayedModel());
            ByteBufCodecs.optional(ProceduralProcessStep.STREAM_CODEC).encode(buffer, recipe.getMultiLoopFirstStep());
        }

        @Override
        public ProceduralProcessRecipe decode(RegistryFriendlyByteBuf buffer) {
            return new ProceduralProcessRecipe(
                BlockStatePredicate.STREAM_CODEC.decode(buffer),
                ProceduralProcessStep.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer),
                ChanceBlockState.STREAM_CODEC.decode(buffer),
                ItemStack.STREAM_CODEC.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.optional(Identifier.STREAM_CODEC).decode(buffer),
                ByteBufCodecs.optional(ProceduralProcessStep.STREAM_CODEC).decode(buffer)
            );
        }
    };

    public static final RecipeSerializer<ProceduralProcessRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
