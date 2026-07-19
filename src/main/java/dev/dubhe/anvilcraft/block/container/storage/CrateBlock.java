package dev.dubhe.anvilcraft.block.container.storage;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CrateBlock extends Block implements EntityBlock, IHammerRemovable {
    public CrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CRATE.create(pos, state);
    }

    public static List<CrateBlockEntity> getNearbyCrates(Level level, BlockPos sourcePos) {
        List<CrateBlockEntity> crates = new ArrayList<>();
        CrateBlockEntity source = null;
        for (BlockPos pos : BlockPos.betweenClosed(sourcePos.offset(-1, -1, -1), sourcePos.offset(1, 1, 1))) {
            if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) {
                continue;
            }
            if (pos.equals(sourcePos)) {
                source = crate;
            } else {
                crates.add(crate);
            }
        }
        if (source != null) {
            crates.add(source);
        }
        return crates;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CrateBlockEntity be) {
            be.playerWillDestroy(level, pos, state, player);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack itemStack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CrateBlockEntity entity) {
            if (player.isSpectator()) return InteractionResult.PASS;
            if (player instanceof ServerPlayer) {
                return InteractionResult.SUCCESS_SERVER;
            } else if (level.isClientSide()) {
                DistExecutor.run(Dist.CLIENT, () -> () -> StorageScreen.openScreen(entity.getBlockPos()));
                return InteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }
}
