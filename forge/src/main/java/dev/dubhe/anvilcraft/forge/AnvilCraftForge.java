package dev.dubhe.anvilcraft.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModCommands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLConfig;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;

@Mod(AnvilCraft.MOD_ID)
public class AnvilCraftForge {
    private static final File COMMON_CONFIG_FILE = Path.of(FMLConfig.defaultConfigPath()).resolve("anvilcraft-common.json").toFile();
    public AnvilCraftForge() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        AnvilCraft.init(new AnvilCraft.InitSettings(COMMON_CONFIG_FILE));
        bus.addListener(AnvilCraftForge::registerCommand);
    }

    public static void registerCommand(@NotNull RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}