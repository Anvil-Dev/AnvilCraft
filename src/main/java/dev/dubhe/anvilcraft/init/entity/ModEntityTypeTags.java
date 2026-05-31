package dev.dubhe.anvilcraft.init.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class ModEntityTypeTags {
    public static final TagKey<EntityType<?>> AMULET_VALID = bind("amulet_valid");
    public static final TagKey<EntityType<?>> EMERALD_AMULET_VALID = bind("amulet_valid/emerald");
    public static final TagKey<EntityType<?>> SAPPHIRE_AMULET_VALID = bind("amulet_valid/sapphire");
    public static final TagKey<EntityType<?>> ANVIL_AMULET_VALID = bind("amulet_valid/anvil");
    public static final TagKey<EntityType<?>> CAT_AMULET_VALID = bind("amulet_valid/cat");
    public static final TagKey<EntityType<?>> DOG_AMULET_VALID = bind("amulet_valid/dog");
    public static final TagKey<EntityType<?>> SILENCE_AMULET_VALID = bind("amulet_valid/silence");

    public static final TagKey<EntityType<?>> FALLING_GIANT_ANVIL_DAMAGE_IMMUNE = bind("falling_giant_anvil_damage_immune");

    @SuppressWarnings("unused")
    private static TagKey<EntityType<?>> bindC(String id) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", id));
    }

    private static TagKey<EntityType<?>> bind(String id) {
        return TagKey.create(Registries.ENTITY_TYPE, AnvilCraft.of(id));
    }
}
