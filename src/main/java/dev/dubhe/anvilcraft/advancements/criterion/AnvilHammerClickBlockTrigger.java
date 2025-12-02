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

public class AnvilHammerClickBlockTrigger extends SimpleCriterionTrigger<AnvilHammerClickBlockTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String type) {
        this.trigger(player, (instance) -> instance.matches(type));
    }

    public record TriggerInstance(
        Optional<ContextAwarePredicate> player,
        Optional<String> type
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            Codec.STRING.optionalFieldOf("type").forGetter(TriggerInstance::type)
        ).apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> leftClickBlock() {
            return ModCriterionTriggers.ANVIL_HAMMER_CLICK_BLOCK.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.of("left_click"))
            );
        }

        public static Criterion<TriggerInstance> rightClickBlock() {
            return ModCriterionTriggers.ANVIL_HAMMER_CLICK_BLOCK.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.of("right_click"))
            );
        }

        public static Criterion<TriggerInstance> shiftRightClickBlock() {
            return ModCriterionTriggers.ANVIL_HAMMER_CLICK_BLOCK.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.of("shift_right_click"))
            );
        }

        public boolean matches(String type) {
            return this.type.isPresent() && this.type.get().equals(type);
        }
    }
}
