package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerUtil {
    public static boolean isFakePlayer(Player player) {
        return player instanceof FakePlayer;
    }

    public static boolean isClient(Player player) {
        AtomicBoolean result = new AtomicBoolean(false);
        DistExecutor.run(Dist.CLIENT, () -> () -> result.set(player instanceof LocalPlayer));
        return result.get();
    }

    public static EquipmentSlot handToSlot(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }

    /**
     * 搜索切比雪夫距离内的玩家们
     *
     * @param level 世界
     * @param pos 搜索起点
     * @param radius 距离
     * @return 玩家列表
     */
    public static List<ServerPlayer> searchPlayerByPos(Level level, BlockPos pos, int radius) {
        if (!level.isClientSide) {
            AABB aabb = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()).inflate(radius);
            return level.getEntitiesOfClass(ServerPlayer.class, aabb);
        }
        return List.of();
    }
}
