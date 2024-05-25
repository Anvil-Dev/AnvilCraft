package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.entity.ActiveSilencerBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.network.ClientboundMutedSoundSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class ActiveSilencerBlock extends BaseEntityBlock {
    public ActiveSilencerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ActiveSilencerBlockEntity(ModBlockEntities.ACTIVE_SILENCER.get(), pos, state);
    }

    @Override
    @SuppressWarnings({"deprecation", "UnreachableCode"})
    public @NotNull InteractionResult use(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull InteractionHand hand,
            @NotNull BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ActiveSilencerBlockEntity eb) {
            if (player instanceof ServerPlayer serverPlayer) {
                if (player.getItemInHand(hand).is(ModItems.DISK.get())) {
                    return eb.useDisk(
                            level,
                            player,
                            hand,
                            player.getItemInHand(hand),
                            hit
                    );
                }
                ModMenuTypes.open(serverPlayer, eb, pos);
                new ClientboundMutedSoundSyncPacket(new ArrayList<>(eb.getMutedSound())).send(serverPlayer);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
                type,
                ModBlockEntities.ACTIVE_SILENCER.get(),
                (level1, blockPos, blockState, blockEntity) -> blockEntity.tick(level1, blockPos)
        );
    }
}
