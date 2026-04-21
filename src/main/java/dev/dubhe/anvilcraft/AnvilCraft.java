package dev.dubhe.anvilcraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.lib.v2.integration.IntegrationHook;
import dev.anvilcraft.lib.v2.integration.IntegrationManager;
import dev.anvilcraft.lib.v2.network.register.NetworkRegistrar;
import dev.anvilcraft.lib.v2.registrum.Registrum;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.taslatower.TeslaFilter;
import dev.dubhe.anvilcraft.api.tooltip.ItemTooltipManager;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
import dev.dubhe.anvilcraft.config.AnvilCraftServerConfig;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.dfu.AnvilCraftDfu;
import dev.dubhe.anvilcraft.init.ModAttachments;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.ModDispenserBehavior;
import dev.dubhe.anvilcraft.init.ModInspections;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.ModMobEffects;
import dev.dubhe.anvilcraft.init.ModParticles;
import dev.dubhe.anvilcraft.init.ModUuidProviders;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.command.ModCommands;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantmentEffectComponents;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantmentEffects;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import dev.dubhe.anvilcraft.init.entity.ModVillagers;
import dev.dubhe.anvilcraft.init.item.ModAmuletTypes;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModCustomDataComponents;
import dev.dubhe.anvilcraft.init.item.ModItemGroups;
import dev.dubhe.anvilcraft.init.item.ModItemSubPredicates;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.loot.ModLootContextParamSets;
import dev.dubhe.anvilcraft.init.loot.ModLootItemConditions;
import dev.dubhe.anvilcraft.init.loot.ModLootItemFunctions;
import dev.dubhe.anvilcraft.init.loot.ModLootModifiers;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeInits;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.init.recipe.ModResultModifierTypes;
import dev.dubhe.anvilcraft.recipe.anvil.cache.RecipeCaches;
import dev.dubhe.anvilcraft.util.ModInteractionMap;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AnvilCraft.MOD_ID)
public class AnvilCraft {
    public static final String MOD_ID = "anvilcraft";
    public static final String MOD_NAME = "AnvilCraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    public static IEventBus MOD_BUS = null;
    public static final AnvilCraftServerConfig CONFIG = ConfigManager.register(AnvilCraft.MOD_ID, AnvilCraftServerConfig::new);
    public static final AnvilCraftClientConfig CLIENT_CONFIG = ConfigManager.register(AnvilCraft.MOD_ID, AnvilCraftClientConfig::new);

    @Getter
    private static final IntegrationManager INTEGRATION_MANAGER = new IntegrationManager(AnvilCraft.MOD_ID);

    public static final Registrum REGISTRUM = Registrum.create(MOD_ID);

    public AnvilCraft(IEventBus modEventBus, ModContainer modContainer) {
        MOD_BUS = modEventBus;
        ModAttachments.register(modEventBus);
        ModItemGroups.register(modEventBus);
        ModBlocks.register();
        ModFluids.register(modEventBus);
        ModEntities.register();
        ModItems.register();
        ModBlockEntities.register();
        ModMenuTypes.register();
        ModComponents.register(modEventBus);
        ModVillagers.register(modEventBus);
        ModRecipeTypes.register(modEventBus);
        ModDataAttachments.register(modEventBus);
        ModParticles.register(modEventBus);
        ModMobEffects.register(modEventBus);
        ModInspections.initialize();
        ModItemSubPredicates.initialize(modEventBus);
        ModUuidProviders.register(modEventBus);

        ModCriterionTriggers.register(modEventBus);
        ModLootContextParamSets.registerAll();
        ModEnchantmentEffectComponents.register(modEventBus);
        ModEnchantmentEffects.register(modEventBus);
        ModLootItemFunctions.LOOT_FUNCTION_TYPES.register(modEventBus);
        ModLootItemConditions.LOOT_CONDITION_TYPES.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        TeslaFilter.init();
        ModAmuletTypes.register(modEventBus);
        // datagen
        AnvilCraftDatagen.init();

        registerEvents(modEventBus);
        StartupNotificationManager.addModMessage("[AnvilCraft] Loading Integrations");
        IntegrationHook.setModEventBus(modEventBus);
        IntegrationHook.setModContainer(modContainer);
        INTEGRATION_MANAGER.compileContent();
        INTEGRATION_MANAGER.loadAllIntegrations();
        StartupNotificationManager.addModMessage("[AnvilCraft] Ciallo~");
        AnvilCraftDfu.constructAndOptimize();
        LOGGER.info("Ciallo～(∠・ω< )⌒★");
        LOGGER.info("let's 0721");

        ModRecipeInits.init(modEventBus);

        ModResultModifierTypes.register(modEventBus);
        ModCustomDataComponents.register(modEventBus);
    }

    private static void registerEvents(IEventBus eventBus) {
        NeoForge.EVENT_BUS.addListener(AnvilCraft::registerCommand);
        NeoForge.EVENT_BUS.addListener(AnvilCraft::addReloadListeners);
        NeoForge.EVENT_BUS.addListener(AnvilCraft::addItemTooltips);

        eventBus.addListener(AnvilCraft::registerPayload);
        eventBus.addListener(AnvilCraft::loadComplete);
        eventBus.addListener(ModFluids::registerFluidInteractions);
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static ResourceLocation advancementOf(String path) {
        return of("anvilcraft/" + path);
    }

    public static void registerCommand(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    public static void registerPayload(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        NetworkRegistrar.register(registrar, AnvilCraft.MOD_ID);
    }

    public static void addItemTooltips(ItemTooltipEvent event) {
        ItemTooltipManager.addTooltip(event.getItemStack(), event.getToolTip());
    }

    public static void addReloadListeners(AddReloadListenerEvent event) {
        RecipeManager recipeManager = event.getServerResources().getRecipeManager();
        event.addListener((
            prepBarrier,
            resourceManager,
            prepProfiler,
            reloadProfiler,
            backgroundExecutor,
            gameExecutor
        ) -> prepBarrier.wait(Unit.INSTANCE)
            .thenRunAsync(() -> RecipeCaches.reload(recipeManager), gameExecutor));
    }

    public static void loadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            ModDispenserBehavior.register();
            ModInteractionMap.initInteractionMap();
            if (Util.isLoaded("apothic_enchanting")) {
                LOGGER.info(
                    "Apothic Enchanting found. Set royalAnvilBeyondMaxLevel, "
                    + "emberAnvilBeyondMaxLevel and transcendenceAnvilBeyondMaxLevel to true."
                );
                CONFIG.royalAnvilBeyondMaxLevel = true;
                CONFIG.emberAnvilBeyondMaxLevel = true;
                CONFIG.transcendenceAnvilBeyondMaxLevel = true;
            }
        });
    }
}
