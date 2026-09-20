package dev.dubhe.anvilcraft.api.amulet.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/// 在护符启用时给予佩戴者属性修饰符的护符效果
public record AttributeAmuletEffect(
    Holder<Attribute> attribute,
    ResourceLocation id,
    double amount,
    AttributeModifier.Operation operation
) implements IAmuletEffect {
    @Override
    public void trigger(Player player, ItemStack amulet, AmuletEffectContext ctx) {
        Optional<Boolean> enabled = ctx.get(ModAmuletEffectContextKeys.ENABLED);
        if (enabled.isEmpty()) {
            return;
        }
        AttributeInstance instance = player.getAttribute(this.attribute);
        if (instance == null) {
            return;
        }
        if (enabled.get()) {
            if (!instance.hasModifier(this.id)) {
                instance.addTransientModifier(new AttributeModifier(this.id, this.amount, this.operation));
            }
        } else {
            instance.removeModifier(this.id);
        }
    }

    @Override
    public Type getType() {
        return ModAmuletEffectTypes.ATTRIBUTE.get();
    }

    public static class Type implements IAmuletEffect.Type<AttributeAmuletEffect> {
        public static final MapCodec<AttributeAmuletEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            BuiltInRegistries.ATTRIBUTE
                .holderByNameCodec()
                .fieldOf("attribute")
                .forGetter(AttributeAmuletEffect::attribute),
            ResourceLocation.CODEC
                .fieldOf("id")
                .forGetter(AttributeAmuletEffect::id),
            Codec.DOUBLE
                .fieldOf("amount")
                .forGetter(AttributeAmuletEffect::amount),
            AttributeModifier.Operation.CODEC
                .fieldOf("operation")
                .forGetter(AttributeAmuletEffect::operation)
        ).apply(inst, AttributeAmuletEffect::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, AttributeAmuletEffect> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(Registries.ATTRIBUTE),
            AttributeAmuletEffect::attribute,
            ResourceLocation.STREAM_CODEC,
            AttributeAmuletEffect::id,
            ByteBufCodecs.DOUBLE,
            AttributeAmuletEffect::amount,
            AttributeModifier.Operation.STREAM_CODEC,
            AttributeAmuletEffect::operation,
            AttributeAmuletEffect::new
        );

        @Override
        public MapCodec<AttributeAmuletEffect> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AttributeAmuletEffect> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
