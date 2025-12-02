package dev.dubhe.anvilcraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class AnvilHammerHurtEntityTrigger extends SimpleCriterionTrigger<AnvilHammerHurtEntityTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, Float damage) {
        this.trigger(player, (instance) -> instance.matches(damage));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Float> damage) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            Codec.FLOAT.optionalFieldOf("damage").forGetter(TriggerInstance::damage)
        ).apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> hurtEntity() {
            return ModCriterionTriggers.ANVIL_HAMMER_HURT_ENTITY.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.empty())
            );
        }

        public static Criterion<TriggerInstance> hurtEntity(float damage) {
            return ModCriterionTriggers.ANVIL_HAMMER_HURT_ENTITY.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.of(damage))
            );
        }

        public boolean matches(Float damage) {
            if (damage().isPresent()) {
                return damage >= this.damage.get();
            } else {
                return true;
            }
        }
    }
}
