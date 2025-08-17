package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntityTags {
    public static final TagKey<BlockEntityType<?>> ITEM_CACHE = TagKey.create(Registries.BLOCK_ENTITY_TYPE, AnvilCraft.of("item_cache"));
}
