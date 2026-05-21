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
import net.minecraft.world.level.ItemLike;

import java.util.Optional;

public class UseItemTrigger extends SimpleCriterionTrigger<UseItemTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, Item item) {
        this.trigger(player, instance -> instance.matches(item));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            EntityPredicate.ADVANCEMENT_CODEC
                .optionalFieldOf("player")
                .forGetter(TriggerInstance::player),
            ItemPredicate.CODEC
                .optionalFieldOf("item")
                .forGetter(TriggerInstance::item)
        ).apply(inst, TriggerInstance::new));

        public static Criterion<TriggerInstance> useItem(HolderGetter<Item> items, ItemLike item) {
            return useItem(ItemPredicate.Builder.item().of(items, item));
        }

        public static Criterion<TriggerInstance> useItem(ItemPredicate.Builder item) {
            return ModCriterionTriggers.USE_ITEM.get().createCriterion(new TriggerInstance(Optional.empty(), Optional.of(item.build())));
        }

        public boolean matches(Item item) {
            return this.item.isEmpty() || this.item.get().test(item.getDefaultInstance());
        }
    }
}
