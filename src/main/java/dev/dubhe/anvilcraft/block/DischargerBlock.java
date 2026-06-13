package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.entity.DischargerBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.IStateListener;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DischargerBlock extends ChargerBlock {

    public DischargerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DischargerBlockEntity(ModBlockEntities.DISCHARGER.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type
    ) {
        if (level.isClientSide) return null;
        return createTickerHelper(
            type,
            ModBlockEntities.DISCHARGER.get(),
            (level1, blockPos, blockState, blockEntity) -> blockEntity.tick(level1, blockPos)
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        level.setBlock(blockPos, ModBlocks.CHARGER.getDefaultState(), 2);
        if (level.getBlockEntity(blockPos) instanceof IStateListener<?> listener) {
            IStateListener<Boolean> self = (IStateListener<Boolean>) listener;
            self.notifyStateChanged(true);
        }
        return true;
    }
}
