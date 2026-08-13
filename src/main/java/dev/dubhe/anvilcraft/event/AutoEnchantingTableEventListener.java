package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.PrimerEnchantmentsEvent;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 自动附魔台引物附魔事件监听器。
 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class AutoEnchantingTableEventListener {
    @SubscribeEvent
    public static void onPrimerEnchantments(PrimerEnchantmentsEvent event) {
        // 绿宝石护符：提供所有可通过村民交易获得的附魔
        if (event.getPrimer().is(ModItems.EMERALD_AMULET.get())) {
            var registry = event.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            registry.getTag(EnchantmentTags.TRADEABLE)
                .ifPresent(tag -> event.addEnchantments(tag.stream().toList()));
        }
    }
}
