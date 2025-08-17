package dev.dubhe.anvilcraft.api.totem;

import dev.dubhe.anvilcraft.api.totem.handler.AmuletBoxHandler;
import dev.dubhe.anvilcraft.api.totem.handler.TotemHandler;
import dev.dubhe.anvilcraft.api.totem.handler.TotemOfRageHandler;
import dev.dubhe.anvilcraft.api.totem.handler.TotemOfRecoveryHandler;
import dev.dubhe.anvilcraft.api.totem.handler.TotemOfUndyingHandler;
import dev.dubhe.anvilcraft.init.ModItems;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Map;

/**
 * 用于管理图腾
 */
public class TotemManager {
    public static final TotemManager INSTANCE = new TotemManager();

    @Getter
    private final Map<Item, TotemHandler> totemMap = new Object2ObjectOpenHashMap<>();

    private TotemManager() {
        registerTotem(ModItems.AMULET_BOX.asItem(), new AmuletBoxHandler());
        registerTotem(Items.TOTEM_OF_UNDYING, new TotemOfUndyingHandler());
        registerTotem(ModItems.TOTEM_OF_RECOVERY.get(), new TotemOfRecoveryHandler());
        registerTotem(ModItems.TOTEM_OF_RAGE.get(), new TotemOfRageHandler());
    }

    public void registerTotem(Item item, TotemHandler handler) {
        totemMap.put(item, handler);
    }
}
