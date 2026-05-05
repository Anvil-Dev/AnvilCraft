package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

import java.util.Optional;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class PlayerWearAnvilHammerEventListener {
    @SubscribeEvent
    public static void onPlayerWearAnvilHammer(LivingEquipmentChangeEvent event) {
        LivingEntity entity = event.getEntity();
        EquipmentSlot slot = event.getSlot();
        ItemStack eventTo = event.getTo();
        if (entity instanceof Player && slot == EquipmentSlot.HEAD && eventTo.getItem() instanceof AnvilHammerItem) {
            Optional<PowerGrid> powerGrid = PowerGrid.findPowerGridContains(entity.level(), entity.position());
            if (powerGrid.isPresent() && powerGrid.get().isWorking()) {
                TriggerUtil.playerWearAnvilHammer(entity.level(), BlockPos.containing(entity.position()));
            }
        }
    }
}
