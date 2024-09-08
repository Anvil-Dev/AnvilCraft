package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.ActiveSilencerBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.network.MutedSoundSyncPacket;
import dev.dubhe.anvilcraft.util.Utils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ActiveSilencerBlock extends BaseEntityBlock implements IHammerRemovable {
    public ActiveSilencerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ActiveSilencerBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ActiveSilencerBlockEntity(ModBlockEntities.ACTIVE_SILENCER.get(), pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack pStack,
        BlockState pState,
        Level pLevel,
        BlockPos pPos,
        Player pPlayer,
        InteractionHand pHand,
        BlockHitResult pHitResult
    ) {
        if (pLevel.isClientSide) return ItemInteractionResult.SUCCESS;
        if (pPlayer instanceof ServerPlayer serverPlayer) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof ActiveSilencerBlockEntity asbe && pPlayer.getItemInHand(pHand).is(ModItems.DISK.get())) {
                return Utils.interactionResultConverter().apply(asbe.useDisk(
                    pLevel,
                    serverPlayer,
                    pHand,
                    serverPlayer.getItemInHand(pHand),
                    pHitResult
                ));
            }
        }
        return super.useItemOn(pStack, pState, pLevel, pPos, pPlayer, pHand, pHitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState pState,
        Level pLevel,
        BlockPos pPos,
        Player pPlayer,
        BlockHitResult pHitResult
    ) {
        if (pLevel.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (be instanceof ActiveSilencerBlockEntity asbe && pPlayer instanceof ServerPlayer sp) {
            ModMenuTypes.open(sp, asbe, pPos);
            PacketDistributor.sendToPlayer(sp, new MutedSoundSyncPacket(new ArrayList<>(asbe.getMutedSound())));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }
}
