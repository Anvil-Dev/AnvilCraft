package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.block.RuinsStructure;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.network.RuinsEditScreenPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class RuinsBlockItem extends BlockItem {
    public RuinsBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getPlayer() == null || !context.getPlayer().isCreative()) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockState(pos).is(ModBlocks.RUINS_BLOCK) && !context.getPlayer().isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins) {
            if (context.getPlayer().isShiftKeyDown() && context.getPlayer() instanceof ServerPlayer player) {
                RuinsBlockEntity main = RuinsStructure.main(level, ruins);
                List<String> ids = player.server.reloadableRegistries().get().registryOrThrow(Registries.LOOT_TABLE)
                    .keySet().stream().map(Object::toString).sorted().toList();
                PacketDistributor.sendToPlayer(player,
                    new RuinsEditScreenPacket(main.getBlockPos(), main.getDropsId(), main.isFragile(), ids));
            }
        } else {
            convert(level, pos);
        }
        return InteractionResult.CONSUME;
    }

    public static void convert(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        List<BlockPos> positions = state.getBlock() instanceof AbstractMultiPartBlock<?> multipart
            ? partPositions(multipart, state, pos) : List.of(pos);
        List<Snapshot> snapshots = new ArrayList<>();
        for (BlockPos part : positions) {
            if (!level.isLoaded(part)) return;
            BlockState partState = level.getBlockState(part);
            if (!partState.is(state.getBlock())) continue;
            BlockEntity entity = level.getBlockEntity(part);
            CompoundTag data = entity == null ? new CompoundTag() : entity.saveWithoutMetadata(level.registryAccess());
            snapshots.add(new Snapshot(part, partState, data));
        }
        // 先移除原实体，避免容器内容在替换时掉落；整组替换完成前不通知相邻部件。
        for (Snapshot snapshot : snapshots) level.removeBlockEntity(snapshot.pos);
        for (Snapshot snapshot : snapshots) {
            level.setBlock(snapshot.pos, ModBlocks.RUINS_BLOCK.getDefaultState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (level.getBlockEntity(snapshot.pos) instanceof RuinsBlockEntity ruins) {
                ruins.setDisplay(snapshot.state, snapshot.data);
            }
        }
        for (Snapshot snapshot : snapshots) {
            level.updateNeighborsAt(snapshot.pos, ModBlocks.RUINS_BLOCK.get());
        }
    }

    private static <P extends Enum<P>> List<BlockPos> partPositions(AbstractMultiPartBlock<P> block, BlockState state, BlockPos pos) {
        List<BlockPos> positions = new ArrayList<>();
        for (P part : block.getParts()) positions.add(pos.offset(block.offsetFrom(state, part)));
        return positions;
    }

    private record Snapshot(BlockPos pos, BlockState state, CompoundTag data) {
    }
}
