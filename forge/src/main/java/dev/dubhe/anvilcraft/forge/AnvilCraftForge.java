package dev.dubhe.anvilcraft.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.config.AnvilCraftConfig;
import dev.dubhe.anvilcraft.event.forge.ClientEventListener;
import dev.dubhe.anvilcraft.init.ModCommands;
import dev.dubhe.anvilcraft.init.forge.ModRecipeTypesForge;
import dev.dubhe.anvilcraft.init.forge.ModVillagers;
import lombok.Getter;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;

@Mod(AnvilCraft.MOD_ID)
@Getter
public class AnvilCraftForge {
    /**
     * Forge 侧初始化
     */
    public AnvilCraftForge() {
        AnvilCraft.init();
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        //bus.addListener(((RegistratorImpl) AnvilCraft.REGISTRATOR)::register);
        bus.addListener(ModVillagers::registerVillagers);
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
    }

    public static void registerCommand(@NotNull RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}