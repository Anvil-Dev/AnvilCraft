package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class RegisterColorHandlersEventListener {
    @SubscribeEvent
    public static void registerBlockColorHandlersEvent(RegisterColorHandlersEvent.Block event) {
        // 复用原版红石粉的强度到颜色映射，使服务端同步的 POWER 能直接驱动导线覆盖层明暗。
        event.register(
            (state, level, pos, tintIndex) -> tintIndex == 0
                ? RedStoneWireBlock.getColorForPower(state.getValue(RedstoneWireBlock.POWER))
                : -1,
            ModBlocks.REDSTONE_WIRE.get()
        );
    }

    @SubscribeEvent
    public static void registerItemColorHandlersEvent(RegisterColorHandlersEvent.Item event) {
        event.register((itemStack, index) -> {
            PotionContents potionContents = itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (index > 0 || potionContents.potion().isEmpty()) {
                return -1;
            } else {
                return FastColor.ARGB32.opaque(potionContents.getColor());
            }
        }, ModFoodItems.PILL);
    }
}
