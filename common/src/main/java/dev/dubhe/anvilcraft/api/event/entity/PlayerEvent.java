package dev.dubhe.anvilcraft.api.event.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import lombok.Getter;
import lombok.Setter;

public class PlayerEvent extends EntityEvent<Player> {
    /**
     * 玩家事件
     *
     * @param entity 实体
     * @param pos    位置
     * @param level  世界
     */
    public PlayerEvent(Player entity, BlockPos pos, Level level) {
        super(entity, pos, level);
    }

    @Getter
    public static class UseEntity extends PlayerEvent {
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
}
