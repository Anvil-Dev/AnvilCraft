package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class FireCauldronBlock extends LayeredCauldronBlock implements IHammerRemovable {
    public FireCauldronBlock(Properties properties) {
        super(properties, p -> false, CauldronInteraction.LAVA);
    }

    @Override
    public void entityInside(
        @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity
    ) {
        if (this.isEntityInsideContent(state, pos, entity)) {
            entity.lavaHurt();
        }
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(
        @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull BlockState state
    ) {
        return new ItemStack(Items.CAULDRON);
    }
}
