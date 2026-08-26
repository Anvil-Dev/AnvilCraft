package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.block.entity.HyperdimensionUploaderBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 超维上传站方块。
 *
 * <p>由「手持已绑定超维终端的玩家右键奇点晶体」转化而来，绑定对应的超维存储站。
 * 内部有 16 格缓存，可通过溜槽 / 漏斗输入输出，缓存内物品会按性能墙限速存入绑定的存储站。</p>
 *
 * <p>防炸（爆炸抗性 1200 与防爆标签）、防凋零、防龙。</p>
 */
public class HyperdimensionUploaderBlock extends BaseEntityBlock {
    public HyperdimensionUploaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(HyperdimensionUploaderBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ModBlockEntities.HYPERDIMENSION_UPLOADER.create(blockPos, blockState);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
            blockEntityType,
            ModBlockEntities.HYPERDIMENSION_UPLOADER.get(),
            (level1, pos, state1, entity) -> entity.tickServer()
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
        if (hand == InteractionHand.MAIN_HAND
            && stack.is(ModItems.HYPERDIMENSION_TERMINAL)
            && level.getBlockEntity(pos) instanceof HyperdimensionUploaderBlockEntity uploader
        ) {
            // 右键已绑定的超维终端可重新绑定到该终端指向的超维存储站
            if (!level.isClientSide) {
                TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
                if (binding == null || binding.id().isEmpty()) {
                    return ItemInteractionResult.sidedSuccess(true);
                }
                uploader.setStorageId(binding.id().get());
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof HyperdimensionUploaderBlockEntity uploader) {
            for (ItemStack drop : drops) {
                if (drop.is(this.asItem())) {
                    uploader.saveToDrop(drop, params.getLevel().registryAccess());
                }
            }
        }
        return drops;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // 创造模式敲掉非空上传站：与仓储端口一致，掉落带缓存与绑定数据的上传站物品
        if (!level.isClientSide && player.hasInfiniteMaterials()
            && level.getBlockEntity(pos) instanceof HyperdimensionUploaderBlockEntity uploader
            && !uploader.isBufferEmpty()) {
            ItemStack drop = new ItemStack(this);
            uploader.saveToDrop(drop, level.registryAccess());
            Block.popResource(level, pos, drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
