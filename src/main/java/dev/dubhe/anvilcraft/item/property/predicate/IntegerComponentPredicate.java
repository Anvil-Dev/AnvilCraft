package dev.dubhe.anvilcraft.item.property.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.item.property.IIntegerComponent;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;

public record IntegerComponentPredicate(
    DataComponentType<? extends IIntegerComponent> component,
    int exact
) implements DataComponentPredicate {
    public static final MapCodec<IntegerComponentPredicate> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Codec.INT
            .fieldOf("value")
            .forGetter(IntegerComponentPredicate::exact),
        DataComponentType.CODEC
            .fieldOf("type")
            .forGetter(IntegerComponentPredicate::component)
    ).apply(inst, IntegerComponentPredicate::new));

    private IntegerComponentPredicate(int exact, DataComponentType<?> type) {
        this(Util.cast(type), exact);
    }

    @Override
    public boolean matches(DataComponentGetter components) {
        return components.has(this.component) && components.get(this.component).value() == this.exact;
    }
}
