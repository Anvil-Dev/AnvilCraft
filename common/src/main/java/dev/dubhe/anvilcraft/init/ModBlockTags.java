package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import javax.swing.text.html.HTML.Tag;
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
    public static final TagKey<Block> LASE_CAN_PASS_THROUGH = bind("lase_can_pass_though");
    public static final TagKey<Block> GLASS = bindC("glass_block");

    private static @NotNull TagKey<Block> bindC(String id) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation("c", id));
    }

    private static @NotNull TagKey<Block> bind(String id) {
        return TagKey.create(Registries.BLOCK, AnvilCraft.of(id));
    }
}
