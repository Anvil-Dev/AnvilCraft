package dev.dubhe.anvilcraft.forge;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.config.AnvilCraftConfig;
import dev.dubhe.anvilcraft.event.forge.ClientEventListener;
import dev.dubhe.anvilcraft.init.ModCommands;
import dev.dubhe.anvilcraft.init.forge.ModRecipeTypesForge;
import dev.dubhe.anvilcraft.init.forge.ModVillagers;
import dev.dubhe.anvilcraft.util.AnvilCraftCustomInfo;
import lombok.Getter;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.language.IModInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mod(AnvilCraft.MOD_ID)
@Getter
public class AnvilCraftForge {
    /**
     * Forge 侧初始化
     */
    public AnvilCraftForge() {
        AnvilCraft.init();
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModVillagers.register(bus);
        bus.addListener(ModRecipeTypesForge::register);
        MinecraftForge.EVENT_BUS.addListener(AnvilCraftForge::registerCommand);
        try {
            ClientEventListener ignore = new ClientEventListener();
        } catch (NoSuchMethodError ignore) {
            AnvilCraft.LOGGER.debug("Server");
        }
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (mc, screen) -> AutoConfig.getConfigScreen(AnvilCraftConfig.class, screen).get()
            )
        );
        for (IModInfo mod : ModList.get().getMods()) {
            Map<String, Object> modProperties = mod.getModProperties();
            for (Map.Entry<String, Object> entry : modProperties.entrySet()) {
                if (!entry.getKey().equals("anvilcraft")) continue;
                if (!(entry.getValue() instanceof UnmodifiableConfig anvilConfig)) return;
                for (UnmodifiableConfig.Entry entry1 : anvilConfig.entrySet()) {
                    if (!entry1.getKey().equals("integrations")) continue;
                    if (!(entry1.getValue() instanceof UnmodifiableConfig config)) return;
                    loadIntegrations(config);
                }
            }
        }
        AnvilCraftCustomInfo.apply();
    }

    private static void loadIntegrations(@NotNull UnmodifiableConfig integrations) {
        for (UnmodifiableConfig.Entry entry2 : integrations.entrySet()) {
            String modid = entry2.getKey();
            Object value = entry2.getValue();
            List<String> classes = Collections.synchronizedList(new ArrayList<>());
            if (value instanceof String string) {
                classes.add(string);
            } else if (value instanceof List<?> list) {
                list.stream()
                    .filter(i -> i instanceof String)
                    .map(Object::toString)
                    .forEach(classes::add);
            }
            AnvilCraftCustomInfo.INTEGRATIONS.put(modid, classes);
        }
    }

    public static void registerCommand(@NotNull RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}