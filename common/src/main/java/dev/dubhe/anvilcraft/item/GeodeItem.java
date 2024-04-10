package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.BlockHighlightUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.NotNull;

public class GeodeItem extends Item {
    public GeodeItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide) return InteractionResult.SUCCESS;
        int interval = AnvilCraft.config.geodeInterval;
        int radius = AnvilCraft.config.geodeRadius;
        for (int x = -radius; x <= radius; x += interval) {
            for (int z = -radius; z <= radius; z += interval) {
                int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = level.getMinBuildHeight(); y <= height; y += interval) {
                    BlockPos offsetPos = pos.offset(x, y, z);
                    BlockState state = level.getBlockState(offsetPos);
                    if(!state.is(BlockTags.CRYSTAL_SOUND_BLOCKS)) continue;
                    BlockHighlightUtil.highlightBlock(level, offsetPos);
                    break;
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
