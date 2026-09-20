package dev.dubhe.anvilcraft.api.amulet.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectTypes;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/// 使指定生物无视佩戴者的护符效果
public record IgnoreMobTargetAmuletEffect(List<EntityTypePredicate> mobs) implements IAmuletEffect {
    @Override
    public void trigger(Player player, ItemStack amulet, AmuletEffectContext ctx) {
        Optional<EntityType<?>> type = ctx.get(ModAmuletEffectContextKeys.MOB_TYPE);
        if (type.isEmpty()) {
            return;
        }
        for (EntityTypePredicate mob : this.mobs) {
            if (mob.matches(type.get())) {
                ctx.set(ModAmuletEffectContextKeys.IGNORE_MOB, true);
                return;
            }
        }
    }

    @Override
    public Type getType() {
        return ModAmuletEffectTypes.IGNORE_MOB_TARGET.get();
    }

    public static class Type implements IAmuletEffect.Type<IgnoreMobTargetAmuletEffect> {
        public static final MapCodec<IgnoreMobTargetAmuletEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            EntityTypePredicate.CODEC
                .listOf()
                .fieldOf("mobs")
                .forGetter(IgnoreMobTargetAmuletEffect::mobs)
        ).apply(inst, IgnoreMobTargetAmuletEffect::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, IgnoreMobTargetAmuletEffect> STREAM_CODEC = StreamCodec.composite(
            StreamCodecUtil.ENTITY_TYPE_PREDICATE.apply(ByteBufCodecs.list()),
            IgnoreMobTargetAmuletEffect::mobs,
            IgnoreMobTargetAmuletEffect::new
        );

        @Override
        public MapCodec<IgnoreMobTargetAmuletEffect> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, IgnoreMobTargetAmuletEffect> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
