package dev.dubhe.anvilcraft.util.sc;

import dev.dubhe.anvilcraft.block.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.entity.ShulkerContainerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class SCBESyncers {
    private static final Map<Level, SCBESyncers> MANAGERS = new HashMap<>();
    private final Map<BlockPos, SCBESyncer> syncers;

    private SCBESyncers(Level ignored) {
        this.syncers = new HashMap<>();
    }

    public static SCBESyncers get(Level level) {
        return SCBESyncers.MANAGERS.computeIfAbsent(level, SCBESyncers::new);
    }

    public static void clear() {
        MANAGERS.clear();
    }

    public SCBESyncer register(ShulkerContainerBlockEntity be) {
        return this.syncers.computeIfAbsent(
            be.getBlockState().getValue(ShulkerContainerBlock.HALF).toMain(be.getBlockPos()).immutable(),
            SCBESyncer::new
        );
    }

    public void remove(ShulkerContainerBlockEntity be) {
        this.syncers.remove(be.getBlockState().getValue(ShulkerContainerBlock.HALF).toMain(be.getBlockPos()));
    }
}
