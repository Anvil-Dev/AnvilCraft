package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class StructureScannerBlock extends BaseEntityBlock {
    public static final MapCodec<StructureScannerBlock> CODEC = simpleCodec(StructureScannerBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public StructureScannerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StructureScannerBlockEntity(ModBlockEntities.STRUCTURE_SCANNER.get(), pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> type
    ) {
        if (level.isClientSide()) {
            return null;
        }
        return (level1, pos, state1, entity) -> {
            if (entity instanceof StructureScannerBlockEntity be) {
                be.tickServer(level1, pos);
            }
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            var menuProvider = state.getMenuProvider(level, pos);
            if (menuProvider != null) {
                ModMenuTypes.open(serverPlayer, menuProvider, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    
    @Override
    public void neighborChanged(
        BlockState state, Level level, BlockPos pos,
        net.minecraft.world.level.block.Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        
        boolean powered = level.hasNeighborSignal(pos);
        boolean wasPowered = state.getValue(POWERED);
        
        // 更新红石状态
        if (powered != wasPowered) {
            level.setBlock(pos, state.setValue(POWERED, powered), 2);
        }
        
        // 收到红石信号时，自动执行扫描并保存
        if (powered && !wasPowered) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof StructureScannerBlockEntity scannerEntity) {
                // 执行自动扫描和保存
                autoScanAndSave(level, scannerEntity);
            }
        }
    }
    
    /**
     * 自动扫描并保存结构到磁盘
     */
    @SuppressWarnings("unused")
    private void autoScanAndSave(Level level, StructureScannerBlockEntity scannerEntity) {
        // 检查是否有磁盘
        if (scannerEntity.getDiskInventory().getItem(0).isEmpty()) {
            return;
        }
        
        // 检查输出槽位是否为空
        if (!scannerEntity.getOutputInventory().getItem(0).isEmpty()) {
            return;
        }
        
        // 检查是否已经在扫描
        if (scannerEntity.isScanning()) {
            return;
        }
        
        // 生成结构名称（使用年月日时分格式：auto-202605221430）
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        String structureName = "auto-" + now.format(formatter);
        
        // 开始扫描
        scannerEntity.startScanning();
        
        // 等待扫描完成后保存（需要延迟执行）
        // 这里使用一个简单的策略：在 tickServer 中检查扫描完成状态
        scannerEntity.scheduleAutoSave(structureName);
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof StructureScannerBlockEntity scannerEntity) {
                    // 掉落Disk物品栏中的物品
                    for (int i = 0; i < scannerEntity.getDiskInventory().getContainerSize(); i++) {
                        ItemStack stack = scannerEntity.getDiskInventory().getItem(i);
                        if (!stack.isEmpty()) {
                            Vec3 vec3 = pos.getCenter();
                            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                                level,
                                vec3.x,
                                vec3.y,
                                vec3.z,
                                stack
                            );
                            itemEntity.setDefaultPickUpDelay();
                            level.addFreshEntity(itemEntity);
                        }
                    }
                    
                    // 掉落Output物品栏中的物品
                    for (int i = 0; i < scannerEntity.getOutputInventory().getContainerSize(); i++) {
                        ItemStack stack = scannerEntity.getOutputInventory().getItem(i);
                        if (!stack.isEmpty()) {
                            Vec3 vec3 = pos.getCenter();
                            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                                level,
                                vec3.x,
                                vec3.y,
                                vec3.z,
                                stack
                            );
                            itemEntity.setDefaultPickUpDelay();
                            level.addFreshEntity(itemEntity);
                        }
                    }
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}
