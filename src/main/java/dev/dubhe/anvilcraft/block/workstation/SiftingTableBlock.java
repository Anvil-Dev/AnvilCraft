package dev.dubhe.anvilcraft.block.workstation;

import dev.dubhe.anvilcraft.block.ProcessingTableBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 过筛台：在台面上放置原料，铁坷砸落时执行过筛配方。
 */
public class SiftingTableBlock extends ProcessingTableBlock {
    public SiftingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.SIFTING_TABLE.create(pos, state);
    }
}
