package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * 放射性储存块（铀块、钚块）。密堆时受随机刻触发衰变。
 *
 * <p>设 R = 六向相邻的放射性块数量，L = 六向相邻的铅块数量：
 * <ul>
 *     <li>当 R == 6（六面全为放射性块）时，衰变为熔岩。</li>
 *     <li>否则当 R >= 3 + L 时，衰变为 {@link #decayResult}（钚→铀、铀→铅）。</li>
 * </ul>
 * 相邻的铅块起屏蔽作用：每有一个铅块，衰变所需的相邻放射性块阈值 +1。
 */
public class RadioactiveBlock extends Block {
    /** 无铅屏蔽时触发衰变所需的最小相邻放射性块数。 */
    public static final int BASE_DECAY_THRESHOLD = 3;

    private final Supplier<? extends Block> decayResult;

    public RadioactiveBlock(Properties properties, Supplier<? extends Block> decayResult) {
        super(properties.randomTicks());
        this.decayResult = decayResult;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int radioactiveCount = 0;
        int leadCount = 0;
        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.getBlock() instanceof RadioactiveBlock) {
                radioactiveCount++;
            } else if (neighbor.is(ModBlockTags.STORAGE_BLOCKS_LEAD)) {
                leadCount++;
            }
        }

        if (radioactiveCount >= Direction.values().length) {
            level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
            return;
        }

        if (radioactiveCount >= BASE_DECAY_THRESHOLD + leadCount) {
            level.setBlockAndUpdate(pos, decayResult.get().defaultBlockState());
        }
    }
}
