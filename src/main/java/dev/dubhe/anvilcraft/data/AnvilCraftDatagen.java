package dev.dubhe.anvilcraft.data;

import dev.anvilcraft.lib.v2.integration.IntegrationHook;
import dev.anvilcraft.lib.v2.multiblock.init.LibRegistries;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.advancement.AdvancementHandler;
import dev.dubhe.anvilcraft.data.lang.LangHandler;
import dev.dubhe.anvilcraft.data.provider.ModDamageTypeTagProvider;
import dev.dubhe.anvilcraft.data.provider.ModFurnaceFuelProvider;
import dev.dubhe.anvilcraft.data.provider.ModLootModifierProvider;
import dev.dubhe.anvilcraft.data.provider.ModLootTableProvider;
import dev.dubhe.anvilcraft.data.provider.ModParticleDescriptionProvider;
import dev.dubhe.anvilcraft.data.provider.ModPoiTagProvider;
import dev.dubhe.anvilcraft.data.recipe.RecipeHandler;
import dev.dubhe.anvilcraft.data.tags.TagsHandler;
import dev.dubhe.anvilcraft.init.block.ModMultiblockDefinitions;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRUM;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class AnvilCraftDatagen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        PackOutput packOutput = generator.getPackOutput();

        generator.addProvider(event.includeServer(), new ModLootTableProvider(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), new ModPoiTagProvider(packOutput, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(), new ModFurnaceFuelProvider(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), new ModDamageTypeTagProvider(packOutput, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(), new ModLootModifierProvider(packOutput, lookupProvider, AnvilCraft.MOD_ID));
        generator.addProvider(event.includeClient(), new ModParticleDescriptionProvider(packOutput, existingFileHelper));

        IntegrationHook.setEvent(event);
        AnvilCraft.getINTEGRATION_MANAGER().loadAllDataIntegrations();
    }

    /**
     * 初始化生成器
     */
    public static void init() {
        var genInit = REGISTRUM.getDataGenInitializer();
        genInit.add(Registries.ENCHANTMENT, ModEnchantments::bootstrap);
        genInit.add(Registries.DAMAGE_TYPE, ModDamageTypes::bootstrap);
        genInit.add(LibRegistries.DEFINITIONS_KEY, ModMultiblockDefinitions::bootstrap);

        genInit.addDependency(ProviderType.RECIPE, ProviderType.DYNAMIC);

        REGISTRUM.addDataGenerator(ProviderType.ITEM_TAGS, TagsHandler::initItem);
        REGISTRUM.addDataGenerator(ProviderType.BLOCK_TAGS, TagsHandler::initBlock);
        REGISTRUM.addDataGenerator(ProviderType.FLUID_TAGS, TagsHandler::initFluid);
        REGISTRUM.addDataGenerator(ProviderType.ENCHANTMENT_TAGS, TagsHandler::initEnchantment);
        REGISTRUM.addDataGenerator(ProviderType.DAMAGE_TYPE_TAGS, TagsHandler::initDamageType);
        REGISTRUM.addDataGenerator(ProviderType.ENTITY_TAGS, TagsHandler::initEntityType);
        REGISTRUM.addDataGenerator(ProviderType.LANG, LangHandler::init);
        REGISTRUM.addDataGenerator(ProviderType.RECIPE, RecipeHandler::init);
        REGISTRUM.addDataGenerator(ProviderType.ADVANCEMENT, AdvancementHandler::init);
    }

    public static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike itemLike) {
        return RegistrumRecipeProvider.has(itemLike);
    }

    public static Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> tag) {
        return RegistrumRecipeProvider.has(tag);
    }

    public static String hasItem(TagKey<Item> item) {
        return "has_" + item.location().getPath();
    }

    public static String hasItem(ItemLike item) {
        return "has_" + BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }
}
