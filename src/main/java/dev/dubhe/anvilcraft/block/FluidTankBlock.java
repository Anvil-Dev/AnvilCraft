package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.IController;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModMultiblockDefinitions;
import dev.dubhe.anvilcraft.util.TankUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FluidTankBlock extends BaseEntityBlock implements HammerRotateBehavior, IHammerRemovable, IController {

    public FluidTankBlock(Properties properties) {
        super(properties);
    }

    /**
     * 判断该储罐位置是否相邻任意门格海绵。
     *
     * <p>相邻（6 向，不含斜角）时储罐进入溢出销毁模式：灌满后继续输入的流体会被直接
     * 销毁，与相邻虚空物质块的板条箱销毁溢出物品相对应。门格海绵本身能无限吸收流体，
     * 因此把溢出交给它销毁在设定上自洽。</p>
     *
     * <p>注意这与 3×3×3 门格结构（{@link TankUtil#isMengerStructure}）不同：后者会把储罐
     * 升级为无限容量，本判定只要求单格相邻。</p>
     *
     * @param level 世界
     * @param pos   储罐位置
     * @return 相邻存在门格海绵时为 true
     */
    public static boolean hasAdjacentMengerSponge(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(ModBlocks.MENGER_SPONGE.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        FluidTankBlock.updateDisposeFromNeighbors(level, pos);
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        FluidTankBlock.updateDisposeFromNeighbors(level, pos);
    }

    /** 相邻方块变化后，按当前邻居重算该储罐的溢出销毁模式。 */
    private static void updateDisposeFromNeighbors(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            tank.refreshDispose();
        }
    }

    /**
     * 按相邻门格海绵返回储罐显示名：溢出销毁模式显示「过量销毁储罐」。
     */
    public static Component displayName(LevelReader level, BlockPos pos) {
        return FluidTankBlock.hasAdjacentMengerSponge(level, pos)
            ? Component.translatable("block.anvilcraft.overflow_disposal_fluid_tank")
            : Component.translatable("block.anvilcraft.fluid_tank");
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(FluidTankBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.FLUID_TANK.create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.FLUID_TANK.get(), FluidTankBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
            if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
                if (tank.onPlayerUse(player, hand)) {
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }

        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof FluidTankBlockEntity tank
            && !tank.getFluidHandler().getFluidInTank(0).isEmpty()) {
            for (ItemStack drop : drops) {
                if (drop.is(this.asItem())) {
                    tank.saveToDrop(drop, params.getLevel().registryAccess());
                }
            }
        }
        return drops;
    }

    @Override
    public Block getBlock() {
        return this;
    }

    @Override
    public ResourceLocation getDefinitionId() {
        return ModMultiblockDefinitions.FLUID_TANK.location();
    }

    @Override
    public void onFormed(Level level, MultiblockState state) {
        if (!TankUtil.isMengerStructure(level, state.getControllerPos(), 3)) return;
        level.getBlockEntity(state.getControllerPos(), ModBlockEntities.FLUID_TANK.get())
            .ifPresent(FluidTankBlockEntity::onFormed);
    }

    @Override
    public void onUnformed(Level level, MultiblockState state) {
        level.getBlockEntity(state.getControllerPos(), ModBlockEntities.FLUID_TANK.get())
            .ifPresent(FluidTankBlockEntity::onUnformed);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof FluidTankBlockEntity be) {
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
        AuxiliaryLightManager manager = level.getAuxLightManager(pos);
        if (manager == null) return 0;
        return manager.getLightAt(pos);
    }
}
