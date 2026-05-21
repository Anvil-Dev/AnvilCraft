package dev.dubhe.anvilcraft.data.tags;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumTagsProvider;
import dev.dubhe.anvilcraft.init.entity.ModEntityTypeTags;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;

public class EntityTypeTagLoader {
    @SuppressWarnings("deprecation")
    private static Identifier findId(EntityType<?> entityType) {
        return entityType.builtInRegistryHolder().key().identifier();
    }

    /**
     * 初始化实体类型标签
     *
     * @param provider 提供器
     */
    public static void init(RegistrumTagsProvider<EntityType<?>> provider) {
        provider.rawBuilder(ModEntityTypeTags.AMULET_VALID)
            .addOptionalTag(ModEntityTypeTags.EMERALD_AMULET_VALID.location())
            .addOptionalTag(ModEntityTypeTags.SAPPHIRE_AMULET_VALID.location())
            .addOptionalTag(ModEntityTypeTags.CAT_AMULET_VALID.location())
            .addOptionalTag(ModEntityTypeTags.DOG_AMULET_VALID.location())
            .addOptionalTag(ModEntityTypeTags.SILENCE_AMULET_VALID.location());

        provider.rawBuilder(ModEntityTypeTags.EMERALD_AMULET_VALID)
            .addElement(findId(EntityType.IRON_GOLEM))
            .addTag(EntityTypeTags.ILLAGER.location());

        provider.rawBuilder(ModEntityTypeTags.SAPPHIRE_AMULET_VALID)
            .addElement(findId(EntityType.GUARDIAN))
            .addElement(findId(EntityType.ELDER_GUARDIAN));

        provider.rawBuilder(ModEntityTypeTags.CAT_AMULET_VALID)
            .addElement(findId(EntityType.CREEPER))
            .addElement(findId(EntityType.PHANTOM));

        provider.rawBuilder(ModEntityTypeTags.DOG_AMULET_VALID)
            .addTag(EntityTypeTags.SKELETONS.location());

        provider.rawBuilder(ModEntityTypeTags.SILENCE_AMULET_VALID)
            .addElement(findId(EntityType.WARDEN));
    }
}
