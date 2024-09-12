package dev.dubhe.anvilcraft;

import dev.dubhe.anvilcraft.config.AnvilCraftConfig;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModCommands;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModDispenserBehavior;
import dev.dubhe.anvilcraft.init.ModEntities;
import dev.dubhe.anvilcraft.init.ModEvents;
import dev.dubhe.anvilcraft.init.ModItemGroups;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.ModNetworks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.init.forge.ModVillagers;
import dev.dubhe.anvilcraft.recipe.cache.RecipeCaches;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tterrag.registrate.Registrate;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AnvilCraft.MOD_ID)
public class AnvilCraft {
    public static final String MOD_ID = "anvilcraft";
    public static final String MOD_NAME = "AnvilCraft";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    public static IEventBus EVENT_BUS;
    public static AnvilCraftConfig config =
            AutoConfig.register(AnvilCraftConfig.class, JanksonConfigSerializer::new).getConfig();

    public static final Registrate REGISTRATE = Registrate.create(MOD_ID);

    public AnvilCraft(IEventBus modEventBus, ModContainer container) {
        EVENT_BUS = modEventBus;
        ModEvents.register();
        ModItemGroups.register(modEventBus);
        ModBlocks.register();
        ModEntities.register();
        ModItems.register();
        ModBlockEntities.register();
        ModMenuTypes.register();
        ModDispenserBehavior.register();
        ModComponents.register(modEventBus);
        ModVillagers.register(modEventBus);
        ModRecipeTypes.register(modEventBus);
        // datagen
        AnvilCraftDatagen.init();

        registerEvents(modEventBus);

        ModLoadingContext.get()
                .registerExtensionPoint(
                        IConfigScreenFactory.class,
                        () -> ((c, screen) ->
                                AutoConfig.getConfigScreen(AnvilCraftConfig.class, screen).get()));
    }

    private static void registerEvents(IEventBus eventBus) {
        NeoForge.EVENT_BUS.addListener(AnvilCraft::registerCommand);
        NeoForge.EVENT_BUS.addListener(AnvilCraft::addReloadListeners);

        eventBus.addListener(AnvilCraft::registerPayload);
    }

    public static @NotNull ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void registerCommand(@NotNull RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    public static void registerPayload(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        ModNetworks.init(registrar);
    }

    public static void addReloadListeners(AddReloadListenerEvent event) {
        RecipeManager recipeManager = event.getServerResources().getRecipeManager();
        event.addListener(
                ((prepBarrier,
                        resourceManager,
                        prepProfiler,
                        reloadProfiler,
                        backgroundExecutor,
                        gameExecutor) -> prepBarrier
                        .wait(Unit.INSTANCE)
                        .thenRunAsync(
                                () -> {
                                    RecipeCaches.reload(recipeManager);
                                },
                                gameExecutor)));
    }
}
