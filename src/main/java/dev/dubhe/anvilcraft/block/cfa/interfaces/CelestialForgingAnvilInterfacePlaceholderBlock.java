package dev.dubhe.anvilcraft.block.cfa.interfaces;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
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
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CelestialForgingAnvilInterfacePlaceholderBlock
    extends HorizontalDirectionalBlock
    implements IHammerRemovable, IHammerChangeable {

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
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState state = level.getBlockState(blockPos);
        level.setBlockAndUpdate(blockPos, state.cycle(FACING));
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        // If player is holding an interface block item, replace this placeholder with it
        if (stack.getItem() instanceof CelestialForgingAnvilInterfaceBlockItem interfaceItem) {
            Block interfaceBlock = interfaceItem.getBlock();
            if (interfaceBlock instanceof CelestialForgingAnvilInterfaceBlock) {
                if (level.isClientSide()) {
                    return ItemInteractionResult.SUCCESS;
                }
                Direction facing = state.getValue(FACING);
                BlockState placementState = interfaceBlock.defaultBlockState()
                    .setValue(CelestialForgingAnvilInterfaceBlock.FACING, facing)
                    .setValue(CelestialForgingAnvilInterfaceBlock.ACTIVE, false);
                level.setBlockAndUpdate(pos, placementState);
                // Play placement sound
                SoundType soundType = placementState.getSoundType();
                level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0f) / 2.0f, soundType.getPitch() * 0.8f);
                // Consume one interface item if not in creative
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                // Return a placeholder item
                ItemStack placeholderStack = new ItemStack(this);
                if (!player.getInventory().add(placeholderStack)) {
                    player.drop(placeholderStack, false);
                }
                return ItemInteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult
    ) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        // Scan nearby for the controller (BOTTOM_CENTER of anvil)
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    BlockState checkState = level.getBlockState(checkPos);
                    if (
                        checkState.getBlock() instanceof CelestialForgingAnvilBlock
                        && checkState.hasProperty(CelestialForgingAnvilBlock.HALF)
                        && checkState.getValue(CelestialForgingAnvilBlock.HALF) == Cube323PartHalf.BOTTOM_CENTER
                    ) {
                        BlockEntity be = level.getBlockEntity(checkPos);
                        if (
                            be instanceof CelestialForgingAnvilBlockEntity cfaBe
                            && player instanceof ServerPlayer sp
                        ) {
                            if (sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) return InteractionResult.PASS;
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
