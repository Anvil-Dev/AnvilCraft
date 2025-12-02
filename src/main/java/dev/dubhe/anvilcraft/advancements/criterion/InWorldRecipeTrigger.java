package dev.dubhe.anvilcraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class InWorldRecipeTrigger extends SimpleCriterionTrigger<InWorldRecipeTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, ResourceLocation recipeType, ResourceLocation id) {
        this.trigger(player, (instance) -> instance.matches(recipeType, id));
    }

    public record TriggerInstance(
        Optional<ContextAwarePredicate> player, Optional<ResourceLocation> recipeType, Optional<ResourceLocation> id
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            ResourceLocation.CODEC.optionalFieldOf("recipeType").forGetter(TriggerInstance::recipeType),
            ResourceLocation.CODEC.optionalFieldOf("id").forGetter(TriggerInstance::id)
        ).apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> inWorldRecipe() {
            return ModCriterionTriggers.IN_WORLD_RECIPE.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty())
            );
        }

        public static Criterion<TriggerInstance> inWorldRecipe(ResourceLocation id) {
            return ModCriterionTriggers.IN_WORLD_RECIPE.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(id))
            );
        }

        public static Criterion<TriggerInstance> inWorldRecipeType(ResourceLocation recipeType) {
            return ModCriterionTriggers.IN_WORLD_RECIPE.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.of(recipeType), Optional.empty())
            );
        }

        public boolean matches(ResourceLocation recipeType, ResourceLocation id) {
            if (this.recipeType.isEmpty() && this.id.isEmpty()) {
                return true;
            }
            if (this.recipeType.isPresent()) {
                return this.recipeType.map(resourceLocation -> resourceLocation.equals(recipeType)).orElse(false);
            }
            return this.id.map(resourceLocation -> resourceLocation.equals(id)).orElse(false);
        }
    }
}
