package dev.dubhe.anvilcraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTerminal;
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

    public void trigger(ServerPlayer player, int massAnvils, StellarTerminal.Kind terminal) {
        this.trigger(player, (instance) -> instance.matches(massAnvils, terminal));
    }

    public record TriggerInstance(
        Optional<ContextAwarePredicate> player,
        Optional<MinMaxBounds.Ints> massAnvils,
        Optional<StellarTerminal.Kind> terminal
    ) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            MinMaxBounds.Ints.CODEC.optionalFieldOf("mass_anvils").forGetter(TriggerInstance::massAnvils),
            StellarTerminal.Kind.CODEC.optionalFieldOf("terminal").forGetter(TriggerInstance::terminal)
        ).apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> evolved(MinMaxBounds.Ints massAnvils) {
            return ModCriterionTriggers.CELESTIAL_EVOLUTION.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.of(massAnvils), Optional.empty())
            );
        }

        public static Criterion<TriggerInstance> evolved(StellarTerminal.Kind terminal) {
            return ModCriterionTriggers.CELESTIAL_EVOLUTION.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(terminal))
            );
        }

        public boolean matches(int massAnvils, StellarTerminal.Kind terminal) {
            if (this.massAnvils.isPresent() && !this.massAnvils.get().matches(massAnvils)) return false;
            return this.terminal.isEmpty() || this.terminal.get() == terminal;
        }
    }
}
