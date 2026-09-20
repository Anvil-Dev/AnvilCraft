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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/// 交互时驯服指定动物的护符效果
public record TameAnimalAmuletEffect(List<EntityTypePredicate> animals) implements IAmuletEffect {
    @Override
    public void trigger(Player player, ItemStack amulet, AmuletEffectContext ctx) {
        Entity target = ctx.get(ModAmuletEffectContextKeys.INTERACT_TARGET).orElse(null);
        if (!(target instanceof TamableAnimal animal) || animal.isTame()) {
            return;
        }
        boolean matched = false;
        for (EntityTypePredicate predicate : this.animals) {
            if (predicate.matches(animal.getType())) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            return;
        }
        ctx.set(ModAmuletEffectContextKeys.HANDLE_INTERACT, true);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (animal instanceof Wolf wolf) {
            wolf.stopBeingAngry();
        }
        animal.tame(serverPlayer);
        animal.getNavigation().stop();
        animal.setTarget(null);
        animal.setOrderedToSit(true);
        serverPlayer.level().broadcastEntityEvent(animal, EntityEvent.TAMING_SUCCEEDED);
    }

    @Override
    public Type getType() {
        return ModAmuletEffectTypes.TAME_ANIMAL.get();
    }

    public static class Type implements IAmuletEffect.Type<TameAnimalAmuletEffect> {
        public static final MapCodec<TameAnimalAmuletEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            EntityTypePredicate.CODEC
                .listOf()
                .fieldOf("animals")
                .forGetter(TameAnimalAmuletEffect::animals)
        ).apply(inst, TameAnimalAmuletEffect::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, TameAnimalAmuletEffect> STREAM_CODEC = StreamCodec.composite(
            StreamCodecUtil.ENTITY_TYPE_PREDICATE.apply(ByteBufCodecs.list()),
            TameAnimalAmuletEffect::animals,
            TameAnimalAmuletEffect::new
        );

        @Override
        public MapCodec<TameAnimalAmuletEffect> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TameAnimalAmuletEffect> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
