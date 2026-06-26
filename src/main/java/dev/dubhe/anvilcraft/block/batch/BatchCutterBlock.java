package dev.dubhe.anvilcraft.block.batch;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.block.entity.batch.BatchCutterBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.network.MachineEnableFilterPacket;
import dev.dubhe.anvilcraft.network.MachineOutputDirectionPacket;
import dev.dubhe.anvilcraft.network.SlotDisableChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterChangePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class BatchCutterBlock extends BaseBatchCraftingBlock {
    public BatchCutterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<BatchCutterBlock> codec() {
        return Block.simpleCodec(BatchCutterBlock::new);
    }

    @Override
    protected InteractionResult playerUse(
        Level level,
        BlockPos pos,
        BlockState state,
        @Nullable BlockEntity be,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (!(be instanceof BatchCutterBlockEntity entity)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        ModMenuTypes.open(serverPlayer, entity, pos);
        PacketDistributor.sendToPlayer(serverPlayer, new MachineOutputDirectionPacket(entity.getDirection()));
        PacketDistributor.sendToPlayer(serverPlayer, new MachineEnableFilterPacket(entity.isFilterEnabled()));
        for (int i = 0; i < entity.getFilteredItems().size(); i++) {
            PacketDistributor.sendToPlayer(
                serverPlayer,
                new SlotDisableChangePacket(
                    i, entity.getFilteredItemStackHandler().getDisabled().get(i)
                )
            );
            PacketDistributor.sendToPlayer(serverPlayer, new SlotFilterChangePacket(i, entity.getFilter(i)));
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatchCutterBlockEntity(ModBlockEntities.BATCH_CUTTER.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return BaseEntityBlock.createTickerHelper(
            type,
            ModBlockEntities.BATCH_CUTTER.get(),
            (level1, pos, blockState, blockEntity) -> blockEntity.tick(level1, pos)
        );
    }

    @Override
    public Item getToastSymbol() {
        return Items.STONECUTTER;
    }
}
