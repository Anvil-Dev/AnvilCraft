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
                    BlockStatePredicate.CODEC.fieldOf("initial_block").forGetter(ProceduralProcessRecipe::initialBlock),
                    ProceduralProcessStep.CODEC.listOf().fieldOf("steps").forGetter(ProceduralProcessRecipe::steps),
                    ChanceBlockState.CODEC.fieldOf("result_block").forGetter(ProceduralProcessRecipe::resultBlock),
                    ItemStackTemplate.CODEC.optionalFieldOf("icon")
                        .forGetter(ProceduralProcessRecipe::icon),
                    Codec.INT.fieldOf("loop").forGetter(ProceduralProcessRecipe::loop),
                    Identifier.CODEC.optionalFieldOf("displayed_model").forGetter(ProceduralProcessRecipe::displayedModel),
                    Identifier.CODEC
                        .listOf()
                        .optionalFieldOf("displayed_models", List.of())
                        .forGetter(ProceduralProcessRecipe::displayedModels),
                    ProceduralProcessStep.CODEC
                        .optionalFieldOf("multiple_loop_first_step")
                        .forGetter(ProceduralProcessRecipe::multiLoopFirstStep)
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
            BlockStatePredicate.STREAM_CODEC.encode(buffer, recipe.initialBlock());
            ProceduralProcessStep.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, recipe.steps());
            ChanceBlockState.STREAM_CODEC.encode(buffer, recipe.resultBlock());
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).encode(buffer, recipe.icon());
            buffer.writeVarInt(recipe.loop());
            ByteBufCodecs.optional(Identifier.STREAM_CODEC).encode(buffer, recipe.displayedModel());
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, recipe.displayedModels());
            ByteBufCodecs.optional(ProceduralProcessStep.STREAM_CODEC).encode(buffer, recipe.multiLoopFirstStep());
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

    public static final RecipeSerializer<ProceduralProcessRecipe> INSTANCE = new RecipeSerializer<>(
        ProceduralProcessSerializer.CODEC,
        ProceduralProcessSerializer.STREAM_CODEC
    );
}
