package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;

@Getter
@Setter
public class ProceduralProcessStep {
    public int stepIndex;
    public final Recipe<?> content;

    public ProceduralProcessStep(int stepIndex, Recipe<?> content) {
        this.stepIndex = stepIndex;
        this.content = content;
    }

    public static Codec<ProceduralProcessStep> CODEC =
        RecordCodecBuilder.create(ins -> ins.group(
                Codec.INT.fieldOf("index").forGetter(ProceduralProcessStep::getStepIndex),
                AbstractProcessRecipe.CODEC.fieldOf("content").forGetter(ProceduralProcessStep::getContent)
            )
            .apply(ins, ProceduralProcessStep::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ProceduralProcessStep> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ProceduralProcessStep::getStepIndex,
            AbstractProcessRecipe.STREAM_CODEC,
            ProceduralProcessStep::getContent,
            ProceduralProcessStep::new
        );


}
