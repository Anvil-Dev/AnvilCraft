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
 * 本地终端：不可绑定储存方块，使用时自动连接玩家 32 格以内的一个最近的大型板条箱，
 * 其余功能（终端界面、超级收纳袋、JEI 补库、物品均衡）与超维终端一致。
 */
public class LocalTerminalItem extends TerminalItem {
    public LocalTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            StorageTerminalClientStub.openRemote(StorageTerminalClientStub.localTerminalId())
                .exceptionally(ignored -> -1L)
                .thenAccept(virtualPos -> Minecraft.getInstance().execute(() -> {
                    if (virtualPos == -1L) {
                        player.displayClientMessage(
                            Component.translatable("message.anvilcraft.local_terminal.not_found"),
                            true
                        );
                        return;
                    }
                    StorageScreen.openScreen(
                        BlockPos.of(virtualPos),
                        Component.translatable("block.anvilcraft.large_crate")
                    );
                }));
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }
        return new InteractionResultHolder<>(InteractionResult.PASS, stack);
    }
}
