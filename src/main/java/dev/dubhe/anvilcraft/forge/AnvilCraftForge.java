package dev.dubhe.anvilcraft.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.config.AnvilCraftConfig;
import dev.dubhe.anvilcraft.event.forge.ClientEventListener;
import dev.dubhe.anvilcraft.init.ModCommands;
import dev.dubhe.anvilcraft.init.forge.ModRecipeTypesForge;
import dev.dubhe.anvilcraft.init.forge.ModVillagers;
import lombok.Getter;
import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.NotNull;

@Mod(AnvilCraft.MOD_ID)
@Getter
public class AnvilCraftForge {
    /**
     * Forge 侧初始化
     */
    public AnvilCraftForge(IEventBus modEventBus, ModContainer container) {
        AnvilCraft.init();
        ModVillagers.register(modEventBus);
        modEventBus.addListener(ModRecipeTypesForge::register);
        NeoForge.EVENT_BUS.addListener(AnvilCraftForge::registerCommand);
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
    }

    public static void registerCommand(@NotNull RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}