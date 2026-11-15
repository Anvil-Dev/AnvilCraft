package dev.dubhe.anvilcraft.data;

import dev.anvilcraft.lib.v2.integration.IntegrationHook;
import dev.anvilcraft.lib.v2.multiblock.init.LibRegistries;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumBlockModelGenerator;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.advancement.ModAdvancementsHandler;
import dev.dubhe.anvilcraft.data.lang.LangHandler;
import dev.dubhe.anvilcraft.data.provider.ModBlockPlacementRuleProvider;
import dev.dubhe.anvilcraft.data.provider.ModFurnaceFuelProvider;
import dev.dubhe.anvilcraft.data.provider.ModLootModifierProvider;
import dev.dubhe.anvilcraft.data.provider.ModLootTableProvider;
import dev.dubhe.anvilcraft.data.provider.ModParticleDescriptionProvider;
import dev.dubhe.anvilcraft.data.provider.ModSoundDefinitionsProvider;
import dev.dubhe.anvilcraft.data.recipe.RecipeHandler;
import dev.dubhe.anvilcraft.data.tags.TagsHandler;
import dev.dubhe.anvilcraft.init.block.ModMultiblockDefinitions;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.init.entity.ModTradeSets;
import dev.dubhe.anvilcraft.init.entity.ModVillagerTrades;
import dev.dubhe.anvilcraft.init.item.ModAmuletDefinitions;
import dev.dubhe.anvilcraft.init.recipe.ModMathFunctions;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.init.storage.ModCategories;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
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
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRUM;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class AnvilCraftDatagen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();

        generator.addProvider(true, new ModParticleDescriptionProvider(packOutput));
        generator.addProvider(true, new ModSoundDefinitionsProvider(packOutput));
        generator.addProvider(true, new ModLootTableProvider(packOutput, event.getLookupProvider()));
        generator.addProvider(true, new ModFurnaceFuelProvider(packOutput, event.getLookupProvider()));
        generator.addProvider(true, new ModLootModifierProvider(packOutput, event.getLookupProvider()));
        generator.addProvider(true, new ModBlockPlacementRuleProvider(packOutput));

        IntegrationHook.setEvent(event);
        AnvilCraft.getINTEGRATION_MANAGER().loadAllClientDataIntegrations();
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Server event) {
        DataGenerator generator = event.getGenerator();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        PackOutput packOutput = generator.getPackOutput();

        generator.addProvider(true, new ModLootTableProvider(packOutput, lookupProvider));
        generator.addProvider(true, new ModFurnaceFuelProvider(packOutput, lookupProvider));
        generator.addProvider(true, new ModLootModifierProvider(packOutput, lookupProvider));
        generator.addProvider(true, new ModBlockPlacementRuleProvider(packOutput));

        IntegrationHook.setEvent(event);
        AnvilCraft.getINTEGRATION_MANAGER().loadAllServerDataIntegrations();
    }

    /// 初始化生成器
    public static void init() {
        var genInit = REGISTRUM.getDataGenInitializer();
        genInit.add(Registries.ENCHANTMENT, ModEnchantments::bootstrap);
        genInit.add(Registries.DAMAGE_TYPE, ModDamageTypes::bootstrap);
        genInit.add(Registries.VILLAGER_TRADE, ModVillagerTrades::bootstrap);
        genInit.add(Registries.TRADE_SET, ModTradeSets::bootstrap);
        genInit.add(LibRegistries.DEFINITIONS_KEY, ModMultiblockDefinitions::bootstrap);
        genInit.add(dev.anvilcraft.lib.v2.math.init.LibRegistries.FUNCTION_KEY, ModMathFunctions::bootstrap);
        genInit.add(ModRegistryKeys.AMULET_DEF, ModAmuletDefinitions::bootstrap);
        genInit.add(ModRegistryKeys.CATEGORY, ModCategories::bootstrap);

        REGISTRUM.addDataGenerator(ProviderType.ITEM_TAGS, TagsHandler::initItem);
        REGISTRUM.addDataGenerator(ProviderType.BLOCK_TAGS, TagsHandler::initBlock);
        REGISTRUM.addDataGenerator(ProviderType.FLUID_TAGS, TagsHandler::initFluid);
        REGISTRUM.addDataGenerator(ProviderType.ENCHANTMENT_TAGS, TagsHandler::initEnchantment);
        REGISTRUM.addDataGenerator(ProviderType.DAMAGE_TYPE_TAGS, TagsHandler::initDamageType);
        REGISTRUM.addDataGenerator(ProviderType.ENTITY_TAGS, TagsHandler::initEntityType);
        REGISTRUM.addDataGenerator(ProviderType.LANG, LangHandler::init);
        REGISTRUM.addDataGenerator(ProviderType.BLOCKSTATE, AnvilCraftDatagen::generateLargeCauldronFire);
        REGISTRUM.addDataGenerator(ProviderType.RECIPE, RecipeHandler::init);
        REGISTRUM.addDataGenerator(ProviderType.ADVANCEMENT, ModAdvancementsHandler::init);
        REGISTRUM.addDataGenerator(
            ProviderType.registerDynamicTag("tags/point_of_interest_type", "poi_type", Registries.POINT_OF_INTEREST_TYPE),
            TagsHandler::initPoiType
        );
    }

    private static void generateLargeCauldronFire(RegistrumBlockModelGenerator provider) {
        TextureSlot fire0 = TextureSlot.create("0");
        TextureSlot fire1 = TextureSlot.create("1");
        provider.getBuilder()
            .parent(AnvilCraft.of("block/fire_cauldron_fire4"))
            .texture(fire0, AnvilCraft.of("block/large_cauldron_fire_0"))
            .texture(fire1, AnvilCraft.of("block/large_cauldron_fire_1"))
            .texture(TextureSlot.PARTICLE, AnvilCraft.of("block/large_cauldron_fire_0"))
            .transformTemplate(template -> template
                .element(element -> element
                    .from(8, 15, 0.8F).to(8, 31, 15.2F)
                    .shade(false)
                    .rotation(rotation -> rotation.origin(8, 22, 8).singleAxis(Direction.Axis.Y, 45).rescale(true))
                    .face(Direction.EAST, face -> face.uvs(0, 0, 16, 16).texture(fire0))
                    .face(Direction.WEST, face -> face.uvs(0, 0, 16, 16).texture(fire0)))
                .element(element -> element
                    .from(0.8F, 15, 8).to(15.2F, 31, 8)
                    .shade(false)
                    .rotation(rotation -> rotation.origin(8, 22, 8).singleAxis(Direction.Axis.Y, 45).rescale(true))
                    .face(Direction.NORTH, face -> face.uvs(0, 0, 16, 16).texture(fire1))
                    .face(Direction.SOUTH, face -> face.uvs(0, 0, 16, 16).texture(fire1))))
            .build(AnvilCraft.of("block/large_cauldron_fire"));
    }

    public static Criterion<InventoryChangeTrigger.TriggerInstance> has(HolderGetter<Item> lookup, ItemLike itemLike) {
        return RegistrumRecipeProvider.inventoryTrigger(ItemPredicate.Builder.item().of(lookup, itemLike));
    }

    public static Criterion<InventoryChangeTrigger.TriggerInstance> has(HolderGetter<Item> lookup, TagKey<Item> tag) {
        return RegistrumRecipeProvider.inventoryTrigger(ItemPredicate.Builder.item().of(lookup, tag));
    }

    public static String hasItem(TagKey<Item> item) {
        return "has_" + item.location().getPath();
    }

    public static String hasItem(ItemLike item) {
        return "has_" + BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }
}
