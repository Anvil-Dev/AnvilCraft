package dev.dubhe.anvilcraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.Optional;

public class AnvilLootingTrigger extends SimpleCriterionTrigger<AnvilLootingTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, Entity entity) {
        LootContext context = EntityPredicate.createContext(player, entity);
        this.trigger(player, (instance) -> instance.matches(context));
    }

    public record TriggerInstance(
        Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity
    ) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(TriggerInstance::entity)
        ).apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> looting() {
            return ModCriterionTriggers.ANVIL_LOOTING.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.empty())
            );
        }

        public static Criterion<TriggerInstance> looting(EntityType<?> entityType) {
            return ModCriterionTriggers.ANVIL_LOOTING.get().createCriterion(
                new TriggerInstance(
                    Optional.empty(),
                    Optional.of(EntityPredicate.wrap(EntityPredicate.Builder.entity().of(entityType).build())))
            );
        }

        public boolean matches(LootContext lootContext) {
            return this.entity.map(contextAwarePredicate -> contextAwarePredicate.matches(lootContext)).orElse(true);
        }
    }
}
