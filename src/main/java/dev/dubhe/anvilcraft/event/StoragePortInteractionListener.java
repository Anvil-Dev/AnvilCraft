package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IStoragePort;
import dev.dubhe.anvilcraft.block.logistics.storage.AbstractStoragePortBlock;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import dev.dubhe.anvilcraft.network.StoragePortTakeOutPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StoragePortInteractionListener {
    @SubscribeEvent
    public static void clickStoragePortEvent(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        if (!(level.getBlockState(pos).getBlock() instanceof AbstractStoragePortBlock portBlock)) {
            return;
        }
        // 交互区域之外（例如模型外边缘一像素框架）：敲掉逻辑，不拦截
        if (!portBlock.interceptsLeftClick(level, pos, player, StoragePortInteractionListener.aimHit(player))) {
            return;
        }
        // 取消事件，阻止挖掘（客户端与服务端都会触发本事件）
        event.setCanceled(true);
        if (!level.isClientSide() || player.getMainHandItem().getItem() instanceof AnvilHammerItem) {
            return;
        }
        // 客户端只发送取出请求，实际取出由服务端在收到请求包后执行
        PlayerInteractEvent.LeftClickBlock.Action action = event.getAction();
        if (
            action != PlayerInteractEvent.LeftClickBlock.Action.START
            && action != PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD
        ) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof IStoragePort port) || port.isLeftClickOnCooldown(player)) {
            return;
        }
        ClientPacketDistributor.sendToServer(new StoragePortTakeOutPacket(pos));
    }

    /**
     * 沿玩家视线做射线检测，得到精确的瞄准位置（左键事件本身不提供命中点）。
     */
    @Nullable
    private static BlockHitResult aimHit(Player player) {
        HitResult pick = player.pick(player.blockInteractionRange(), 1.0F, false);
        return pick instanceof BlockHitResult hitResult ? hitResult : null;
    }

}
