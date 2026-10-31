package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.logistics.storage.AbstractStoragePortBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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
