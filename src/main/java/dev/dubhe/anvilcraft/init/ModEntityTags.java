package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class ModEntityTags {
    public static final TagKey<EntityType<?>> ITEM_CACHE = TagKey.create(Registries.ENTITY_TYPE, AnvilCraft.of("item_cache"));
}
