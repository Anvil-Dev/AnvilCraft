package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformRecipe;
import dev.dubhe.anvilcraft.recipe.transform.NumericTagValuePredicate;
import dev.dubhe.anvilcraft.recipe.transform.TagModification;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;

public class MobTransformRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        MobTransformRecipe.from(EntityType.COW).to(EntityType.RAVAGER).save(provider);
        MobTransformRecipe.from(EntityType.PIG).to(EntityType.HOGLIN).save(provider);
        MobTransformRecipe.from(EntityType.GUARDIAN)
            .to(EntityType.ELDER_GUARDIAN)
            .save(provider);
        MobTransformRecipe.from(EntityType.PIGLIN).to(EntityType.PIGLIN_BRUTE).save(provider);
        MobTransformRecipe.from(EntityType.VILLAGER)
            .result(EntityType.PILLAGER, 0.3)
            .result(EntityType.VINDICATOR, 0.6)
            .result(EntityType.EVOKER, 0.1)
            .save(provider);
        MobTransformRecipe.from(EntityType.ALLAY).to(EntityType.VEX).save(provider);
        MobTransformRecipe.from(EntityType.BAT).to(EntityType.PHANTOM).save(provider);
        MobTransformRecipe.from(EntityType.HORSE)
            .result(EntityType.SKELETON_HORSE, 0.9)
            .result(EntityType.ZOMBIE_HORSE, 0.1)
            .save(provider);
        MobTransformRecipe.from(EntityType.IRON_GOLEM)
            .to(EntityType.WARDEN)
            .predicate(b -> b.compare(NumericTagValuePredicate.ValueFunction.EQUAL)
                .lhs("PlayerCreated")
                .rhs(0))
            .tagModification(b -> {
                CompoundTag tag = new CompoundTag();
                CompoundTag cooldownMemoryTag = new CompoundTag();
                cooldownMemoryTag.put("value", new CompoundTag());
                cooldownMemoryTag.putLong("ttl", 1200L);
                tag.put("minecraft:dig_cooldown", cooldownMemoryTag);
                b.path("Brain.memories")
                    .operation(TagModification.ModifyOperation.SET)
                    .value(tag);
            })
            .save(provider);
    }
}
