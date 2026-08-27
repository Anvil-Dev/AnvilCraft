package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.entity.HyperdimensionUploaderBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.UUID;

/**
 * 奇点晶体方块。
 *
 * <p>除了作为极端天体数据的存储介质外，手持已绑定超维存储站的超维终端右键时，
 * 奇点晶体将转化为绑定对应超维存储站的超维上传站。</p>
 */
public class SingularityCrystalBlock extends Block {
    public SingularityCrystalBlock(Properties properties) {
        super(properties);
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
        if (hand == InteractionHand.MAIN_HAND && stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            if (!level.isClientSide) {
                TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
                if (binding == null || binding.id().isEmpty()) {
                    player.displayClientMessage(
                        Component.translatable("message.anvilcraft.hyperdimension_terminal.not_bound"),
                        true
                    );
                    return ItemInteractionResult.sidedSuccess(true);
                }
                UUID storageId = binding.id().get();
                level.setBlockAndUpdate(pos, ModBlocks.HYPERDIMENSION_UPLOADER.get().defaultBlockState());
                if (level.getBlockEntity(pos) instanceof HyperdimensionUploaderBlockEntity uploader) {
                    uploader.setStorageId(storageId);
                }
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
                player.displayClientMessage(
                    Component.translatable("message.anvilcraft.hyperdimension_uploader.bound"),
                    true
                );
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
}
