package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.EquipmentAbilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class EquipmentAdvancementEventListener {
    /** 虚空行者累计行走距离（米）。 */
    private static final double VOID_WALK_DISTANCE = 100.0;
    private static final Map<UUID, Double> VOID_WALKED = new HashMap<>();
    private static final Map<UUID, Vec3> VOID_LAST_POS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 == 0 && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BREATHING_HELMET)) {
            ModCriterionTriggers.WEAR_BREATHING_HELMET.get().trigger(player);
        }
        if (EquipmentAbilities.hasFullSuit(player)) {
            ModCriterionTriggers.FULL_SPACESUIT.get().trigger(player);
        }
        EquipmentAdvancementEventListener.trackVoidWalking(player);
    }

    /** 穿着全套耐候航天服踩在虚空地面（世界最低高度以下）时累计水平位移。 */
    private static void trackVoidWalking(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean onVoidFloor = !player.isSpectator()
            && !player.getAbilities().flying
            && EquipmentAbilities.hasFullSuit(player)
            && player.getY() <= player.level().getMinBuildHeight() + 0.1;
        if (!onVoidFloor) {
            VOID_WALKED.remove(id);
            VOID_LAST_POS.remove(id);
            return;
        }
        Vec3 current = player.position();
        Vec3 last = VOID_LAST_POS.put(id, current);
        if (last == null) return;
        double horizontal = Math.hypot(current.x - last.x, current.z - last.z);
        // 传送或跨维度时位移会异常大，直接忽略这一帧
        if (horizontal > 4.0) return;
        double walked = VOID_WALKED.getOrDefault(id, 0.0) + horizontal;
        if (walked >= VOID_WALK_DISTANCE) {
            VOID_WALKED.put(id, 0.0);
            ModCriterionTriggers.VOID_WALKER.get().trigger(player);
            return;
        }
        VOID_WALKED.put(id, walked);
    }
}