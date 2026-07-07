package dev.dubhe.anvilcraft.api.block.entity;

import dev.anvilcraft.lib.v2.util.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

public interface ITickable {
    static <T extends BlockEntity> @Nullable BlockEntityTicker<T> tickServerOnly(
        Level level,
        BlockEntityType<?> expected,
        BlockEntityType<?> actual
    ) {
        if (level.isClientSide()) {
            return null;
        }
        return ITickable.tick(expected, actual);
    }

    static <T extends BlockEntity> @Nullable BlockEntityTicker<T> tick(
        BlockEntityType<?> expected,
        BlockEntityType<?> actual
    ) {
        if (expected != actual) {
            return null;
        }
        return (_, _, _, entity) -> Util.<ITickable>cast(entity).tick();
    }

    void tick();
}
