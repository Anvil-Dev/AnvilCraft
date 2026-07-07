package dev.dubhe.anvilcraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import dev.dubhe.anvilcraft.init.ModStats;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class ElectricAllergyTrigger extends SimpleCriterionTrigger<ElectricAllergyTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, ItemStack changed) {
        super.trigger(player, instance -> instance.matches(player, changed));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)
        ).apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> of() {
            return ModCriterionTriggers.ELECTRIC_ALLERGY.get().createCriterion(new TriggerInstance(Optional.empty()));
        }

        public boolean matches(ServerPlayer player, ItemStack changed) {
            return changed.is(ModBlocks.TRANSCENDENCE_ANVIL.asItem())
                   && player.getStats().getValue(Stats.CUSTOM, ModStats.PLACE_POWER_COMPONENT) == 0
                   && player.getStats().getValue(Stats.CUSTOM, ModStats.ENTER_POWER_GRID) == 0;
        }
    }
}
