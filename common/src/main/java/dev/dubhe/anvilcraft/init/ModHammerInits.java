package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.api.hammer.HammerManager;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeableBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

public class ModHammerInits {
    /**
     * 初始化铁砧锤处理器
     */
    @SuppressWarnings("deprecation")
    public static void init() {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!block.builtInRegistryHolder().is(ModBlockTags.HAMMER_CHANGEABLE)) continue;
            HammerManager.registerChange(() -> block, IHammerChangeableBlock.DEFAULT);
        }
    }
}
