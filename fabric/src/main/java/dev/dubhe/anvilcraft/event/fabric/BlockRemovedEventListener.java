package dev.dubhe.anvilcraft.event.fabric;

import dev.dubhe.anvilcraft.api.chargecollector.HeatedBlockRecorder;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

public class BlockRemovedEventListener {
    /**
     * 初始化
     */
    public static void init() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            HeatedBlockRecorder.INSTANCE.delete(pos);
        });
    }
}
