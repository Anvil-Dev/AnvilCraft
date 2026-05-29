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
    /**
     * 这个步骤所在的Procedural Process配方的ResourceLocation，会在加载时赋值
     */
    public ResourceLocation ppRecipeId;

    /**
     * 这个步骤的步数，会在加载时重新赋值
     */
    public int stepIndex;

    /**
     * 这个step的内容，它应当是AbstractProcessRecipe
     * （尽管数据包作者可能会输入任何东西）
     */
    public final Recipe<?> content;

    /**
     * 序列装配配方步骤ProceduralProcessStep的构造器
     * 如果输入的步骤内容不是应当是AbstractProcessRecipe，在后续的注册和调用中会被忽略并警告
     *
     * @param stepIndex 步骤编号
     * @param content 步骤的内容，应当是AbstractProcessRecipe铁砧处理配方
     */
    public ProceduralProcessStep(int stepIndex, Recipe<?> content) {
        this.stepIndex = stepIndex;
        this.content = content;
    }

    /**
     * 带ppRecipeId的序列装配配方步骤ProceduralProcessStep的构造器
     * 如果输入的步骤内容不是应当是AbstractProcessRecipe，在后续的注册和调用中会被忽略并警告
     *
     * @param ppRecipeId 步骤所对应的Procedural Process配方的rl
     * @param stepIndex 步骤编号
     * @param content 步骤的内容，应当是AbstractProcessRecipe铁砧处理配方
     */
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
