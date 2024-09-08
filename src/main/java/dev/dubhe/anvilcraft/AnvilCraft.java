package dev.dubhe.anvilcraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.registry.AnvilCraftRegistrate;
import dev.dubhe.anvilcraft.config.AnvilCraftConfig;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModDispenserBehavior;
import dev.dubhe.anvilcraft.init.ModEntities;
import dev.dubhe.anvilcraft.init.ModEvents;
import dev.dubhe.anvilcraft.init.ModItemGroups;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.ModResourcePacks;
import dev.dubhe.anvilcraft.util.EnchantmentDisableUtil;
import dev.dubhe.anvilcraft.util.Lazy;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnvilCraft {
    public static final String MOD_ID = "anvilcraft";
    public static final String MOD_NAME = "AnvilCraft";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    public static final IEventBus EVENT_BUS = BusBuilder.builder()
            .checkTypesOnDispatch()
            .markerType(SubscribeEvent.class)
            .setExceptionHandler((bus, event, listeners, index, throwable) -> LOGGER.error(
                    "An exception was thrown from {} while dispatching event {} on {}",
                    listeners[index],
                    event,
                    bus,
                    throwable
            ))
            .build();

    public static AnvilCraftConfig config = AutoConfig
            .register(AnvilCraftConfig.class, JanksonConfigSerializer::new)
            .getConfig();
    public static IEventBus MOD_EVENT_BUS;
    // EnchantmentDisable
    public static final Lazy<EnchantmentDisableUtil> enchantmentDisableUtil = new Lazy<>(EnchantmentDisableUtil::new);

    public static final AnvilCraftRegistrate REGISTRATE = AnvilCraftRegistrate.create(MOD_ID);

    /**
     * 初始化函数
     */
    public static void init(IEventBus bus) {
        MOD_EVENT_BUS = bus;
        // common
        ModEvents.register();
        ModBlocks.register();
        ModEntities.register();
        ModItems.register();
        ModItemGroups.register();
        ModBlockEntities.register();
        ModMenuTypes.register();
        ModDispenserBehavior.register();
        ModResourcePacks.register();
        ModComponents.register(bus);

        AnvilRecipe.init();
        // datagen
        AnvilCraftDatagen.init();
        // fabric 独有，请在此之前插入注册
        // 现在没有了
        REGISTRATE.registerRegistrate(bus);
    }

    public static @NotNull ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static EnchantmentDisableUtil getEnchantmentDisableUtil() {
        return AnvilCraft.enchantmentDisableUtil.get();
    }
}
