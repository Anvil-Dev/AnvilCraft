package dev.dubhe.anvilcraft.api.recipe.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.api.recipe.slot.RecipeInputSlot;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModCustomDataComponents;
import dev.dubhe.anvilcraft.item.property.component.Multiphase;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class MultiphaseData implements ICustomDataComponent<Multiphase> {
    private static final String TYPE = "two";

    public static MultiphaseData two() {
        return new MultiphaseData();
    }

    private static MultiphaseData fromType(String type) {
        if (!TYPE.equals(type)) {
            throw new IllegalArgumentException("Invalid multiphase input type: " + type);
        }
        return new MultiphaseData();
    }

    @Override
    public DataComponentType<Multiphase> getDataComponentType() {
        return ModComponents.MULTIPHASE;
    }

    @Override
    public Type getType() {
        return ModCustomDataComponents.MULTIPHASE.get();
    }

    @Override
    public Multiphase make(ResultContext ctx) {
        return new Multiphase(
            List.of(
                Multiphase.Phase.fromInput(ctx.getInput(RecipeInputSlot.input(0))),
                Multiphase.Phase.fromInput(ctx.getInput(RecipeInputSlot.input(1)))
            ),
            0
        );
    }

    @Override
    public void applyToStack(ItemStack stack, @Nullable Multiphase value) {
        if (value == null) {
            stack.remove(ModComponents.MULTIPHASE);
            return;
        }
        value.initialize(stack);
    }

    @Override
    public Multiphase merge(Multiphase oldData, Multiphase newData) {
        return newData;
    }

    private String type() {
        return TYPE;
    }

    public static class Type implements ICustomDataComponent.Type<MultiphaseData> {
        public static final MapCodec<MultiphaseData> CODEC = Codec.STRING
            .fieldOf("input_type")
            .xmap(MultiphaseData::fromType, MultiphaseData::type);
        public static final StreamCodec<RegistryFriendlyByteBuf, MultiphaseData> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .map(MultiphaseData::fromType, MultiphaseData::type)
            .cast();

        @Override
        public MapCodec<MultiphaseData> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MultiphaseData> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
