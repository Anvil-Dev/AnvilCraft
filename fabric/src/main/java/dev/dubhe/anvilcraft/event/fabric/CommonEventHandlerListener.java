package dev.dubhe.anvilcraft.event.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.server.block.ServerBlockEntityLoadEvent;
import dev.dubhe.anvilcraft.api.event.server.block.ServerBlockEntityUnloadEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CommonEventHandlerListener {
    public static void serverInit() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(CommonEventHandlerListener::onServerBlockEntityLoad);
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(CommonEventHandlerListener::onServerBlockEntityUnload);
    }

    private static void onServerBlockEntityLoad(BlockEntity entity, ServerLevel level) {
        AnvilCraft.EVENT_BUS.post(new ServerBlockEntityLoadEvent(level, entity));
    }

    private static void onServerBlockEntityUnload(BlockEntity entity, ServerLevel level) {
        AnvilCraft.EVENT_BUS.post(new ServerBlockEntityUnloadEvent(level, entity));
    }
}
