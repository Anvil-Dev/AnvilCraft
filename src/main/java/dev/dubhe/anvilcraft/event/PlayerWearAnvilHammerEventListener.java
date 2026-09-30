package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class PlayerWearAnvilHammerEventListener {
    @SubscribeEvent
    public static void onPlayerWearAnvilHammer(LivingEquipmentChangeEvent event) {
        if (event.getSlot() == EquipmentSlot.HEAD) tryTrigger(event.getEntity(), event.getTo());
    }

    public static void tryTrigger(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof ServerPlayer) || !(stack.getItem() instanceof AnvilHammerItem)) return;
        if (PowerGrid.findPowerGridContains(entity.level(), entity.position()).filter(PowerGrid::isWorking).isPresent()) {
            TriggerUtil.playerWearAnvilHammer(entity.level(), BlockPos.containing(entity.position()));
        }
    }
}
