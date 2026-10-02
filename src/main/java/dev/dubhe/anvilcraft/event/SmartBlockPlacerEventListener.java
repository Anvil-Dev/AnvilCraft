package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.SmartBlockPlacerFindPointerEvent;
import dev.dubhe.anvilcraft.api.pointer.ITargetPointer;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.init.ModTargetPointers;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class SmartBlockPlacerEventListener {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFindPointer(SmartBlockPlacerFindPointerEvent event) {
        if (event.getOperation() == SmartBlockPlacerBlockEntity.OperationMode.PICKUP) {
            ITargetPointer pointer = ModTargetPointers.BLOCK_ITEM_HANDLER_ONLY_BLOCK_ITEM.get()
                .point(event.getLevel(), event.getSourcePos(), event.getFacing(), event.getRequiredState());
            if (pointer == null) {
                pointer = ModTargetPointers.ITEM_ENTITY_ONLY_BLOCK_ITEM.get()
                    .point(event.getLevel(), event.getSourcePos(), event.getFacing(), event.getRequiredState());
            }
            if (pointer != null) {
                event.setPointer(pointer);
            }
        } else if (event.getOperation() == SmartBlockPlacerBlockEntity.OperationMode.MOVE) {
            ITargetPointer pointer = ModTargetPointers.BLOCK.get()
                .point(event.getLevel(), event.getSourcePos(), event.getFacing(), event.getRequiredState());
            if (pointer != null) {
                event.setPointer(pointer);
            }
        }
    }
}
