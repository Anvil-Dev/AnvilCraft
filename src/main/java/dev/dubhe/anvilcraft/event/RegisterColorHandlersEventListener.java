package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.RedstoneWireClientPowerCache;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class RegisterColorHandlersEventListener {
    @SubscribeEvent
    public static void registerBlockColorHandlersEvent(RegisterColorHandlersEvent.Block event) {
        // 复用原版红石粉的强度到颜色映射，功率来自服务端网络管理器同步的客户端缓存。
        event.register(
            (state, level, pos, tintIndex) -> {
                if (tintIndex != 0) {
                    return -1;
                }
                return RedStoneWireBlock.getColorForPower(
                    pos == null ? 0 : RedstoneWireClientPowerCache.getCurrent(pos)
                );
            },
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

        event.register(new DynamicFluidContainerModel.Colors(),
            ModItems.HYDROGEN_BUCKET,
            ModItems.OXYGEN_BUCKET,
            ModItems.HELIUM_BUCKET,
            ModItems.DEUTERIUM_BUCKET);
    }
}
