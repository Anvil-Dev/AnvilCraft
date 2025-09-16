package dev.dubhe.anvilcraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class MineralFountainCreateTrigger extends SimpleCriterionTrigger<MineralFountainCreateTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, TriggerInstance::matches);
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)
        ).apply(ins, TriggerInstance::new));

        public static Criterion<TriggerInstance> create() {
            return ModCriterionTriggers.MINERAL_FOUNTAIN_CREATE.get().createCriterion(new TriggerInstance(Optional.empty()));
        }

        public boolean matches() {
            return true;
        }
    }
}
