package dev.dubhe.anvilcraft.event.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.entity.PlayerEvent;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import org.jetbrains.annotations.Nullable;

public class PlayerEventListener {
    /**
     * 初始化
     */
    public static void init() {
        UseEntityCallback.EVENT.register(PlayerEventListener::useEntity);
    }

    private static InteractionResult useEntity(
            Player player,
            Level world,
            InteractionHand hand,
            Entity entity,
            @Nullable EntityHitResult hitResult) {
        PlayerEvent.UseEntity playerEvent = new PlayerEvent.UseEntity(player, entity, hand, world);
        AnvilCraft.EVENT_BUS.post(playerEvent);
        return playerEvent.getResult();
    }
}
