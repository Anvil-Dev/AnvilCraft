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
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.List;

public class ProceduralProcessSerializer {

    public static final MapCodec<ProceduralProcessRecipe> CODEC =
        RecordCodecBuilder.mapCodec(ins -> ins.group(
                    BlockStatePredicate.CODEC.fieldOf("initial_block").forGetter(ProceduralProcessRecipe::getInitialBlock),
                    ProceduralProcessStep.CODEC.listOf().fieldOf("steps").forGetter(ProceduralProcessRecipe::getSteps),
                    ChanceBlockState.CODEC.fieldOf("result_block").forGetter(ProceduralProcessRecipe::getResultBlock),
                    ItemStackTemplate.CODEC.optionalFieldOf("icon")
                        .forGetter(ProceduralProcessRecipe::getIcon),
                    Codec.INT.fieldOf("loop").forGetter(ProceduralProcessRecipe::getLoop),
                    Identifier.CODEC.optionalFieldOf("displayed_model").forGetter(ProceduralProcessRecipe::getDisplayedModel),
                    Identifier.CODEC
                        .listOf()
                        .optionalFieldOf("displayed_models", List.of())
                        .forGetter(ProceduralProcessRecipe::getDisplayedModels),
                    ProceduralProcessStep.CODEC
                        .optionalFieldOf("multiple_loop_first_step")
                        .forGetter(ProceduralProcessRecipe::getMultiLoopFirstStep)
                )
                .apply(
                    ins, (initialBlock, steps, resultBlock, icon, loop, displayedModel, displayedModels, multiLoopFirstStep) ->
                        new ProceduralProcessRecipe(
                            initialBlock,
                            steps,
                            resultBlock,
                            icon,
                            loop,
                            displayedModel,
                            displayedModels,
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
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).encode(buffer, recipe.getIcon());
            buffer.writeVarInt(recipe.getLoop());
            ByteBufCodecs.optional(Identifier.STREAM_CODEC).encode(buffer, recipe.getDisplayedModel());
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, recipe.getDisplayedModels());
            ByteBufCodecs.optional(ProceduralProcessStep.STREAM_CODEC).encode(buffer, recipe.getMultiLoopFirstStep());
        }

        @Override
        public ProceduralProcessRecipe decode(RegistryFriendlyByteBuf buffer) {
            return new ProceduralProcessRecipe(
                BlockStatePredicate.STREAM_CODEC.decode(buffer),
                ProceduralProcessStep.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer),
                ChanceBlockState.STREAM_CODEC.decode(buffer),
                ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.optional(Identifier.STREAM_CODEC).decode(buffer),
                Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer),
                ByteBufCodecs.optional(ProceduralProcessStep.STREAM_CODEC).decode(buffer)
            );
        }
    };

    public static final RecipeSerializer<ProceduralProcessRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
