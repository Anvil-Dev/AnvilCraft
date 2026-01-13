package dev.dubhe.anvilcraft.api.recipe.slot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

@Getter
@EqualsAndHashCode
public class RecipeInputSlot implements StringRepresentable {
    public static final MapCodec<RecipeInputSlot> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.STRING
            .fieldOf("input")
            .forGetter(RecipeInputSlot::getSerializedName)
    ).apply(ins, RecipeInputSlot::new));
    public static final StreamCodec<ByteBuf, RecipeInputSlot> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        RecipeInputSlot::getSerializedName,
        RecipeInputSlot::new
    );
    private final String serializedName;

    public RecipeInputSlot(String serializedName) {
        this.serializedName = serializedName;
    }

    // 实例&构建
    public static final RecipeInputSlot TEMPLATE = new RecipeInputSlot("template");
    public static final RecipeInputSlot MATERIAL = new RecipeInputSlot("material");

    public static RecipeInputSlot input(int index) {
        return new RecipeInputSlot("input." + index);
    }
}
