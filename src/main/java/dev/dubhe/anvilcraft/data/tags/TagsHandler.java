package dev.dubhe.anvilcraft.data.tags;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumTagsProvider;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public class TagsHandler {
    public static void initItem(RegistrumTagsProvider<Item> provider) {
        ItemTagLoader.init(provider);
    }

    public static void initBlock(RegistrumTagsProvider<Block> provider) {
        BlockTagLoader.init(provider);
    }

    public static void initFluid(RegistrumTagsProvider<Fluid> provider) {
        FluidTagLoader.init(provider);
    }

    public static void initEnchantment(RegistrumTagsProvider<Enchantment> provider) {
        EnchantmentTagLoader.init(provider);
    }

    public static void initDamageType(RegistrumTagsProvider<DamageType> provider) {
        DamageTypeTagLoader.init(provider);
    }

    public static void initEntityType(RegistrumTagsProvider<EntityType<?>> provider) {
        EntityTypeTagLoader.init(provider);
    }
}
