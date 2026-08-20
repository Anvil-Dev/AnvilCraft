package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.ModCategoryInitEvent;
import dev.dubhe.anvilcraft.init.storage.ModCategories;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.NamespaceCategory;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class CategoryInitEventListener {
    private static final Set<ICategory> ALL_MODS_CATEGORIES = new HashSet<>();

    public static Set<ICategory> getAllModsCategories() {
        return new HashSet<>(ALL_MODS_CATEGORIES);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSetup(ServerStartedEvent event) {
        RegistryAccess.Frozen registries = event.getServer().registryAccess();

        // 从事件获取预设类别
        Map<String, ICategory> defaultCategories = NeoForge.EVENT_BUS.post(new ModCategoryInitEvent(registries)).getCategories();
        CategoryInitEventListener.ALL_MODS_CATEGORIES.addAll(defaultCategories.values());

        Optional<Registry<Item>> registryOp = registries.registry(Registries.ITEM);
        if (registryOp.isEmpty()) {
            return;
        }
        Registry<Item> itemRegistry = registryOp.get();
        Map<String, ItemStack> firstItemByNamespace = new HashMap<>();
        Iterator<Holder.Reference<Item>> itemIt = itemRegistry.holders()
            .sorted(Comparator.comparing(Holder.Reference::key))
            .iterator();
        while (itemIt.hasNext()) {
            Holder.Reference<Item> itemRef = itemIt.next();
            String namespace = itemRef.key().location().getNamespace();
            firstItemByNamespace.putIfAbsent(namespace, itemRef.value().getDefaultInstance());
        }

        Optional<Registry<CreativeModeTab>> tabRegistryOp = registries.registry(Registries.CREATIVE_MODE_TAB);
        if (tabRegistryOp.isEmpty()) {
            return;
        }
        Registry<CreativeModeTab> tabRegistry = tabRegistryOp.get();
        Iterator<Holder.Reference<CreativeModeTab>> iterator = tabRegistry.holders()
            .sorted(Comparator.comparing(Holder.Reference::key))
            .iterator();

        // 从创造模式标签页提取信息
        Map<String, ItemStack> cache = new HashMap<>();
        while (iterator.hasNext()) {
            Holder.Reference<CreativeModeTab> ref = iterator.next();
            String namespace = ref.key().location().getNamespace();
            if (defaultCategories.containsKey(namespace)) {
                continue;
            }
            if (cache.containsKey(namespace)) {
                continue;
            }
            cache.put(namespace, CategoryInitEventListener.resolveIcon(ref.value(), namespace, firstItemByNamespace));
        }

        // 提取没有创造模式标签页的模组
        Set<String> missingIds = new HashSet<>();
        for (IModInfo mod : ModList.get().getMods()) {
            String modId = mod.getModId();
            if (defaultCategories.containsKey(modId)) {
                continue;
            }
            if (cache.containsKey(modId)) {
                continue;
            }
            missingIds.add(modId);
        }

        for (String missingId : missingIds) {
            ItemStack icon = firstItemByNamespace.getOrDefault(missingId, ItemStack.EMPTY);
            if (!icon.isEmpty()) {
                cache.put(missingId, icon);
            }
        }

        for (Map.Entry<String, ItemStack> entry : cache.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            CategoryInitEventListener.ALL_MODS_CATEGORIES.add(new NamespaceCategory(entry.getValue(), entry.getKey()));
        }
    }

    private static ItemStack resolveIcon(
        CreativeModeTab tab,
        String namespace,
        Map<String, ItemStack> firstItemByNamespace
    ) {
        try {
            ItemStack icon = tab.getIconItem();
            if (!icon.isEmpty()) {
                return icon;
            }
        } catch (Exception e) {
            // 某些模组的 getIconItem 依赖创造界面已打开或客户端上下文（如 Draconic Evolution 的 CyclingTab），
            // 服务端启动阶段调用可能抛异常，降级为命名空间下首个物品的默认实例
            AnvilCraft.LOGGER.warn("Failed to get icon from creative tab {}: {}", namespace, e);
        }
        return firstItemByNamespace.getOrDefault(namespace, ItemStack.EMPTY);
    }

    @SubscribeEvent
    public static void registerDefault(ModCategoryInitEvent event) {
        event.register(ResourceLocation.DEFAULT_NAMESPACE, event.get(ModCategories.MINECRAFT));
        event.register(AnvilCraft.MOD_ID, event.get(ModCategories.ANVILCRAFT));
    }
}
