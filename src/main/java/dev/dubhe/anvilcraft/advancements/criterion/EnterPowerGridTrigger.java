package dev.dubhe.anvilcraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import dev.dubhe.anvilcraft.init.ModStats;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

import java.util.Optional;

public class EnterPowerGridTrigger extends SimpleCriterionTrigger<EnterPowerGridTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> instance.matches(player));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)
        ).apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> enter() {
            return ModCriterionTriggers.ENTER_POWER_GRID.get().createCriterion(new TriggerInstance(Optional.empty()));
        }

        /// 进入电网、放置电网组件、获得电网组件三者任一不为零都算达成「感受电网律动」。
        public boolean matches(ServerPlayer player) {
            return player.getStats().getValue(Stats.CUSTOM, ModStats.ENTER_POWER_GRID) > 0
                   || player.getStats().getValue(Stats.CUSTOM, ModStats.PLACE_POWER_COMPONENT) > 0
                   || player.getStats().getValue(Stats.CUSTOM, ModStats.OBTAIN_POWER_COMPONENT) > 0;
        }
    }
}
