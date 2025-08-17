package dev.dubhe.anvilcraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tterrag.registrate.Registrate;
import dev.dubhe.anvilcraft.api.integration.IntegrationManager;
import dev.dubhe.anvilcraft.api.taslatower.TeslaFilter;
import dev.dubhe.anvilcraft.api.tooltip.ItemTooltipManager;
import dev.dubhe.anvilcraft.config.AnvilCraftConfig;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.dfu.AnvilCraftDfu;
import dev.dubhe.anvilcraft.init.ModAmuletTypes;
import dev.dubhe.anvilcraft.init.ModAttatchments;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModCommands;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModCustomDataComponents;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.ModDispenserBehavior;
import dev.dubhe.anvilcraft.init.ModEnchantmentEffectComponents;
import dev.dubhe.anvilcraft.init.ModEnchantmentEffects;
import dev.dubhe.anvilcraft.init.ModEntities;
import dev.dubhe.anvilcraft.init.ModFluids;
import dev.dubhe.anvilcraft.init.ModInspections;
import dev.dubhe.anvilcraft.init.ModItemGroups;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModLootContextParamSets;
import dev.dubhe.anvilcraft.init.ModLootItemConditions;
import dev.dubhe.anvilcraft.init.ModLootItemFunctions;
import dev.dubhe.anvilcraft.init.ModLootModifiers;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.ModMobEffects;
import dev.dubhe.anvilcraft.init.ModNetworks;
import dev.dubhe.anvilcraft.init.ModParticles;
import dev.dubhe.anvilcraft.init.ModRecipeOutcomeTypes;
import dev.dubhe.anvilcraft.init.ModRecipePredicateTypes;
import dev.dubhe.anvilcraft.init.ModRecipeTriggers;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.init.ModResultModifierTypes;
import dev.dubhe.anvilcraft.init.ModVillagers;
import dev.dubhe.anvilcraft.integration.top.AnvilCraftTopPlugin;
import dev.dubhe.anvilcraft.recipe.anvil.cache.RecipeCaches;
import dev.dubhe.anvilcraft.util.ModInteractionMap;
import dev.dubhe.anvilcraft.util.Util;
import lombok.Getter;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
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
    public static IEventBus MOD_BUS = null;
    public static AnvilCraftConfig config = AutoConfig.register(AnvilCraftConfig.class, JanksonConfigSerializer::new)
        .getConfig();

    @Getter
    private static final IntegrationManager integrationManager = new IntegrationManager();

    public static final Registrate REGISTRATE = Registrate.create(MOD_ID);

    public AnvilCraft(IEventBus modEventBus) {
        MOD_BUS = modEventBus;
        ModAttatchments.register(modEventBus);
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
        integrationManager.compileContent();
        integrationManager.loadAllIntegrations();
        StartupNotificationManager.addModMessage("[AnvilCraft] Ciallo~");
        AnvilCraftDfu.constructAndOptimize();
        LOGGER.info("Ciallo～(∠・ω< )⌒★");
        LOGGER.info("let's 0721");

        ModRecipeTriggers.TRIGGER.register(modEventBus);
        ModRecipePredicateTypes.PREDICATE_TYPE.register(modEventBus);
        ModRecipeOutcomeTypes.OUTCOME_TYPE.register(modEventBus);
        ModResultModifierTypes.register(modEventBus);
        ModCustomDataComponents.register(modEventBus);
    }

    private static void registerEvents(@NotNull IEventBus eventBus) {
        NeoForge.EVENT_BUS.addListener(AnvilCraft::registerCommand);
        NeoForge.EVENT_BUS.addListener(AnvilCraft::addReloadListeners);
        NeoForge.EVENT_BUS.addListener(AnvilCraft::addItemTooltips);

        eventBus.addListener(AnvilCraft::registerPayload);
        eventBus.addListener(AnvilCraft::loadComplete);
        eventBus.addListener(ModFluids::registerFluidInteractions);
    }

    public static @NotNull ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void registerCommand(@NotNull RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    public static void registerPayload(@NotNull RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        ModNetworks.init(registrar);
    }

    public static void addItemTooltips(@NotNull ItemTooltipEvent event) {
        ItemTooltipManager.addTooltip(event.getItemStack(), event.getToolTip());
    }

    public static void addReloadListeners(@NotNull AddReloadListenerEvent event) {
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

    public static void loadComplete(@NotNull FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            ModDispenserBehavior.register();
            ModInteractionMap.initInteractionMap();
            if (Util.isLoaded("theoneprobe")) {
                LOGGER.info("TheOneProbe found. Loading AnvilCraft TheOneProbe plugin...");
                AnvilCraftTopPlugin.init();
            }
            if (Util.isLoaded("apothic_enchanting")) {
                LOGGER.info(
                    "Apothic Enchanting found. Set royalAnvilBeyondMaxLevel, "
                        + "emberAnvilBeyondMaxLevel and transcendenceAnvilBeyondMaxLevel to true."
                );
                config.royalAnvilBeyondMaxLevel = true;
                config.emberAnvilBeyondMaxLevel = true;
                config.transcendenceAnvilBeyondMaxLevel = true;
            }
        });
    }
}
