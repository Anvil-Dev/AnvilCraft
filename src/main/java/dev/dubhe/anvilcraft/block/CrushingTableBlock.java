package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 粉碎台：在台面上放置原料，铁坷砸落时执行粉碎配方。
 */
public class CrushingTableBlock extends ProcessingTableBlock {
    public CrushingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CRUSHING_TABLE.create(pos, state);
    }
}
