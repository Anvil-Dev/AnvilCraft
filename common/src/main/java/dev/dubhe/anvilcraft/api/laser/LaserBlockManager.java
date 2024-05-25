package dev.dubhe.anvilcraft.api.laser;

import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RubyPrismBlockEntity;
import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class LaserBlockManager {
    private static HashMap<Level, LevelLaserBlockManager> levelLevelLaserBlockManagerMap = new HashMap<>();

    public static void registerLevelManager(Level level) {
        levelLevelLaserBlockManagerMap.put(level, new LevelLaserBlockManager(level));
    }

    private static void registerPrism(Level level, RubyPrismBlockEntity rubyPrismBlockEntity) {
        if (!levelLevelLaserBlockManagerMap.containsKey(level)) return;
        levelLevelLaserBlockManagerMap.get(level).registerPrism(
            rubyPrismBlockEntity.getBlockState().getValue(BlockStateProperties.FACING),
            rubyPrismBlockEntity.getBlockPos(),
            rubyPrismBlockEntity
        );
    }

    public static void registerBlock(Level level, BaseLaserBlockEntity baseLaserBlockEntity) {
        if (baseLaserBlockEntity instanceof RubyPrismBlockEntity rubyPrismBlockEntity)
            registerPrism(level, rubyPrismBlockEntity);
    }

    public static void unregisterBlock(Level level, BlockPos blockPos) {
        if (!levelLevelLaserBlockManagerMap.containsKey(level)) return;
        levelLevelLaserBlockManagerMap.get(level).unregister(blockPos);
    }


    public static void unregisterAll() {
        levelLevelLaserBlockManagerMap.clear();
    }

    public static LevelLaserBlockManager getLevelLaserBlockManager(Level level) {
        if (!levelLevelLaserBlockManagerMap.containsKey(level)) registerLevelManager(level);
        return levelLevelLaserBlockManagerMap.get(level);
    }
}
