package dev.dubhe.anvilcraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class HeatCollectorTrigger extends SimpleCriterionTrigger<HeatCollectorTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, BlockState state, @Nullable BlockEntity entity, int output) {
        super.trigger(player, t -> t.matches(player.level(), state, entity, output));
    }

    public record TriggerInstance(
        Optional<ContextAwarePredicate> player,
        Optional<BlockStatePredicate> collecting,
        Optional<MinMaxBounds.Ints> output
    ) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            BlockStatePredicate.CODEC.optionalFieldOf("collecting").forGetter(TriggerInstance::collecting),
            MinMaxBounds.Ints.CODEC.optionalFieldOf("output").forGetter(TriggerInstance::output)
        ).apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> collectOn(BlockStatePredicate.Builder builder) {
            return ModCriterionTriggers.HEAT_COLLECTOR_COLLECT.get().createCriterion(new TriggerInstance(
                Optional.empty(),
                Optional.of(builder.build()),
                Optional.empty()
            ));
        }

        public static Criterion<TriggerInstance> collectOn(BlockStatePredicate statePredicate) {
            return ModCriterionTriggers.HEAT_COLLECTOR_COLLECT.get().createCriterion(new TriggerInstance(
                Optional.empty(),
                Optional.of(statePredicate),
                Optional.empty()
            ));
        }

        public static Criterion<TriggerInstance> output(MinMaxBounds.Ints output) {
            return ModCriterionTriggers.HEAT_COLLECTOR_COLLECT.get().createCriterion(new TriggerInstance(
                Optional.empty(),
                Optional.empty(),
                Optional.of(output)
            ));
        }

        public boolean matches(Level level, BlockState state, @Nullable BlockEntity entity, int output) {
            if (this.collecting.isPresent() && !this.collecting.get().test(level, state, entity)) {
                return false;
            } else {
                return this.output.isEmpty() || this.output.get().matches(output);
            }
        }
    }
}
