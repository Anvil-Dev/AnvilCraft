package dev.dubhe.anvilcraft.block.container;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class CreativeFluidTankBlock extends BaseEntityBlock implements IHammerRemovable {

    public CreativeFluidTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(CreativeFluidTankBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CREATIVE_FLUID_TANK.create(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        if (level.getBlockEntity(pos) instanceof CreativeFluidTankBlockEntity tank) {
            return tank.onPlayerUse(player, hand)
                   ? InteractionResult.SUCCESS
                   : InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
}
