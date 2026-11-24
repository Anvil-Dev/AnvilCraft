package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.EquipmentAbilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class EquipmentAdvancementEventListener {
    private static final double VOID_WALK_DISTANCE = 100;

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getSlot() == EquipmentSlot.HEAD && event.getTo().is(ModItems.BREATHING_HELMET)) {
            ModCriterionTriggers.WEAR_BREATHING_HELMET.get().trigger(player);
        }
        if (event.getSlot().isArmor() && EquipmentAbilities.hasFullSuit(player)) {
            ModCriterionTriggers.FULL_SPACESUIT.get().trigger(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BREATHING_HELMET)) {
            ModCriterionTriggers.WEAR_BREATHING_HELMET.get().trigger(player);
        }
        if (EquipmentAbilities.hasFullSuit(player)) {
            ModCriterionTriggers.FULL_SPACESUIT.get().trigger(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        event.getEntity().removeData(ModDataAttachments.LAST_VOID_POSITION);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.onGround() || player.isPassenger() || Math.abs(player.getY() - player.level().getMinBuildHeight()) > 1e-5
            || !EquipmentAbilities.isVoidProtected(player)) {
            player.removeData(ModDataAttachments.LAST_VOID_POSITION);
            return;
        }
        Vec3 position = player.position();
        Vec3 previous = player.setData(ModDataAttachments.LAST_VOID_POSITION, position);
        if (previous == null) {
            return;
        }
        double distance = Math.hypot(position.x - previous.x, position.z - previous.z);
        if (distance == 0 || distance > 4) {
            return;
        }
        double walked = player.getData(ModDataAttachments.VOID_WALK_DISTANCE) + distance;
        if (walked >= VOID_WALK_DISTANCE) {
            walked -= VOID_WALK_DISTANCE;
            ModCriterionTriggers.VOID_WALKER.get().trigger(player);
        }
        player.setData(ModDataAttachments.VOID_WALK_DISTANCE, walked);
    }
}
