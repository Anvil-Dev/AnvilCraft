package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager.Entry;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PiezoelectricCrystalBlock extends Block implements IHammerRemovable {

    public static final Map<Block, List<Integer>> ANVIL_TYPES = new HashMap<>();

    static {
        ANVIL_TYPES.put(ModBlocks.SPECTRAL_ANVIL.get(), List.of(1, 2, 3, 4));
        ANVIL_TYPES.put(ModBlocks.ROYAL_ANVIL.get(), List.of(1, 2, 4, 8));
        ANVIL_TYPES.put(Blocks.ANVIL, List.of(1, 2, 4, 8));
        ANVIL_TYPES.put(Blocks.CHIPPED_ANVIL, List.of(1, 2, 4, 8));
        ANVIL_TYPES.put(Blocks.DAMAGED_ANVIL, List.of(1, 2, 4, 8));
        ANVIL_TYPES.put(ModBlocks.EMBER_ANVIL.get(), List.of(1, 2, 5, 12));
        ANVIL_TYPES.put(ModBlocks.TRANSCENDENCE_ANVIL.get(), List.of(2, 5, 15, 60));
        ANVIL_TYPES.put(ModBlocks.GIANT_ANVIL.get(), List.of(1, 2, 3, 4, 5, 6, 7, 8));
    }

    public static VoxelShape SHAPE =
        Shapes.or(Block.box(0, 14, 0, 16, 16, 16), Block.box(2, 2, 2, 14, 14, 14), Block.box(0, 0, 0, 16, 2, 16));

    public PiezoelectricCrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    /**
     * 被铁砧砸事件
     */
    public void onHitByAnvil(FallingBlockEntity entity, float fallDistance, Level level, BlockPos blockPos) {
        if (level.getBlockTicks().hasScheduledTick(blockPos, this)) return;
        List<Integer> chargeNums = ANVIL_TYPES.get(entity.blockState.getBlock());
        if (chargeNums == null) return;
        int distance = (int) Math.min(chargeNums.size() - 1, fallDistance);
        int chargeNum = chargeNums.get(distance);
        ChargeCollectorManager.charge(chargeNum, level, blockPos);
        pressureConduction(level, blockPos, chargeNum / 2);
        TriggerUtil.anvilHitPiezoelectricCrystal(level, blockPos);
        level.scheduleTick(blockPos, this, 4);
    }

    private void pressureConduction(Level level, BlockPos blockPos, int chargeNum) {
        BlockPos pos = blockPos.below();
        if (level.getBlockState(pos).getBlock() instanceof PiezoelectricCrystalBlock block) {
            if (chargeNum == 0) return;
            ChargeCollectorManager.charge(chargeNum, level, blockPos);
            block.pressureConduction(level, pos, chargeNum / 2);
        }
    }
}
