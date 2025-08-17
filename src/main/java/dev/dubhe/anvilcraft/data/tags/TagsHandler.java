package dev.dubhe.anvilcraft.data.tags;

import com.tterrag.registrate.providers.RegistrateTagsProvider;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public class TagsHandler {
    public static void initItem(RegistrateTagsProvider<Item> provider) {
        ItemTagLoader.init(provider);
    }

    public static void initBlock(RegistrateTagsProvider<Block> provider) {
        BlockTagLoader.init(provider);
    }

    public static void initFluid(RegistrateTagsProvider<Fluid> provider) {
        FluidTagLoader.init(provider);
    }

    public static void initEnchantment(RegistrateTagsProvider<Enchantment> provider) {
        EnchantmentTagLoader.init(provider);
    }

    public static void initDamageType(RegistrateTagsProvider<DamageType> provider) {
        DamageTypeTagLoader.init(provider);
    }

    public static void initEntityType(RegistrateTagsProvider<EntityType<?>> provider) {
        EntityTypeTagLoader.init(provider);
    }
}
