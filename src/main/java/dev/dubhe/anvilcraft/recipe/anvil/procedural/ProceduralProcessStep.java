package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

    public static final Codec<ProceduralProcessStep> CODEC =
        RecordCodecBuilder.create(ins -> ins.group(
                Codec.INT.fieldOf("index").forGetter(ProceduralProcessStep::getStepIndex),
                Recipe.CODEC.fieldOf("content").forGetter(ProceduralProcessStep::getContent)
            )
            .apply(ins, (index, recipe) -> new ProceduralProcessStep(index, recipe))
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ProceduralProcessStep> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ProceduralProcessStep::getStepIndex,
            Recipe.STREAM_CODEC,
            ProceduralProcessStep::getContent,
            (index, recipe) -> new ProceduralProcessStep(index, recipe)
        );
}
