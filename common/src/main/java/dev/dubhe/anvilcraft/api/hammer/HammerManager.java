package dev.dubhe.anvilcraft.api.hammer;

import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class HammerManager {
    private static final Map<Supplier<Block>, IHammerChangeable> INIT_CHANGE = new HashMap<>();
    private static final Map<Block, IHammerChangeable> CHANGE = new HashMap<>();

    public static void registerChange(Supplier<Block> block, IHammerChangeable changeable) {
        HammerManager.INIT_CHANGE.put(block, changeable);
    }

    public static IHammerChangeable getChange(Block block) {
        if (block instanceof IHammerChangeable changeable) return changeable;
        return HammerManager.CHANGE.getOrDefault(block, IHammerChangeableBlock.EMPTY);
    }

    /**
     * 注册铁砧锤处理器
     */
    public static void register() {
        for (Map.Entry<Supplier<Block>, IHammerChangeable> entry : INIT_CHANGE.entrySet()) {
            HammerManager.CHANGE.put(entry.getKey().get(), entry.getValue());
        }
    }
}
