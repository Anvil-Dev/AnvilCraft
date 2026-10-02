package dev.dubhe.anvilcraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class CelestialEvolutionTrigger extends SimpleCriterionTrigger<CelestialEvolutionTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, int massAnvils) {
        this.trigger(player, (instance) -> instance.matches(massAnvils));
    }

    public record TriggerInstance(
        Optional<ContextAwarePredicate> player, Optional<MinMaxBounds.Ints> massAnvils
    ) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            MinMaxBounds.Ints.CODEC.optionalFieldOf("mass_anvils").forGetter(TriggerInstance::massAnvils)
        ).apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> evolved(MinMaxBounds.Ints massAnvils) {
            return ModCriterionTriggers.CELESTIAL_EVOLUTION.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.of(massAnvils))
            );
        }

        public boolean matches(int massAnvils) {
            return this.massAnvils.isEmpty() || this.massAnvils.get().matches(massAnvils);
        }
    }
}
