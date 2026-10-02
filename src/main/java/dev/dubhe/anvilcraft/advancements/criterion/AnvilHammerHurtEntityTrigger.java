package dev.dubhe.anvilcraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Optional;

public class AnvilHammerHurtEntityTrigger extends SimpleCriterionTrigger<AnvilHammerHurtEntityTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, Float damage) {
        this.trigger(player, damage, Items.AIR);
    }

    public void trigger(ServerPlayer player, Float damage, Item item) {
        this.trigger(player, (instance) -> instance.matches(damage, item));
    }

    public record TriggerInstance(
        Optional<ContextAwarePredicate> player,
        Optional<Float> damage,
        Optional<ItemPredicate> item
    ) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            Codec.FLOAT.optionalFieldOf("damage").forGetter(TriggerInstance::damage),
            ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::item)
        ).apply(instance, TriggerInstance::new));

        public TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Float> damage) {
            this(player, damage, Optional.empty());
        }

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

        public static Criterion<TriggerInstance> hurtEntity(HolderGetter<Item> items, float damage, Item item) {
            return ModCriterionTriggers.ANVIL_HAMMER_HURT_ENTITY.get().createCriterion(
                new TriggerInstance(
                    Optional.empty(), Optional.of(damage), Optional.of(ItemPredicate.Builder.item().of(items, item).build()))
            );
        }

        public boolean matches(Float damage) {
            return this.matches(damage, Items.AIR);
        }

        public boolean matches(Float damage, Item item) {
            return (this.damage.isEmpty() || damage >= this.damage.get())
                && (this.item.isEmpty() || this.item.get().test(item.getDefaultInstance()));
        }
    }
}
