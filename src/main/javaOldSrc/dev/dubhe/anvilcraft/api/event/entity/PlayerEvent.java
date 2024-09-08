package dev.dubhe.anvilcraft.api.event.entity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PlayerEvent<T extends Player> extends EntityEvent<T> {
    /**
     * 玩家事件
     *
     * @param entity 实体
     * @param pos    位置
     * @param level  世界
     */
    public PlayerEvent(T entity, BlockPos pos, Level level) {
        super(entity, pos, level);
    }

    @Getter
    public static class UseEntity extends PlayerEvent<Player> {
        private final Entity target;
        private final InteractionHand hand;
        @Setter
        private InteractionResult result = InteractionResult.PASS;

        /**
         * 玩家右键实体事件
         */
        public UseEntity(Player entity, Entity target, InteractionHand hand, Level level) {
            super(entity, entity.getOnPos(), level);
            this.target = target;
            this.hand = hand;
        }
    }

    public static class ClientPlayerJoin extends PlayerEvent<LocalPlayer> {
        /**
         * 玩家加入事件
         *
         * @param entity 实体
         * @param pos    位置
         * @param level  世界
         */
        public ClientPlayerJoin(LocalPlayer entity, BlockPos pos, Level level) {
            super(entity, pos, level);
        }
    }
}
