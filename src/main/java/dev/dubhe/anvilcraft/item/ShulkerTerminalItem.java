package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 潜影终端：不可绑定储存方块，使用时按优先级自动连接
 * 玩家身上槽位最靠前的潜影集装箱、聚合身上的所有潜影盒，
 * 或 64 格以内的一个最近的世界潜影集装箱；其余功能与超维终端一致。
 */
public class ShulkerTerminalItem extends TerminalItem {
    public ShulkerTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            StorageTerminalClientStub.openRemote(StorageTerminalClientStub.shulkerTerminalId())
                .exceptionally(ignored -> -1L)
                .thenAccept(virtualPos -> Minecraft.getInstance().execute(() -> {
                    if (virtualPos == -1L) {
                        player.displayClientMessage(
                            Component.translatable("message.anvilcraft.shulker_terminal.not_found"),
                            true
                        );
                        return;
                    }
                    StorageScreen.openScreen(
                        BlockPos.of(virtualPos),
                        Component.translatable("block.anvilcraft.shulker_container")
                    );
                }));
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }
        return new InteractionResultHolder<>(InteractionResult.PASS, stack);
    }
}
