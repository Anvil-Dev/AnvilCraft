package dev.dubhe.anvilcraft.api.event;

import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 巨型铁砧生命周期扩展。均在服务器线程发布。
 * 取消 {@link Multiblock} 将跳过写死的多方块转化配方；
 * 取消 {@link BlockTick} / {@link FallingTick} 将跳过本体本 tick 的坍塌或下落逻辑。
 */
public class GiantAnvilEvent {
    private GiantAnvilEvent() {
    }

    @Getter
    public static class Multiblock extends Event implements ICancellableEvent {
        private final AnvilEvent.GiantOnLand landing;

        public Multiblock(AnvilEvent.GiantOnLand landing) {
            this.landing = landing;
        }
    }

    @Getter
    public static class BlockTick extends Event implements ICancellableEvent {
        private final GiantAnvilBlock block;
        private final BlockState state;
        private final ServerLevel level;
        private final BlockPos pos;
        private final RandomSource random;

        public BlockTick(
            GiantAnvilBlock block,
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
        ) {
            this.block = block;
            this.state = state;
            this.level = level;
            this.pos = pos;
            this.random = random;
        }
    }

    @Getter
    public static class FallingTick extends Event implements ICancellableEvent {
        private final FallingGiantAnvilEntity entity;

        public FallingTick(FallingGiantAnvilEntity entity) {
            this.entity = entity;
        }
    }
}
