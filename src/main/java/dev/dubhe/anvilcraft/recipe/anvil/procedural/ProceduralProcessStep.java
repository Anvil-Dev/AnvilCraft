package dev.dubhe.anvilcraft.recipe.anvil.procedural;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

@Getter
@Setter
public class ProceduralProcessStep {
    public ResourceLocation ppRecipeId;
    //ppRecipeId: 这个步骤所对应的Procedural Process配方的rl，在加载时赋值
    public int stepIndex;
    //stepIndex：这个步骤的步数，需要在加载时重新载入
    public final Recipe<?> content;
    //content：这个step的内容（AbstractProcessRecipe）

    public ProceduralProcessStep(int stepIndex, Recipe<?> content) {
        this.stepIndex = stepIndex;
        this.content = content;
    }

    public ProceduralProcessStep(ResourceLocation ppRecipeId, int stepIndex, Recipe<?> content) {
        this.ppRecipeId = ppRecipeId;
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
