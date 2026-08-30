package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 冲压平台：在台面上放置原料，铁坷砸落时执行冲压配方。
 */
public class StampingPlatformBlock extends ProcessingTableBlock {
    public StampingPlatformBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.STAMPING_PLATFORM.create(pos, state);
    }

    @Override
    public ItemStack getCloneItemStack(
        BlockState state,
        HitResult target,
        LevelReader level,
        BlockPos pos,
        Player player
    ) {
        return new ItemStack(this);
    }
}
