package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class ModBlockTags {
    public static final TagKey<Block> UNDER_CAULDRON = bind("under_cauldron");
    public static final TagKey<Block> MAGNET = bind("magnet");
    public static final TagKey<Block> REDSTONE_TORCH = bind("redstone_torch");
    public static final TagKey<Block> MUSHROOM_BLOCK = bind("mushroom_block");
    public static final TagKey<Block> CANT_BROKEN_ANVIL = bind("cant_broken_anvil");
    public static final TagKey<Block> HAMMER_REMOVABLE = bind("hammer_removable");
    public static final TagKey<Block> HAMMER_CHANGEABLE = bind("hammer_changeable");
    public static final TagKey<Block> OVERSEER_BASE = bind("overseer_base");
    public static final TagKey<Block> BLOCK_DEVOURER_PROBABILITY_DROPPING = bind("block_devourer_probability_dropping");
    public static final TagKey<Block> DEEPSLATE_METAL = bindC("deepslate_metal");
    public static final TagKey<Block> LASE_CAN_PASS_THROUGH = bind("lase_can_pass_though");
    public static final TagKey<Block> GLASS_BLOCKS = bindC("glass_blocks");
    public static final TagKey<Block> GLASS_PANES = bindC("glass_panes");
    public static final TagKey<Block> ORES = bindC("ores");
    public static final TagKey<Block> ORES_IN_GROUND_NETHERRACK = bindC("ores_in_ground/netherrack");
    public static final TagKey<Block> ORES_IN_GROUND_DEEPSLATE = bindC("ores_in_ground/deepslate");
    public static final TagKey<Block> FORGE_GLASS_BLOCKS = bindForge("glass");
    public static final TagKey<Block> FORGE_GLASS_PANES = bindForge("glass_panes");
    public static final TagKey<Block> FORGE_ORES = bindForge("ores");
    public static final TagKey<Block> FORGE_ORES_IN_GROUND_NETHERRACK = bindForge("ores_in_ground/netherrack");
    public static final TagKey<Block> FORGE_ORES_IN_GROUND_DEEPSLATE = bindForge("ores_in_ground/deepslate");

    // 矿物块tag
    public static final TagKey<Block> TUNGSTEN_BLOCKS = bindC("tungsten_blocks");
    public static final TagKey<Block> TITANIUM_BLOCKS = bindC("titanium_blocks");
    public static final TagKey<Block> ZINC_BLOCKS = bindC("zinc_blocks");
    public static final TagKey<Block> TIN_BLOCKS = bindC("tin_blocks");
    public static final TagKey<Block> LEAD_BLOCKS = bindC("lead_blocks");
    public static final TagKey<Block> SILVER_BLOCKS = bindC("silver_blocks");
    public static final TagKey<Block> URANIUM_BLOCKS = bindC("uranium_blocks");
    public static final TagKey<Block> BRONZE_BLOCKS = bindC("bronze_blocks");
    public static final TagKey<Block> BRASS_BLOCKS = bindC("brass_blocks");

    public static final TagKey<Block> FORGE_TUNGSTEN_BLOCKS = bindForge("storage_blocks/tungsten_blocks");
    public static final TagKey<Block> FORGE_TITANIUM_BLOCKS = bindForge("storage_blocks/titanium_blocks");
    public static final TagKey<Block> FORGE_ZINC_BLOCKS = bindForge("storage_blocks/zinc_blocks");
    public static final TagKey<Block> FORGE_TIN_BLOCKS = bindForge("storage_blocks/tin_blocks");
    public static final TagKey<Block> FORGE_LEAD_BLOCKS = bindForge("storage_blocks/lead_blocks");
    public static final TagKey<Block> FORGE_SILVER_BLOCKS = bindForge("storage_blocks/silver_blocks");
    public static final TagKey<Block> FORGE_URANIUM_BLOCKS = bindForge("storage_blocks/uranium_blocks");
    public static final TagKey<Block> FORGE_BRONZE_BLOCKS = bindForge("storage_blocks/bronze_blocks");
    public static final TagKey<Block> FORGE_BRASS_BLOCKS = bindForge("storage_blocks/brass_blocks");

    private static @NotNull TagKey<Block> bindC(String id) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation("c", id));
    }

    private static @NotNull TagKey<Block> bindForge(String id) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation("forge", id));
    }

    private static @NotNull TagKey<Block> bind(String id) {
        return TagKey.create(Registries.BLOCK, AnvilCraft.of(id));
    }
}
