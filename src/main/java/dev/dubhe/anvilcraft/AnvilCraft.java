package dev.dubhe.anvilcraft;

import dev.dubhe.anvilcraft.api.tooltip.ItemTooltipManager;
import dev.dubhe.anvilcraft.config.AnvilCraftConfig;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModCommands;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModDispenserBehavior;
import dev.dubhe.anvilcraft.init.ModEnchantmentEffectComponents;
import dev.dubhe.anvilcraft.init.ModEnchantmentEffects;
import dev.dubhe.anvilcraft.init.ModEntities;
import dev.dubhe.anvilcraft.init.ModFluids;
import dev.dubhe.anvilcraft.init.ModInspections;
import dev.dubhe.anvilcraft.init.ModItemGroups;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModLootContextParamSets;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.ModNetworks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.init.ModVillagers;
import dev.dubhe.anvilcraft.integration.top.AnvilCraftTopPlugin;
import dev.dubhe.anvilcraft.recipe.anvil.cache.RecipeCaches;
import dev.dubhe.anvilcraft.util.ModInteractionMap;
import dev.dubhe.anvilcraft.util.Util;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.util.Unit;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
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
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    public static final Gson GSON =
        new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    public static AnvilCraftConfig config = AutoConfig.register(AnvilCraftConfig.class, JanksonConfigSerializer::new)
        .getConfig();

    public static final Registrate REGISTRATE = Registrate.create(MOD_ID);

    public AnvilCraft(IEventBus modEventBus, ModContainer container) {
        ModItemGroups.register(modEventBus);
        ModBlocks.register();
        ModFluids.register(modEventBus);
        ModEntities.register();
        ModItems.register();
        ModBlockEntities.register();
        ModMenuTypes.register();
        ModDispenserBehavior.register();
        ModComponents.register(modEventBus);
        ModVillagers.register(modEventBus);
        ModRecipeTypes.register(modEventBus);
        ModInspections.initialize();

        ModLootContextParamSets.registerAll();
        ModEnchantmentEffectComponents.register(modEventBus);
        ModEnchantmentEffects.register(modEventBus);
        // datagen
        AnvilCraftDatagen.init();

        registerEvents(modEventBus);
    }

    private static void registerEvents(IEventBus eventBus) {
        NeoForge.EVENT_BUS.addListener(AnvilCraft::registerCommand);
        NeoForge.EVENT_BUS.addListener(AnvilCraft::addReloadListeners);
        NeoForge.EVENT_BUS.addListener(AnvilCraft::addItemTooltips);

        eventBus.addListener(AnvilCraft::registerPayload);
        eventBus.addListener(AnvilCraft::loadComplete);
        eventBus.addListener(AnvilCraft::packSetup);
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

    public static void addItemTooltips(ItemTooltipEvent event) {
        ItemTooltipManager.addTooltip(event.getItemStack(), event.getToolTip());
    }

    public static void addReloadListeners(AddReloadListenerEvent event) {
        RecipeManager recipeManager = event.getServerResources().getRecipeManager();
        event.addListener(
            (
                prepBarrier,
                resourceManager,
                prepProfiler, reloadProfiler,
                backgroundExecutor,
                gameExecutor
            ) -> prepBarrier
                .wait(Unit.INSTANCE)
                .thenRunAsync(() -> RecipeCaches.reload(recipeManager), gameExecutor)
        );
    }

    public static void loadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            ModInteractionMap.initInteractionMap();
            if (Util.isLoaded("theoneprobe")) {
                LOGGER.info("TheOneProbe found. Loading AnvilCraft TheOneProbe plugin...");
                AnvilCraftTopPlugin.init();
            }
        });
    }

    public static void packSetup(AddPackFindersEvent event) {
        event.addPackFinders(
            of("resourcepacks/transparent_cauldron"),
            PackType.CLIENT_RESOURCES,
            Component.translatable("pack.anvilcraft.builtin_pack"),
            PackSource.BUILT_IN,
            false,
            Pack.Position.TOP);
    }
}
