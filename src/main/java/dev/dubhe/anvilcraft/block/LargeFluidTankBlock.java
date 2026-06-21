package dev.dubhe.anvilcraft.block;

import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.IController;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModMultiblockDefinitions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import org.jetbrains.annotations.Nullable;

import static dev.dubhe.anvilcraft.block.PropelPiston.createTickerHelper;

public class LargeFluidTankBlock
    extends SimpleMultiPartBlock<Cube3x3PartHalf>
    implements MultiPartBlockEntity<Cube3x3PartHalf, LargeFluidTankBlock>, IHammerRemovable, IController {
    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);

    public LargeFluidTankBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(HALF, Cube3x3PartHalf.BOTTOM_CENTER));
    }

    @Override
    public Vec3i getMainPartOffset() {
        return new Vec3i(0, 1, 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    public Property<Cube3x3PartHalf> getPart() {
        return HALF;
    }

    @Override
    public Cube3x3PartHalf[] getParts() {
        return Cube3x3PartHalf.values();
    }

    @Override
    public LargeFluidTankBlock getMultiBlock() {
        return this;
    }

    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return createBlockEntity(pos, state);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.LARGE_FLUID_TANK.create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
            type,
            ModBlockEntities.LARGE_FLUID_TANK.get(),
            (level1, blockPos, blockState, blockEntity) -> blockEntity.tick()
        );
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        InteractionResult result = super.useItemOn(stack, state, level, pos, player, hand, hitResult).result();
        if (result == InteractionResult.PASS) {
            BlockPos mainPartPos = getMainPartPos(pos, state);
            BlockEntity blockEntity = level.getBlockEntity(mainPartPos);
            if (blockEntity instanceof LargeFluidTankBlockEntity tank) {
                if (tank.onPlayerUse(player, hand)) {
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof LargeFluidTankBlockEntity be) {
            return be.getRedstoneSignal();
        }
        return 0;
    }

    @Override
    public boolean hasDynamicLightEmission(BlockState state) {
        return true;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        BlockPos mainPartPos = this.getMainPartPos(pos, state);
        AuxiliaryLightManager manager = level.getAuxLightManager(mainPartPos);
        if (manager == null) return 0;
        return manager.getLightAt(mainPartPos);
    }

    @Override
    public Block getBlock() {
        return this;
    }

    @Override
    public ResourceLocation getDefinitionId() {
        return ModMultiblockDefinitions.LARGE_FLUID_TANK.location();
    }

    @Override
    public void onFormed(Level level, MultiblockState state) {
        level.getBlockEntity(state.getControllerPos(), ModBlockEntities.LARGE_FLUID_TANK.get())
            .ifPresent(LargeFluidTankBlockEntity::onFormed);
    }

    @Override
    public void onUnformed(Level level, MultiblockState state) {
        level.getBlockEntity(state.getControllerPos(), ModBlockEntities.LARGE_FLUID_TANK.get())
            .ifPresent(LargeFluidTankBlockEntity::onUnformed);
    }

    @Override
    public BlockPos correctPos(ServerLevel level, BlockPos pos, BlockState state) {
        return this.getMainPartPos(pos, state);
    }
}
