package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.block.entity.TeslaTowerBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Vertical4PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TeslaTowerItem extends SimpleMultiPartBlockItem<Vertical4PartHalf> {
    public TeslaTowerItem(SimpleMultiPartBlock<Vertical4PartHalf> block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(
        @NotNull BlockPos pos,
        @NotNull Level level,
        @Nullable Player player,
        @NotNull ItemStack stack,
        @NotNull BlockState state) {
        if (!(level.getBlockEntity(pos) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity)) return false;
        if (player == null) return false;
        teslaTowerBlockEntity.initWhiteList(player);
        return true;
    }
}
