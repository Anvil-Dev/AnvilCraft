package dev.dubhe.anvilcraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * 锁定第 N 种新的天体类型时触发，count 为该判据要求累计的天体类别数。
 */
public class CelestialVisitTrigger extends SimpleCriterionTrigger<CelestialVisitTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, int count) {
        this.trigger(player, (instance) -> instance.matches(count));
    }

    public record TriggerInstance(
        Optional<ContextAwarePredicate> player, Optional<Integer> count
    ) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            Codec.INT.optionalFieldOf("count").forGetter(TriggerInstance::count)
        ).apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> visited() {
            return ModCriterionTriggers.CELESTIAL_VISIT.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.empty())
            );
        }

        public static Criterion<TriggerInstance> visited(int count) {
            return ModCriterionTriggers.CELESTIAL_VISIT.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.of(count))
            );
        }

        public boolean matches(int count) {
            return this.count.isEmpty() || count >= this.count.get();
        }
    }
}
