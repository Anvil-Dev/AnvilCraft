package dev.dubhe.anvilcraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class AnvilHammerChangeBlockTrigger extends SimpleCriterionTrigger<AnvilHammerChangeBlockTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, BlockState oldState, BlockState newState) {
        this.trigger(player, instance -> instance.matches(oldState, newState));
    }

    public record TriggerInstance(
        Optional<ContextAwarePredicate> player,
        Optional<BlockStatePredicate> oldState,
        Optional<BlockStatePredicate> newState
    ) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            BlockStatePredicate.CODEC.optionalFieldOf("old_state").forGetter(TriggerInstance::oldState),
            BlockStatePredicate.CODEC.optionalFieldOf("new_state").forGetter(TriggerInstance::newState)
        ).apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> change(BlockStatePredicate oldState, BlockStatePredicate newState) {
            return ModCriterionTriggers.ANVIL_HAMMER_CHANGE_BLOCK.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.of(oldState), Optional.of(newState))
            );
        }

        public static Criterion<TriggerInstance> change(BlockStatePredicate.Builder oldBuilder, BlockStatePredicate.Builder newBuilder) {
            return TriggerInstance.change(oldBuilder.build(), newBuilder.build());
        }

        public boolean matches(BlockState oldState, BlockState newState) {
            return (this.oldState.isEmpty() || this.oldState.get().testWithoutEntity(oldState))
                   && (this.newState.isEmpty() || this.newState.get().testWithoutEntity(newState));
        }
    }
}
