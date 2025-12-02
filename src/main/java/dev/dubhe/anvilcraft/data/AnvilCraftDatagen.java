package dev.dubhe.anvilcraft.data;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.anvilcraft.lib.integration.IntegrationHook;
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

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
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
        var genInit = REGISTRATE.getDataGenInitializer();
        genInit.add(Registries.ENCHANTMENT, ModEnchantments::bootstrap);
        genInit.add(Registries.DAMAGE_TYPE, ModDamageTypes::bootstrap);

        genInit.addDependency(ProviderType.RECIPE, ProviderType.DYNAMIC);

        REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, TagsHandler::initItem);
        REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, TagsHandler::initBlock);
        REGISTRATE.addDataGenerator(ProviderType.FLUID_TAGS, TagsHandler::initFluid);
        REGISTRATE.addDataGenerator(ProviderType.ENCHANTMENT_TAGS, TagsHandler::initEnchantment);
        REGISTRATE.addDataGenerator(
            ProviderType.registerDynamicTag("tags/damage_type", "damage_type", Registries.DAMAGE_TYPE),
            TagsHandler::initDamageType);
        REGISTRATE.addDataGenerator(ProviderType.ENTITY_TAGS, TagsHandler::initEntityType);
        REGISTRATE.addDataGenerator(ProviderType.LANG, LangHandler::init);
        REGISTRATE.addDataGenerator(ProviderType.RECIPE, RecipeHandler::init);
        REGISTRATE.addDataGenerator(ProviderType.ADVANCEMENT, AdvancementHandler::init);
    }

    public static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike itemLike) {
        return RegistrateRecipeProvider.has(itemLike);
    }

    public static Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> tag) {
        return RegistrateRecipeProvider.has(tag);
    }

    public static String hasItem(TagKey<Item> item) {
        return "has_" + item.location().getPath();
    }

    public static String hasItem(ItemLike item) {
        return "has_" + BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }
}
