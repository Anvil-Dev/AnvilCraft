package dev.dubhe.anvilcraft.init.item;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.def.AmuletDefinition;
import dev.dubhe.anvilcraft.api.amulet.def.IAmuletDefinition;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypeTags;
import dev.dubhe.anvilcraft.init.entity.ModEntityTypeTags;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.item.amulet.def.AbnormalAmuletDefinition;
import dev.dubhe.anvilcraft.item.amulet.def.ComradeAmuletDefinition;
import dev.dubhe.anvilcraft.predicate.FallingBlockPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.SlotRanges;

import java.util.Objects;

public class ModAmuletDefinitions {
    public static final ResourceKey<IAmuletDefinition> EMERALD = ModAmuletDefinitions.key("emerald");
    public static final ResourceKey<IAmuletDefinition> TOPAZ = ModAmuletDefinitions.key("topaz");
    public static final ResourceKey<IAmuletDefinition> RUBY = ModAmuletDefinitions.key("ruby");
    public static final ResourceKey<IAmuletDefinition> SAPPHIRE = ModAmuletDefinitions.key("sapphire");
    public static final ResourceKey<IAmuletDefinition> ANVIL = ModAmuletDefinitions.key("anvil");
    public static final ResourceKey<IAmuletDefinition> COMRADE = ModAmuletDefinitions.key("comrade");
    public static final ResourceKey<IAmuletDefinition> FEATHER = ModAmuletDefinitions.key("feather");
    public static final ResourceKey<IAmuletDefinition> CAT = ModAmuletDefinitions.key("cat");
    public static final ResourceKey<IAmuletDefinition> DOG = ModAmuletDefinitions.key("dog");
    public static final ResourceKey<IAmuletDefinition> SILENCE = ModAmuletDefinitions.key("silence");
    public static final ResourceKey<IAmuletDefinition> ABNORMAL = ModAmuletDefinitions.key("abnormal");
    public static final ResourceKey<IAmuletDefinition> GEM = ModAmuletDefinitions.key("gem");
    public static final ResourceKey<IAmuletDefinition> NATURE = ModAmuletDefinitions.key("nature");

    public static void bootstrap(BootstrapContext<IAmuletDefinition> ctx) {
        ctx.register(
            ModAmuletDefinitions.EMERALD,
            AmuletDefinition.builder(ModItems.EMERALD_AMULET)
                .obtainEntity(ModEntityTypeTags.EMERALD_AMULET_VALID)
                .build()
        );
        ctx.register(
            ModAmuletDefinitions.TOPAZ,
            AmuletDefinition.builder(ModItems.TOPAZ_AMULET)
                .obtain(ModDamageTypeTags.TOPAZ_AMULET_VALID)
                .build()
        );
        ctx.register(
            ModAmuletDefinitions.RUBY,
            AmuletDefinition.builder(ModItems.RUBY_AMULET)
                .obtain(ModDamageTypeTags.RUBY_AMULET_VALID)
                .build()
        );
        ctx.register(
            ModAmuletDefinitions.SAPPHIRE,
            AmuletDefinition.builder(ModItems.SAPPHIRE_AMULET)
                .obtain(ModDamageTypeTags.SAPPHIRE_AMULET_VALID)
                .obtainEnd()
                .obtainEntity(ModEntityTypeTags.SAPPHIRE_AMULET_VALID)
                .build()
        );
        ctx.register(
            ModAmuletDefinitions.ANVIL,
            AmuletDefinition.builder(ModItems.ANVIL_AMULET)
                .obtain(ModDamageTypeTags.ANVIL_AMULET_VALID)
                .obtainEnd()
                .obtainEntity(ModEntityTypeTags.ANVIL_AMULET_VALID)
                .obtain(new FallingBlockPredicate(BlockStatePredicate.builder().of(BlockTags.ANVIL).build()))
                .obtainEnd()
                .obtainDirect(
                    Objects.requireNonNull(SlotRanges.nameToIds("weapon")),
                    ItemPredicate.Builder.item().of(ModItemTags.ANVIL_HAMMER)
                )
                .build()
        );
        ctx.register(
            ModAmuletDefinitions.COMRADE,
            new ComradeAmuletDefinition(ModItems.COMRADE_AMULET)
        );
        ctx.register(
            ModAmuletDefinitions.FEATHER,
            AmuletDefinition.builder(ModItems.FEATHER_AMULET)
                .obtain(ModDamageTypeTags.FEATHER_AMULET_VALID)
                .build()
        );
        ctx.register(
            ModAmuletDefinitions.CAT,
            AmuletDefinition.builder(ModItems.CAT_AMULET)
                .obtainEntity(ModEntityTypeTags.CAT_AMULET_VALID)
                .build()
        );
        ctx.register(
            ModAmuletDefinitions.DOG,
            AmuletDefinition.builder(ModItems.DOG_AMULET)
                .obtainEntity(ModEntityTypeTags.DOG_AMULET_VALID)
                .build()
        );
        ctx.register(
            ModAmuletDefinitions.SILENCE,
            AmuletDefinition.builder(ModItems.SILENCE_AMULET)
                .obtainEntity(ModEntityTypeTags.SILENCE_AMULET_VALID)
                .build()
        );
        ctx.register(
            ModAmuletDefinitions.ABNORMAL,
            new AbnormalAmuletDefinition(ModItems.ABNORMAL_AMULET)
        );
        ctx.register(
            ModAmuletDefinitions.GEM,
            new AmuletDefinition(ModItems.GEM_AMULET)
        );
        ctx.register(
            ModAmuletDefinitions.NATURE,
            new AmuletDefinition(ModItems.NATURE_AMULET)
        );
    }

    private static ResourceKey<IAmuletDefinition> key(String name) {
        return ResourceKey.create(ModRegistryKeys.AMULET_DEF, AnvilCraft.of(name));
    }
}
