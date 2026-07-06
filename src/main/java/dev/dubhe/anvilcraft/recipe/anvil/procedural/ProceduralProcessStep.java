package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;

@Getter
@Setter
public class ProceduralProcessStep {
    public Identifier ppRecipeId;
    public int stepIndex;
    public final Recipe<?> content;

    public ProceduralProcessStep(int stepIndex, Recipe<?> content) {
        this.stepIndex = stepIndex;
        this.content = content;
    }

    public ProceduralProcessStep(Identifier ppRecipeId, int stepIndex, Recipe<?> content) {
        this.ppRecipeId = ppRecipeId;
        this.stepIndex = stepIndex;
        this.content = content;
    }

    @SuppressWarnings("unchecked")
    public static final Codec<ProceduralProcessStep> CODEC =
        RecordCodecBuilder.create(ins -> ins.group(
                Codec.INT.fieldOf("index").forGetter(ProceduralProcessStep::getStepIndex),
                InWorldRecipe.Serializer.CODEC.codec().fieldOf("content").forGetter(s -> (InWorldRecipe) s.getContent())
            )
            .apply(ins, (index, recipe) -> new ProceduralProcessStep(index, recipe))
    );

    @SuppressWarnings("unchecked")
    public static final StreamCodec<RegistryFriendlyByteBuf, ProceduralProcessStep> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ProceduralProcessStep::getStepIndex,
            InWorldRecipe.Serializer.STREAM_CODEC,
            s -> (InWorldRecipe) s.getContent(),
            (index, recipe) -> new ProceduralProcessStep(index, recipe)
        );
}
