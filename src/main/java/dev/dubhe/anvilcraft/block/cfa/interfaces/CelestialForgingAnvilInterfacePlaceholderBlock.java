package dev.dubhe.anvilcraft.block.cfa.interfaces;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.item.CelestialForgingAnvilInterfaceBlockItem;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public class CelestialForgingAnvilInterfacePlaceholderBlock
    extends HorizontalDirectionalBlock
    implements IHammerRemovable {

    public CelestialForgingAnvilInterfacePlaceholderBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(CelestialForgingAnvilInterfacePlaceholderBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        if (stack.getItem() instanceof CelestialForgingAnvilInterfaceBlockItem interfaceItem) {
            Block interfaceBlock = interfaceItem.getBlock();
            if (interfaceBlock instanceof CelestialForgingAnvilInterfaceBlock) {
                if (level.isClientSide()) {
                    return InteractionResult.SUCCESS;
                }
                Direction facing = state.getValue(FACING);
                BlockState placementState = interfaceBlock.defaultBlockState()
                    .setValue(CelestialForgingAnvilInterfaceBlock.FACING, facing)
                    .setValue(CelestialForgingAnvilInterfaceBlock.ACTIVE, false);
                level.setBlockAndUpdate(pos, placementState);
                SoundType soundType = placementState.getSoundType();
                level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0f) / 2.0f, soundType.getPitch() * 0.8f);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                ItemStack placeholderStack = new ItemStack(this);
                if (!player.getInventory().add(placeholderStack)) {
                    player.drop(placeholderStack, false);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult
    ) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    BlockState checkState = level.getBlockState(checkPos);
                    if (checkState.getBlock() instanceof CelestialForgingAnvilBlock
                        && checkState.hasProperty(CelestialForgingAnvilBlock.HALF)
                        && checkState.getValue(CelestialForgingAnvilBlock.HALF) == Cube323PartHalf.BOTTOM_CENTER) {
                        if (level.getBlockEntity(checkPos) instanceof CelestialForgingAnvilBlockEntity cfaBe
                            && player instanceof ServerPlayer sp) {
                            ModMenuTypes.open(sp, cfaBe, checkPos);
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }
}
