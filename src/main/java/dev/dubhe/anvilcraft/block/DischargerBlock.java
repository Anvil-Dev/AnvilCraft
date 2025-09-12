package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.IStateListener;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class DischargerBlock extends ChargerBlock {

    public DischargerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, @NotNull Level level, ItemStack anvilHammer) {
        level.setBlock(blockPos, ModBlocks.CHARGER.getDefaultState(), 2);
        if (level.getBlockEntity(blockPos) instanceof IStateListener<?> listener) {
            IStateListener<Boolean> thiz = (IStateListener<Boolean>) listener;
            thiz.notifyStateChanged(true);
        }
        return true;
    }
}
