package dev.dubhe.anvilcraft.data.tags;

import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.dubhe.anvilcraft.init.ModEntityTypeTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class EntityTypeTagLoader {
    @SuppressWarnings("deprecation")
    private static ResourceKey<EntityType<?>> findResourceKey(EntityType<?> entityType) {
        return entityType.builtInRegistryHolder().key();
    }

    /**
     * 初始化实体类型标签
     *
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateTagsProvider<EntityType<?>> provider) {
        provider.addTag(ModEntityTypeTags.AMULET_VALID)
            .addOptionalTag(ModEntityTypeTags.EMERALD_AMULET_VALID)
            .addOptionalTag(ModEntityTypeTags.SAPPHIRE_AMULET_VALID)
            .addOptionalTag(ModEntityTypeTags.CAT_AMULET_VALID)
            .addOptionalTag(ModEntityTypeTags.DOG_AMULET_VALID)
            .addOptionalTag(ModEntityTypeTags.SILENCE_AMULET_VALID);

        provider.addTag(ModEntityTypeTags.EMERALD_AMULET_VALID)
            .add(findResourceKey(EntityType.IRON_GOLEM))
            .addTag(EntityTypeTags.ILLAGER);

        provider.addTag(ModEntityTypeTags.SAPPHIRE_AMULET_VALID)
            .add(findResourceKey(EntityType.GUARDIAN))
            .add(findResourceKey(EntityType.ELDER_GUARDIAN));

        provider.addTag(ModEntityTypeTags.CAT_AMULET_VALID)
            .add(findResourceKey(EntityType.CREEPER))
            .add(findResourceKey(EntityType.PHANTOM));

        provider.addTag(ModEntityTypeTags.DOG_AMULET_VALID)
            .addTag(EntityTypeTags.SKELETONS);

        provider.addTag(ModEntityTypeTags.SILENCE_AMULET_VALID)
            .add(findResourceKey(EntityType.WARDEN));
    }
}
