package dev.dubhe.anvilcraft.init.block;

import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumBlockModelGenerator;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumItemModelGenerator;
import dev.anvilcraft.lib.v2.registrum.providers.generators.model.PropertyDispatchWrap;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiConsumer;
import dev.anvilcraft.lib.v2.util.nullness.NonNullFunction;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent.Switch;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.block.cake.BerryCakeBlock;
import dev.dubhe.anvilcraft.block.cake.BerryCreamBlock;
import dev.dubhe.anvilcraft.block.cake.CakeBaseBlock;
import dev.dubhe.anvilcraft.block.cake.CakeBlock;
import dev.dubhe.anvilcraft.block.cake.ChocolateCakeBlock;
import dev.dubhe.anvilcraft.block.cake.ChocolateCreamBlock;
import dev.dubhe.anvilcraft.block.cake.CreamBlock;
import dev.dubhe.anvilcraft.block.cake.LargeCakeBlock;
import dev.dubhe.anvilcraft.block.cake.StepEffectBlock;
import dev.dubhe.anvilcraft.block.cake.StepEffectSlabBlock;
import dev.dubhe.anvilcraft.block.cake.StepEffectStairBlock;
import dev.dubhe.anvilcraft.block.cauldron.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.cauldron.ExpFluidCauldronBlock;
import dev.dubhe.anvilcraft.block.cauldron.HoneyCauldronBlock;
import dev.dubhe.anvilcraft.block.cauldron.LavaCauldronBlock;
import dev.dubhe.anvilcraft.block.cauldron.MeltGemCauldronBlock;
import dev.dubhe.anvilcraft.block.cauldron.ObsidianCauldronBlock;
import dev.dubhe.anvilcraft.block.cauldron.OilCauldronBlock;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilAmplifierBlock;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilFluidInterfaceBlock;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilLaserInterfaceBlock;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilLogisticsInterfaceBlock;
import dev.dubhe.anvilcraft.block.cfa.item.CelestialForgingAnvilAmplifierBlockItem;
import dev.dubhe.anvilcraft.block.cfa.item.CelestialForgingAnvilInterfaceBlockItem;
import dev.dubhe.anvilcraft.block.container.FluidTankBlock;
import dev.dubhe.anvilcraft.block.container.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.container.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.decoration.ReinforcedConcreteBlock;
import dev.dubhe.anvilcraft.block.decoration.ember.EmberMetalBlock;
import dev.dubhe.anvilcraft.block.decoration.ember.EmberMetalPillarBlock;
import dev.dubhe.anvilcraft.block.decoration.ember.EmberMetalSlabBlock;
import dev.dubhe.anvilcraft.block.decoration.ember.EmberMetalStairBlock;
import dev.dubhe.anvilcraft.block.decoration.frost.FrostMetalBlock;
import dev.dubhe.anvilcraft.block.decoration.frost.FrostMetalPillarBlock;
import dev.dubhe.anvilcraft.block.decoration.frost.FrostMetalSlabBlock;
import dev.dubhe.anvilcraft.block.decoration.frost.FrostMetalStairBlock;
import dev.dubhe.anvilcraft.block.decoration.heavyiron.HeavyIronBeamBlock;
import dev.dubhe.anvilcraft.block.decoration.heavyiron.HeavyIronDoorBlock;
import dev.dubhe.anvilcraft.block.decoration.heavyiron.HeavyIronPlateBlock;
import dev.dubhe.anvilcraft.block.decoration.heavyiron.HeavyIronTrapdoorBlock;
import dev.dubhe.anvilcraft.block.decoration.heavyiron.HeavyIronWallBlock;
import dev.dubhe.anvilcraft.block.fluid.ExpFluidBlock;
import dev.dubhe.anvilcraft.block.heatable.GlowingBlock;
import dev.dubhe.anvilcraft.block.heatable.HeatedBlock;
import dev.dubhe.anvilcraft.block.heatable.IncandescentBlock;
import dev.dubhe.anvilcraft.block.heatable.NormalBlock;
import dev.dubhe.anvilcraft.block.heatable.OverheatedEmberMetalBlock;
import dev.dubhe.anvilcraft.block.heatable.RedhotBlock;
import dev.dubhe.anvilcraft.block.laser.LaserReceiverBlock;
import dev.dubhe.anvilcraft.block.laser.PropelPistonBlock;
import dev.dubhe.anvilcraft.block.laser.RubyLaserBlock;
import dev.dubhe.anvilcraft.block.laser.RubyPrismBlock;
import dev.dubhe.anvilcraft.block.logistics.chute.ChuteBlock;
import dev.dubhe.anvilcraft.block.logistics.chute.MagneticChuteBlock;
import dev.dubhe.anvilcraft.block.logistics.chute.SimpleChuteBlock;
import dev.dubhe.anvilcraft.block.logistics.sliding.ActivatorSlidingRailBlock;
import dev.dubhe.anvilcraft.block.logistics.sliding.DetectorSlidingRailBlock;
import dev.dubhe.anvilcraft.block.logistics.sliding.PoweredSlidingRailBlock;
import dev.dubhe.anvilcraft.block.logistics.sliding.SlidingRailBlock;
import dev.dubhe.anvilcraft.block.logistics.sliding.SlidingRailStopBlock;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.nesting.NestingShulkerBoxBlock;
import dev.dubhe.anvilcraft.block.nesting.OverNestingShulkerBoxBlock;
import dev.dubhe.anvilcraft.block.nesting.SupercriticalNestingShulkerBoxBlock;
import dev.dubhe.anvilcraft.block.plate.EntityCountPressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.EntityTypePressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.FireImmunePressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.HealthPercentPressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.ItemDurabilityPressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.PlayerHungerPressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.PlayerInHandItemDurabilityPressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.PlayerInventoryPressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.PowerLevelPressurePlateBlock;
import dev.dubhe.anvilcraft.block.plate.TimeCountedPressurePlateBlock;
import dev.dubhe.anvilcraft.block.power.HeliostatsBlock;
import dev.dubhe.anvilcraft.block.power.LoadMonitorBlock;
import dev.dubhe.anvilcraft.block.power.PiezoelectricCrystalBlock;
import dev.dubhe.anvilcraft.block.power.batch.BaseBatchCraftingBlock;
import dev.dubhe.anvilcraft.block.power.batch.BatchCrafterBlock;
import dev.dubhe.anvilcraft.block.power.batch.BatchCutterBlock;
import dev.dubhe.anvilcraft.block.power.consumer.DischargerBlock;
import dev.dubhe.anvilcraft.block.power.consumer.HeaterBlock;
import dev.dubhe.anvilcraft.block.power.consumer.InductionLightBlock;
import dev.dubhe.anvilcraft.block.power.consumer.ItemCollectorBlock;
import dev.dubhe.anvilcraft.block.power.consumer.TeslaTowerBlock;
import dev.dubhe.anvilcraft.block.power.converter.PowerConverterBigBlock;
import dev.dubhe.anvilcraft.block.power.converter.PowerConverterMiddleBlock;
import dev.dubhe.anvilcraft.block.power.converter.PowerConverterSmallBlock;
import dev.dubhe.anvilcraft.block.power.generator.ChargeCollectorBlock;
import dev.dubhe.anvilcraft.block.power.generator.ChargerBlock;
import dev.dubhe.anvilcraft.block.power.generator.CreativeGeneratorBlock;
import dev.dubhe.anvilcraft.block.power.generator.HeatCollectorBlock;
import dev.dubhe.anvilcraft.block.power.generator.VoidEnergyCollectorBlock;
import dev.dubhe.anvilcraft.block.power.ring.AccelerationRingBlock;
import dev.dubhe.anvilcraft.block.power.ring.DeflectionRingBlock;
import dev.dubhe.anvilcraft.block.power.transmitting.RemoteTransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.power.transmitting.TransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.production.CrabTrapBlock;
import dev.dubhe.anvilcraft.block.production.MineralFountainBlock;
import dev.dubhe.anvilcraft.block.special.BlackHoleBlock;
import dev.dubhe.anvilcraft.block.special.PlasmaJetsBlock;
import dev.dubhe.anvilcraft.block.special.WhiteHoleBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.FragmentationDegree;
import dev.dubhe.anvilcraft.block.state.ReinforcedConcreteHalf;
import dev.dubhe.anvilcraft.block.state.Vertical3PartHalf;
import dev.dubhe.anvilcraft.block.state.Vertical4PartHalf;
import dev.dubhe.anvilcraft.block.storage.AmberBlock;
import dev.dubhe.anvilcraft.block.storage.EndDustBlock;
import dev.dubhe.anvilcraft.block.storage.FerriteCoreMagnetBlock;
import dev.dubhe.anvilcraft.block.storage.FlintBlock;
import dev.dubhe.anvilcraft.block.storage.GunpowderBlock;
import dev.dubhe.anvilcraft.block.storage.HollowMagnetBlock;
import dev.dubhe.anvilcraft.block.storage.LevitationPowderBlock;
import dev.dubhe.anvilcraft.block.storage.MagnetBlock;
import dev.dubhe.anvilcraft.block.storage.MagnetoElectricCoreBlock;
import dev.dubhe.anvilcraft.block.storage.MobAmberBlock;
import dev.dubhe.anvilcraft.block.storage.NegativeMatterBlock;
import dev.dubhe.anvilcraft.block.storage.ResentfulAmberBlock;
import dev.dubhe.anvilcraft.block.storage.ResinBlock;
import dev.dubhe.anvilcraft.block.storage.RottenFleshBlock;
import dev.dubhe.anvilcraft.block.storage.SimpleConfinementAnvilonBlock;
import dev.dubhe.anvilcraft.block.storage.SugarBlock;
import dev.dubhe.anvilcraft.block.storage.TranscendiumBlock;
import dev.dubhe.anvilcraft.block.storage.VoidMatterBlock;
import dev.dubhe.anvilcraft.block.utility.ActiveSilencerBlock;
import dev.dubhe.anvilcraft.block.utility.ArrowBlock;
import dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.block.StructureScannerBlock;
import dev.dubhe.anvilcraft.block.utility.BlockDevourerBlock;
import dev.dubhe.anvilcraft.block.utility.BlockPlacerBlock;
import dev.dubhe.anvilcraft.block.utility.ControllableSandBlock;
import dev.dubhe.anvilcraft.block.utility.ImpactPileBlock;
import dev.dubhe.anvilcraft.block.utility.MengerSpongeBlock;
import dev.dubhe.anvilcraft.block.utility.OverseerBlock;
import dev.dubhe.anvilcraft.block.utility.redstone.AdvancedComparatorBlock;
import dev.dubhe.anvilcraft.block.utility.redstone.BlockComparatorBlock;
import dev.dubhe.anvilcraft.block.utility.redstone.ItemDetectorBlock;
import dev.dubhe.anvilcraft.block.utility.redstone.PulseGeneratorBlock;
import dev.dubhe.anvilcraft.block.workstation.ConfinementChamberBlock;
import dev.dubhe.anvilcraft.block.workstation.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.block.workstation.CrushingTableBlock;
import dev.dubhe.anvilcraft.block.workstation.FishTankBlock;
import dev.dubhe.anvilcraft.block.workstation.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.workstation.JewelCraftingTable;
import dev.dubhe.anvilcraft.block.workstation.NeoforgeBlock;
import dev.dubhe.anvilcraft.block.workstation.NeutronIrradiatorBlock;
import dev.dubhe.anvilcraft.block.workstation.SpaceOvercompressorBlock;
import dev.dubhe.anvilcraft.block.workstation.SpectralAnvilBlock;
import dev.dubhe.anvilcraft.block.workstation.StampingPlatformBlock;
import dev.dubhe.anvilcraft.block.workstation.TranscendenceAnvilBlock;
import dev.dubhe.anvilcraft.block.workstation.TransparentCraftingTableBlock;
import dev.dubhe.anvilcraft.block.workstation.ember.EmberAnvilBlock;
import dev.dubhe.anvilcraft.block.workstation.ember.EmberGrindstoneBlock;
import dev.dubhe.anvilcraft.block.workstation.ember.EmberSmithingTableBlock;
import dev.dubhe.anvilcraft.block.workstation.frost.FrostAnvilBlock;
import dev.dubhe.anvilcraft.block.workstation.frost.FrostGrindstoneBlock;
import dev.dubhe.anvilcraft.block.workstation.frost.FrostSmithingTableBlock;
import dev.dubhe.anvilcraft.block.workstation.royal.RoyalAnvilBlock;
import dev.dubhe.anvilcraft.block.workstation.royal.RoyalGrindstoneBlock;
import dev.dubhe.anvilcraft.block.workstation.royal.RoyalSmithingTableBlock;
import dev.dubhe.anvilcraft.data.recipe.RegistrumBlockRecipeLoader;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemGroups;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.block.ChuteBlockItem;
import dev.dubhe.anvilcraft.item.block.CursedBlockItem;
import dev.dubhe.anvilcraft.item.block.EndDustBlockItem;
import dev.dubhe.anvilcraft.item.block.FishTankBlockItem;
import dev.dubhe.anvilcraft.item.block.FlexibleMultiPartBlockItem;
import dev.dubhe.anvilcraft.item.block.FrostMetalBlockItem;
import dev.dubhe.anvilcraft.item.block.HasMobBlockItem;
import dev.dubhe.anvilcraft.item.block.HeatableBlockItem;
import dev.dubhe.anvilcraft.item.block.HeliostatsItem;
import dev.dubhe.anvilcraft.item.block.LevitationBlockItem;
import dev.dubhe.anvilcraft.item.block.MengerSpongeBlockItem;
import dev.dubhe.anvilcraft.item.block.MultiphaseMatterBlockItem;
import dev.dubhe.anvilcraft.item.block.PlaceInWaterBlockItem;
import dev.dubhe.anvilcraft.item.block.RadiationBlockItem;
import dev.dubhe.anvilcraft.item.block.ResinBlockItem;
import dev.dubhe.anvilcraft.item.block.ShulkerContainerBlockItem;
import dev.dubhe.anvilcraft.item.block.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.item.block.SuperHeavyBlockItem;
import dev.dubhe.anvilcraft.item.block.TeslaTowerItem;
import dev.dubhe.anvilcraft.item.block.UncontainableBlockItem;
import dev.dubhe.anvilcraft.item.property.component.OverLimitItemContainerContents;
import dev.dubhe.anvilcraft.util.registrater.DataGenUtil;
import dev.dubhe.anvilcraft.util.registrater.ModelProviderUtil;
import dev.dubhe.anvilcraft.util.registrater.PropertiesProviderUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.common.Tags;

import java.util.function.Supplier;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRUM;

@SuppressWarnings({
    "unused",
    "CodeBlock2Expr"
})
public class ModBlocks {
    static {
        REGISTRUM.defaultCreativeTab(ModItemGroups.ANVILCRAFT_FUNCTION_BLOCK.getKey());
    }

    public static final BlockEntry<? extends Block> MAGNET_BLOCK = REGISTRUM.block("magnet_block", MagnetBlock::new)
        .lang("Block of Magnet")
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS, ModItemTags.STORAGE_BLOCKS_MAGNET)
        .build()
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.MAGNET,
            BlockTags.NEEDS_STONE_TOOL,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_MAGNET
        )
        .recipe(RegistrumBlockRecipeLoader::magnetBlock)
        .register();

    public static final BlockEntry<? extends Block> HOLLOW_MAGNET_BLOCK = REGISTRUM.block("hollow_magnet_block", HollowMagnetBlock::new)
        .lang("Hollowed Block of Magnet")
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.MAGNET, BlockTags.NEEDS_STONE_TOOL)
        .recipe(RegistrumBlockRecipeLoader::hollowMagnetBlock)
        .register();

    public static final BlockEntry<? extends Block> FERRITE_CORE_MAGNET_BLOCK = REGISTRUM.block(
            "ferrite_core_magnet_block",
            FerriteCoreMagnetBlock::new
        )
        .lang("Ferrite-Cored Block of Magnet")
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(BlockBehaviour.Properties::randomTicks)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.MAGNET, BlockTags.NEEDS_STONE_TOOL)
        .recipe(RegistrumBlockRecipeLoader::ferriteCoreMagnetBlock)
        .register();

    public static final BlockEntry<? extends Block> STAMPING_PLATFORM = REGISTRUM.block("stamping_platform", StampingPlatformBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::horizontalFacingBlock)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::stampingPlatform)
        .register();

    public static final BlockEntry<? extends Block> CRUSHING_TABLE = REGISTRUM.block("crushing_table", CrushingTableBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::crushingTable)
        .register();

    public static final BlockEntry<FishTankBlock> FISH_TANK = REGISTRUM.block("fish_tank", FishTankBlock::new)
        .initialProperties(() -> Blocks.CAULDRON)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item(FishTankBlockItem::new)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.CAULDRONS)
        .recipe(RegistrumBlockRecipeLoader::fishTank)
        .register();

    public static final BlockEntry<FluidTankBlock> FLUID_TANK = REGISTRUM.block("fluid_tank", FluidTankBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::fluidTank)
        .register();

    public static final BlockEntry<? extends Block> CORRUPTED_BEACON = REGISTRUM.block("corrupted_beacon", CorruptedBeaconBlock::new)
        .initialProperties(() -> Blocks.BEACON)
        .properties(p -> p.isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<GiantAnvilBlock> GIANT_ANVIL = REGISTRUM.block("giant_anvil", GiantAnvilBlock::new)
        .initialProperties(() -> Blocks.ANVIL)
        .properties(p -> p
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .strength(4.0F)
            .sound(GiantAnvilBlock.SOUND_TYPE)
            .explosionResistance(1200))
        .loot(SimpleMultiPartBlock::loot)
        .item(SimpleMultiPartBlockItem<Cube3x3PartHalf>::new)
        .properties(properties -> properties.stacksTo(16))
        .build()
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<? extends Block> NEUTRON_IRRADIATOR = REGISTRUM.block("neutron_irradiator", NeutronIrradiatorBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .properties(p -> p.strength(50.0F, 1200F).lightLevel(state -> 7).emissiveRendering(ModBlocks::always))
        .recipe(RegistrumBlockRecipeLoader::neutronIrradiator)
        .tag(BlockTags.WITHER_IMMUNE, BlockTags.DRAGON_IMMUNE, ModBlockTags.COLLISION_IMMUNE)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .register();

    public static final BlockEntry<? extends SpectralAnvilBlock> SPECTRAL_ANVIL = REGISTRUM.block(
            "spectral_anvil",
            SpectralAnvilBlock::new
        )
        .initialProperties(() -> Blocks.GLASS)
        .properties(p -> p.mapColor(MapColor.METAL)
            .isValidSpawn(Blocks::never)
            .requiresCorrectToolForDrops()
            .strength(5.0F, 1200.0F)
            .sound(SoundType.ANVIL))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .tag(ItemTags.ANVIL)
        .build()
        .tag(BlockTags.ANVIL, ModBlockTags.NON_MAGNETIC, ModBlockTags.CANT_BROKEN_ANVIL, BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<? extends Block> ROYAL_ANVIL = REGISTRUM.block("royal_anvil", RoyalAnvilBlock::new)
        .recipe(RegistrumBlockRecipeLoader::royalAnvil)
        .initialProperties(() -> Blocks.ANVIL)
        .properties(p -> p.isValidSpawn(Blocks::never).strength(5.0F, 1200F))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .tag(ItemTags.ANVIL)
        .build()
        .tag(BlockTags.ANVIL, ModBlockTags.CANT_BROKEN_ANVIL, BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
        .register();

    public static final BlockEntry<NeoforgeBlock> NEOFORGE = REGISTRUM
        .block("neoforge", NeoforgeBlock::new)
        .initialProperties(() -> Blocks.CAKE)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .tag(ItemTags.ANVIL)
        .build()
        .tag(BlockTags.ANVIL, ModBlockTags.NON_MAGNETIC, ModBlockTags.CANT_BROKEN_ANVIL)
        .recipe(RegistrumBlockRecipeLoader::neoforge)
        .register();

    public static final BlockEntry<? extends Block> ROYAL_GRINDSTONE = REGISTRUM.block("royal_grindstone", RoyalGrindstoneBlock::new)
        .recipe(RegistrumBlockRecipeLoader::royalGrindstone)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.isValidSpawn(Blocks::never).strength(5.0F, 1200F))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
        .register();

    public static final BlockEntry<? extends Block> ROYAL_SMITHING_TABLE = REGISTRUM.block(
            "royal_smithing_table",
            RoyalSmithingTableBlock::new
        )
        .recipe(RegistrumBlockRecipeLoader::royalSmithingTable)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
        .register();

    public static final BlockEntry<FrostAnvilBlock> FROST_ANVIL = REGISTRUM
        .block("frost_anvil", FrostAnvilBlock::new)
        .recipe(RegistrumBlockRecipeLoader::frostAnvil)
        .initialProperties(() -> Blocks.ANVIL)
        .tag(
            BlockTags.ANVIL,
            ModBlockTags.CANT_BROKEN_ANVIL,
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL
        )
        .properties(properties -> properties
            .isValidSpawn(Blocks::never)
            .lightLevel(state -> 9)
            .noOcclusion()
            .emissiveRendering(ModBlocks::always)
            .strength(50.0F, 1200F)
        )
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .tag(ItemTags.ANVIL)
        .build()
        .register();

    public static final BlockEntry<FrostGrindstoneBlock> FROST_GRINDSTONE = REGISTRUM
        .block("frost_grindstone", FrostGrindstoneBlock::new)
        .recipe(RegistrumBlockRecipeLoader::frostGrindstone)
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL
        )
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 9).noOcclusion().emissiveRendering(ModBlocks::always))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .register();

    public static final BlockEntry<FrostSmithingTableBlock> FROST_SMITHING_TABLE = REGISTRUM
        .block("frost_smithing_table", FrostSmithingTableBlock::new)
        .recipe(RegistrumBlockRecipeLoader::frostSmithingTable)
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL
        )
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 9).noOcclusion().emissiveRendering(ModBlocks::always))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .register();

    public static final BlockEntry<EmberAnvilBlock> EMBER_ANVIL = REGISTRUM.block("ember_anvil", EmberAnvilBlock::new)
        .recipe(RegistrumBlockRecipeLoader::emberAnvil)
        .initialProperties(() -> Blocks.ANVIL)
        .tag(
            BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE,
            BlockTags.ANVIL,
            ModBlockTags.CANT_BROKEN_ANVIL,
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL
        )
        .properties(properties -> properties
            .isValidSpawn(Blocks::never)
            .lightLevel(state -> 9)
            .noOcclusion()
            .emissiveRendering(ModBlocks::always)
            .strength(50.0F, 1200F)
        )
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ItemTags.ANVIL)
        .build()
        .register();

    public static final BlockEntry<EmberGrindstoneBlock> EMBER_GRINDSTONE = REGISTRUM.block("ember_grindstone", EmberGrindstoneBlock::new)
        .recipe(RegistrumBlockRecipeLoader::emberGrindstone)
        .tag(BlockTags.WITHER_IMMUNE, BlockTags.DRAGON_IMMUNE, BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_DIAMOND_TOOL)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 9).noOcclusion().emissiveRendering(ModBlocks::always))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .register();

    public static final BlockEntry<EmberSmithingTableBlock> EMBER_SMITHING_TABLE = REGISTRUM.block(
            "ember_smithing_table",
            EmberSmithingTableBlock::new
        )
        .recipe(RegistrumBlockRecipeLoader::emberSmithingTable)
        .tag(BlockTags.WITHER_IMMUNE, BlockTags.DRAGON_IMMUNE, BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_DIAMOND_TOOL)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 9).noOcclusion().emissiveRendering(ModBlocks::always))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .register();

    public static final BlockEntry<TranscendenceAnvilBlock> TRANSCENDENCE_ANVIL = REGISTRUM.block(
            "transcendence_anvil",
            TranscendenceAnvilBlock::new
        )
        .recipe(RegistrumBlockRecipeLoader::transcendenceAnvil)
        .initialProperties(() -> Blocks.ANVIL)
        .tag(
            BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE,
            BlockTags.ANVIL,
            ModBlockTags.CANT_BROKEN_ANVIL,
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            ModBlockTags.COLLISION_IMMUNE
        )
        .properties(properties -> properties
            .isValidSpawn(Blocks::never)
            .lightLevel(state -> 9)
            .noOcclusion()
            .emissiveRendering(ModBlocks::always)
            .strength(50.0F, 1200F)
        )
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ItemTags.ANVIL, ModItemTags.EXPLOSION_PROOF)
        .build()
        .register();

    public static final BlockEntry<? extends Block> CREATIVE_GENERATOR = REGISTRUM.block("creative_generator", CreativeGeneratorBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .model(DataGenUtil::noExtraModelOrState)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<? extends Block> HEATER = REGISTRUM.block("heater", HeaterBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .properties(properties -> properties.isValidSpawn(Blocks::never)
            .noOcclusion()
            .lightLevel(state -> state.getValue(IPowerConsumer.OVERLOAD) ? 0 : 15))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::heater)
        .register();

    public static final BlockEntry<TransmissionPoleBlock> TRANSMISSION_POLE = REGISTRUM.block(
            "transmission_pole",
            TransmissionPoleBlock::new
        )
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .properties(properties -> properties.isValidSpawn(Blocks::never).noOcclusion().lightLevel(state -> {
            if (state.getValue(TransmissionPoleBlock.HALF) != Vertical3PartHalf.TOP) return 0;
            if (state.getValue(IPowerConsumer.SWITCH) == Switch.OFF) return 0;
            if (state.getValue(IPowerConsumer.OVERLOAD)) return 6;
            return 15;
        }))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item(SimpleMultiPartBlockItem<Vertical3PartHalf>::new)
        .model(DataGenUtil::onlyInfo)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::transmissionPole)
        .loot(SimpleMultiPartBlock::loot)
        .register();

    public static final BlockEntry<? extends Block> REMOTE_TRANSMISSION_POLE = REGISTRUM.block(
            "remote_transmission_pole",
            RemoteTransmissionPoleBlock::new
        )
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .loot(SimpleMultiPartBlock::loot)
        .properties(properties -> properties.isValidSpawn(Blocks::never).noOcclusion().lightLevel(state -> {
            if (state.getValue(RemoteTransmissionPoleBlock.HALF) != Vertical4PartHalf.TOP) return 0;
            if (state.getValue(IPowerConsumer.SWITCH) == Switch.OFF) return 0;
            if (state.getValue(IPowerConsumer.OVERLOAD)) return 6;
            return 15;
        }))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item(SimpleMultiPartBlockItem<Vertical4PartHalf>::new)
        .model(DataGenUtil::onlyInfo)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::remoteTransmissionPole)
        .register();

    @SuppressWarnings("Convert2Lambda")
    public static final BlockEntry<TeslaTowerBlock> TESLA_TOWER = REGISTRUM.block("tesla_tower", TeslaTowerBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .loot(SimpleMultiPartBlock::loot)
        .properties(properties -> properties.isValidSpawn(Blocks::never).noOcclusion().lightLevel(state -> {
            if (state.getValue(TeslaTowerBlock.HALF) != Vertical4PartHalf.TOP) return 0;
            if (state.getValue(IPowerConsumer.SWITCH) == Switch.OFF) return 0;
            if (state.getValue(IPowerConsumer.OVERLOAD)) return 6;
            return 15;
        }))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item(TeslaTowerItem::new)
        .model(() -> new NonNullBiConsumer<>() {
            @Override
            public void accept(
                DataGenContext<Item, TeslaTowerItem> ctx,
                RegistrumItemModelGenerator generator
            ) {
                generator.createWithExistingModel(ctx.get(), ctx.getId().withPrefix("block/").withSuffix("_overall"));
            }
        })
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::teslaTower)
        .register();

    public static final BlockEntry<InductionLightBlock> INDUCTION_LIGHT = REGISTRUM.block("induction_light", InductionLightBlock::new)
        .initialProperties(ModBlocks.MAGNET_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never).lightLevel(state -> {
            if (state.getValue(InductionLightBlock.POWERED)) return 0;
            if (state.getValue(InductionLightBlock.OVERLOAD)) return 7;
            return 15;
        }))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::inductionLight)
        .register();

    public static final BlockEntry<ChargeCollectorBlock> CHARGE_COLLECTOR = REGISTRUM.block("charge_collector", ChargeCollectorBlock::new)
        .simpleItem()
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::chargeCollector)
        .register();

    public static final BlockEntry<HeliostatsBlock> HELIOSTATS = REGISTRUM.block("heliostats", HeliostatsBlock::new)
        .initialProperties(() -> Blocks.GLASS)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .defaultLoot()
        .item(HeliostatsItem::new)
        .model(DataGenUtil::onlyInfo)
        .build()
        .recipe(RegistrumBlockRecipeLoader::heliostats)
        .register();

    @SuppressWarnings("Convert2Lambda")
    public static final BlockEntry<LoadMonitorBlock> LOAD_MONITOR = REGISTRUM.block("load_monitor", LoadMonitorBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.isValidSpawn(Blocks::never).lightLevel(state -> {
            if (state.getValue(IPowerConsumer.OVERLOAD)) {
                return 6;
            } else {
                return 15;
            }
        }).noOcclusion())
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .item()
        .model(() -> new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, BlockItem> ctx, RegistrumItemModelGenerator generator) {
                generator.createWithExistingModel(ctx.get(), ctx.getId().withPrefix("block/").withSuffix("_0"));
            }
        })
        .build()
        .recipe(RegistrumBlockRecipeLoader::loadMonitor)
        .register();

    @SuppressWarnings("Convert2Lambda")
    public static final BlockEntry<PowerConverterSmallBlock> POWER_CONVERTER_SMALL = REGISTRUM.block(
            "power_converter_small",
            PowerConverterSmallBlock::new
        )
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.isValidSpawn(Blocks::never).lightLevel(state -> {
            if (state.getValue(IPowerConsumer.OVERLOAD)) {
                return 6;
            } else {
                return 15;
            }
        }))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .recipe(RegistrumBlockRecipeLoader::powerConverterSmall)
        .item()
        .model(() -> new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, BlockItem> ctx, RegistrumItemModelGenerator generator) {
                generator.createWithExistingModel(ctx.get(), ctx.getId().withPrefix("block/"));
            }
        })
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    @SuppressWarnings("Convert2Lambda")
    public static final BlockEntry<PowerConverterMiddleBlock> POWER_CONVERTER_MIDDLE = REGISTRUM.block(
            "power_converter_middle",
            PowerConverterMiddleBlock::new
        )
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.isValidSpawn(Blocks::never).lightLevel(state -> {
            if (state.getValue(IPowerConsumer.OVERLOAD)) {
                return 6;
            } else {
                return 15;
            }
        }))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .recipe(RegistrumBlockRecipeLoader::powerConverterMiddle)
        .item()
        .model(() -> new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, BlockItem> ctx, RegistrumItemModelGenerator generator) {
                generator.createWithExistingModel(ctx.get(), ctx.getId().withPrefix("block/"));
            }
        })
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    @SuppressWarnings("Convert2Lambda")
    public static final BlockEntry<PowerConverterBigBlock> POWER_CONVERTER_BIG = REGISTRUM.block(
            "power_converter_big",
            PowerConverterBigBlock::new
        )
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.isValidSpawn(Blocks::never).lightLevel(state -> {
            if (state.getValue(IPowerConsumer.OVERLOAD)) {
                return 6;
            } else {
                return 15;
            }
        }))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .recipe(RegistrumBlockRecipeLoader::powerConverterBig)
        .item()
        .model(() -> new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, BlockItem> ctx, RegistrumItemModelGenerator generator) {
                generator.createWithExistingModel(ctx.get(), ctx.getId().withPrefix("block/"));
            }
        })
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<PiezoelectricCrystalBlock> PIEZOELECTRIC_CRYSTAL = REGISTRUM.block(
            "piezoelectric_crystal",
            PiezoelectricCrystalBlock::new
        )
        .simpleItem()
        .blockstate(DataGenUtil::noExtraModelOrState)
        .initialProperties(() -> Blocks.GLASS)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::piezoelectricCrystal)
        .register();

    public static final BlockEntry<? extends Block> BATCH_CRAFTER = REGISTRUM.block("batch_crafter", BatchCrafterBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::batchCrafter)
        .simpleItem()
        .onRegister(block -> BaseBatchCraftingBlock.registerBatchCrafting(() -> block))
        .register();

    public static final BlockEntry<? extends Block> BATCH_CUTTER = REGISTRUM.block("batch_cutter", BatchCutterBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::batchCutter)
        .simpleItem()
        .onRegister(block -> BaseBatchCraftingBlock.registerBatchCrafting(() -> block))
        .register();

    public static final BlockEntry<ItemCollectorBlock> ITEM_COLLECTOR = REGISTRUM.block("item_collector", ItemCollectorBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.isValidSpawn(Blocks::never))
        .simpleItem()
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::itemCollector)
        .register();

    public static final BlockEntry<HeatCollectorBlock> HEAT_COLLECTOR = REGISTRUM.block("heat_collector", HeatCollectorBlock::new)
        .simpleItem()
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .recipe(RegistrumBlockRecipeLoader::heatCollector)
        .register();

    public static final BlockEntry<ChargerBlock> CHARGER = REGISTRUM.block("charger", ChargerBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::charger)
        .register();

    public static final BlockEntry<DischargerBlock> DISCHARGER = REGISTRUM.block("discharger", DischargerBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::discharger)
        .register();

    public static final BlockEntry<ActiveSilencerBlock> ACTIVE_SILENCER = REGISTRUM.block("active_silencer", ActiveSilencerBlock::new)
        .simpleItem()
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .recipe(RegistrumBlockRecipeLoader::activeSilencer)
        .register();

    public static final BlockEntry<BlockPlacerBlock> BLOCK_PLACER = REGISTRUM.block("block_placer", BlockPlacerBlock::new)
        .simpleItem()
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .recipe(RegistrumBlockRecipeLoader::blockPlacer)
        .register();

    public static final BlockEntry<SmartBlockPlacerBlock> SMART_BLOCK_PLACER = REGISTRUM
        .block("smart_block_placer", SmartBlockPlacerBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::horizontalFacingBlock)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::smartBlockPlacer)
        .register();

    public static final BlockEntry<StructureScannerBlock> STRUCTURE_SCANNER = REGISTRUM
        .block("structure_scanner", StructureScannerBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::horizontalFacingBlock)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::structureScanner)
        .register();

    public static final BlockEntry<BlockDevourerBlock> BLOCK_DEVOURER = REGISTRUM.block("block_devourer", BlockDevourerBlock::new)
        .item()
        .properties(Item.Properties::fireResistant)
        .properties(p -> p.rarity(Rarity.UNCOMMON))
        .build()
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never).explosionResistance(1200F))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .recipe(RegistrumBlockRecipeLoader::blockDevourer)
        .register();

    public static final BlockEntry<RubyLaserBlock> RUBY_LASER = REGISTRUM.block("ruby_laser", RubyLaserBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.lightLevel(it -> {
            if (it.getValue(RubyLaserBlock.SWITCH) == Switch.ON) {
                return 15;
            } else {
                return 0;
            }
        }))
        .recipe(RegistrumBlockRecipeLoader::rubyLaser)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<RubyPrismBlock> RUBY_PRISM = REGISTRUM.block("ruby_prism", RubyPrismBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .recipe(RegistrumBlockRecipeLoader::rubyPrism)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<LaserReceiverBlock> LASER_RECEIVER = REGISTRUM.block("laser_receiver", LaserReceiverBlock::new)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .initialProperties(ModBlocks.RUBY_PRISM::get)
        .properties(properties -> properties.noOcclusion()
            .isValidSpawn(Blocks::never)
            .isRedstoneConductor(ModBlocks::never)
            .requiresCorrectToolForDrops()
            .lightLevel((blockState) -> blockState.getValue(LaserReceiverBlock.ACTIVE) ? 15 : 0))
        .recipe(RegistrumBlockRecipeLoader::laserReceiver)
        .simpleItem()
        .register();

    public static final BlockEntry<BlockComparatorBlock> BLOCK_COMPARATOR = REGISTRUM.block("block_comparator", BlockComparatorBlock::new)
        .initialProperties(() -> Blocks.OBSERVER)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::blockComparator)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .register();

    public static final BlockEntry<ItemDetectorBlock> ITEM_DETECTOR = REGISTRUM.block("item_detector", ItemDetectorBlock::new)
        .initialProperties(() -> Blocks.DAYLIGHT_DETECTOR)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::itemDetector)
        .blockstate(() -> DataGenUtil.horizontalFacingBlockInverted(
            ItemDetectorBlock.POWERED,
            ctx -> ctx.getId().withPrefix("block/").withSuffix("_on"),
            ctx -> ctx.getId().withPrefix("block/")
        ))
        .simpleItem()
        .register();

    public static final BlockEntry<ImpactPileBlock> IMPACT_PILE = REGISTRUM.block("impact_pile", ImpactPileBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_DIAMOND_TOOL)
        .blockstate(DataGenUtil::onlyState)
        .recipe(RegistrumBlockRecipeLoader::impactPile)
        .item()
        .properties(Item.Properties::fireResistant)
        .build()
        .register();

    public static final BlockEntry<OverseerBlock> OVERSEER_BLOCK = REGISTRUM.block("overseer", OverseerBlock::new)
        .initialProperties(() -> Blocks.OBSIDIAN)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .loot(SimpleMultiPartBlock::loot)
        .item(SimpleMultiPartBlockItem<Vertical3PartHalf>::new)
        .model(DataGenUtil::onlyInfo)
        .build()
        .recipe(RegistrumBlockRecipeLoader::overseerBlock)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<ShulkerContainerBlock> SHULKER_CONTAINER = REGISTRUM
        .block("shulker_container", ShulkerContainerBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .loot(FlexibleMultiPartBlock::loot)
        .properties(properties -> properties
            .noOcclusion()
            .isValidSpawn(ModBlocks::never)
            .requiresCorrectToolForDrops()
        )
        .item(ShulkerContainerBlockItem::new)
        .properties(properties -> properties.stacksTo(16))
        .tag(ModItemTags.EXPLOSION_PROOF)
        .build()
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.NEEDS_DIAMOND_TOOL)
        .register();

    public static final BlockEntry<JewelCraftingTable> JEWEL_CRAFTING_TABLE = REGISTRUM.block(
            "jewelcrafting_table",
            JewelCraftingTable::new
        )
        .initialProperties(() -> Blocks.STONE)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .tag(Tags.Items.VILLAGER_JOB_SITES)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, Tags.Blocks.VILLAGER_JOB_SITES)
        .lang("Jewel Crafting Table")
        .recipe(RegistrumBlockRecipeLoader::jewelCraftingTable)
        .register();

    public static final BlockEntry<TransparentCraftingTableBlock> TRANSPARENT_CRAFTING_TABLE = REGISTRUM.block(
            "transparent_crafting_table",
            TransparentCraftingTableBlock::new
        )
        .properties(properties -> properties.mapColor(MapColor.COLOR_PURPLE).strength(1.5F, 3).sound(SoundType.AMETHYST).noOcclusion())
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES)
        .recipe(RegistrumBlockRecipeLoader::transparentCraftingTable)
        .register();

    public static final BlockEntry<CrabTrapBlock> CRAB_TRAP = REGISTRUM.block("crab_trap", CrabTrapBlock::new)
        .properties(p -> p.sound(SoundType.SCAFFOLDING).strength(2))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .item(PlaceInWaterBlockItem::new)
        .build()
        .tag(BlockTags.MINEABLE_WITH_AXE)
        .recipe(RegistrumBlockRecipeLoader::crabTrap)
        .register();

    public static final BlockEntry<MengerSpongeBlock> MENGER_SPONGE = REGISTRUM.block("menger_sponge", MengerSpongeBlock::new)
        .initialProperties(() -> Blocks.SPONGE)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_HOE)
        .item(MengerSpongeBlockItem::new)
        .build()
        .register();
    public static final BlockEntry<? extends Block> CHUTE = REGISTRUM.block("chute", ChuteBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item(ChuteBlockItem::new)
        .onRegister(blockItem -> Item.BY_BLOCK.put(ModBlocks.SIMPLE_CHUTE.get(), blockItem))
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::chute)
        .register();

    public static final BlockEntry<MagneticChuteBlock> MAGNETIC_CHUTE = REGISTRUM.block("magnetic_chute", MagneticChuteBlock::new)
        .initialProperties(ModBlocks.CHUTE)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item(ChuteBlockItem::new)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::magneticChute)
        .register();

    public static final BlockEntry<SimpleChuteBlock> SIMPLE_CHUTE = REGISTRUM.block("simple_chute", SimpleChuteBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .loot((tables, block) -> tables.dropOther(block, ModBlocks.CHUTE))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<MineralFountainBlock> MINERAL_FOUNTAIN = REGISTRUM.block("mineral_fountain", MineralFountainBlock::new)
        .initialProperties(() -> Blocks.REINFORCED_DEEPSLATE)
        .properties(p -> p.noLootTable().isValidSpawn(Blocks::never))
        .simpleItem()
        .blockstate(DataGenUtil::onlyState)
        .register();

    public static final BlockEntry<SpaceOvercompressorBlock> SPACE_OVERCOMPRESSOR = REGISTRUM.block(
            "space_overcompressor",
            SpaceOvercompressorBlock::new
        )
        .initialProperties(() -> Blocks.SHULKER_BOX)
        .properties(properties -> properties.isValidSpawn(Blocks::never).noOcclusion().pushReaction(PushReaction.NORMAL))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .item()
        .properties(properties -> properties.stacksTo(16))
        .model(DataGenUtil::blockItem)
        .build()
        .register();

    @SuppressWarnings("Convert2Lambda")
    public static BlockEntry<SlidingRailBlock> SLIDING_RAIL = REGISTRUM.block("sliding_rail", SlidingRailBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(it -> it.noOcclusion().isValidSpawn(Blocks::never).mapColor(MapColor.COLOR_GRAY))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.SLIDING_RAILS)
        .blockstate(() -> new NonNullBiConsumer<>() {
            @Override
            public void accept(
                DataGenContext<Block, SlidingRailBlock> ctx,
                RegistrumBlockModelGenerator generator
            ) {
                generator.blockStateOutput.accept(
                    MultiVariantGenerator.dispatch(ctx.get())
                        .with(
                            PropertyDispatchWrap.initial(SlidingRailBlock.AXIS)
                                .select(
                                    Direction.Axis.X,
                                    BlockModelGenerators.plainVariant(AnvilCraft.of("block/sliding_rail"))
                                        .with(BlockModelGenerators.Y_ROT_90)
                                )
                                .select(
                                    Direction.Axis.Z,
                                    BlockModelGenerators.plainVariant(AnvilCraft.of("block/sliding_rail"))
                                )
                                .select(
                                    Direction.Axis.Y,
                                    BlockModelGenerators.plainVariant(AnvilCraft.of("block/sliding_rail_cross"))
                                )
                                .dispatch()
                        ));
            }
        })
        .item()
        .model(DataGenUtil::blockItem)
        .build()
        .recipe(RegistrumBlockRecipeLoader::slidingRail)
        .register();

    public static BlockEntry<PoweredSlidingRailBlock> POWERED_SLIDING_RAIL = REGISTRUM.block(
            "powered_sliding_rail",
            PoweredSlidingRailBlock::new
        )
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(it -> it.noOcclusion().isValidSpawn(Blocks::never).mapColor(MapColor.COLOR_GRAY))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.SLIDING_RAILS)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .model(DataGenUtil::blockItem)
        .build()
        .recipe(RegistrumBlockRecipeLoader::poweredSlidingRail)
        .register();

    public static BlockEntry<ActivatorSlidingRailBlock> ACTIVATOR_SLIDING_RAIL = REGISTRUM.block(
            "activator_sliding_rail",
            ActivatorSlidingRailBlock::new
        )
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(it -> it.noOcclusion().isValidSpawn(Blocks::never).mapColor(MapColor.COLOR_GRAY))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.SLIDING_RAILS)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .model(DataGenUtil::blockItem)
        .build()
        .recipe(RegistrumBlockRecipeLoader::activatorSlidingRail)
        .register();

    public static BlockEntry<DetectorSlidingRailBlock> DETECTOR_SLIDING_RAIL = REGISTRUM.block(
            "detector_sliding_rail",
            DetectorSlidingRailBlock::new
        )
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(it -> it.mapColor(MapColor.COLOR_GRAY).noOcclusion().isValidSpawn(Blocks::never))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.SLIDING_RAILS)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .model(DataGenUtil::blockItem)
        .build()
        .recipe(RegistrumBlockRecipeLoader::detectorSlidingRail)
        .register();

    public static BlockEntry<SlidingRailStopBlock> SLIDING_RAIL_STOP = REGISTRUM.block("sliding_rail_stop", SlidingRailStopBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(it -> it.noOcclusion().isValidSpawn(Blocks::never).mapColor(MapColor.COLOR_GRAY))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(DataGenUtil::onlyState)
        .item()
        .model(DataGenUtil::blockItem)
        .build()
        .recipe(RegistrumBlockRecipeLoader::slidingRailStop)
        .register();

    public static final BlockEntry<LargeFluidTankBlock> LARGE_FLUID_TANK = REGISTRUM.block(
            "large_fluid_tank",
            LargeFluidTankBlock::new
        )
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.isSuffocating(ModBlocks::never).noOcclusion().isValidSpawn(Blocks::never))
        .loot(SimpleMultiPartBlock::loot)
        .item(SimpleMultiPartBlockItem<Cube3x3PartHalf>::new)
        .properties(properties -> properties.stacksTo(16))
        .build()
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<AccelerationRingBlock> ACCELERATION_RING = REGISTRUM.block(
            "acceleration_ring",
            AccelerationRingBlock::new
        )
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .loot(FlexibleMultiPartBlock::loot)
        .properties(it -> it.isSuffocating(ModBlocks::never).noOcclusion().isValidSpawn(Blocks::never).explosionResistance(1200))
        .item(FlexibleMultiPartBlockItem<DirectionCube3x3PartHalf, EnumProperty<Direction>, Direction>::new)
        .properties(properties -> properties.stacksTo(16))
        .build()
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<DeflectionRingBlock> DEFLECTION_RING = REGISTRUM.block("deflection_ring", DeflectionRingBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .loot(FlexibleMultiPartBlock::loot)
        .properties(it -> it.isSuffocating(ModBlocks::never).noOcclusion().isValidSpawn(Blocks::never).explosionResistance(1200))
        .item(FlexibleMultiPartBlockItem<DirectionCube3x3PartHalf, EnumProperty<Direction>, Direction>::new)
        .properties(properties -> properties.stacksTo(16))
        .build()
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<CelestialForgingAnvilBlock> CELESTIAL_FORGING_ANVIL = REGISTRUM
        .block("celestial_forging_anvil", CelestialForgingAnvilBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .loot(SimpleMultiPartBlock::loot)
        .properties(properties -> properties
            .isSuffocating(ModBlocks::never)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .explosionResistance(1200)
            .emissiveRendering(ModBlocks::always))
        .item(SimpleMultiPartBlockItem::new)
        .properties(properties -> properties.stacksTo(16))
        .build()
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag((BlockTags.MINEABLE_WITH_PICKAXE))
        .register();

    public static final BlockEntry<CelestialForgingAnvilAmplifierBlock> CELESTIAL_FORGING_ANVIL_AMPLIFIER = REGISTRUM
        .block("celestial_forging_anvil_amplifier", CelestialForgingAnvilAmplifierBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .loot(FlexibleMultiPartBlock::loot)
        .properties(properties -> properties
            .isSuffocating(ModBlocks::never)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .explosionResistance(1200)
            .emissiveRendering(ModBlocks::always))
        .item(CelestialForgingAnvilAmplifierBlockItem::new)
        .properties(properties -> properties.stacksTo(16))
        .build()
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag((BlockTags.MINEABLE_WITH_PICKAXE))
        .register();

    public static final BlockEntry<CelestialForgingAnvilLogisticsInterfaceBlock> CELESTIAL_FORGING_ANVIL_LOGISTICS_INTERFACE = REGISTRUM
        .block("celestial_forging_anvil_logistics_interface", CelestialForgingAnvilLogisticsInterfaceBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .properties(properties -> properties
            .isSuffocating(ModBlocks::never)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .explosionResistance(1200)
            .emissiveRendering(ModBlocks::always))
        .item(CelestialForgingAnvilInterfaceBlockItem::new)
        .build()
        .tag((BlockTags.MINEABLE_WITH_PICKAXE))
        .register();

    public static final BlockEntry<CelestialForgingAnvilFluidInterfaceBlock> CELESTIAL_FORGING_ANVIL_FLUID_INTERFACE = REGISTRUM
        .block("celestial_forging_anvil_fluid_interface", CelestialForgingAnvilFluidInterfaceBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .properties(properties -> properties
            .isSuffocating(ModBlocks::never)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .explosionResistance(1200)
            .emissiveRendering(ModBlocks::always))
        .item(CelestialForgingAnvilInterfaceBlockItem::new)
        .build()
        .tag((BlockTags.MINEABLE_WITH_PICKAXE))
        .register();

    public static final BlockEntry<CelestialForgingAnvilLaserInterfaceBlock> CELESTIAL_FORGING_ANVIL_LASER_INTERFACE = REGISTRUM
        .block("celestial_forging_anvil_laser_interface", CelestialForgingAnvilLaserInterfaceBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .properties(properties -> properties
            .isSuffocating(ModBlocks::never)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .explosionResistance(1200)
            .emissiveRendering(ModBlocks::always))
        .item(CelestialForgingAnvilInterfaceBlockItem::new)
        .build()
        .tag((BlockTags.MINEABLE_WITH_PICKAXE))
        .register();

    public static final BlockEntry<VoidEnergyCollectorBlock> VOID_ENERGY_COLLECTOR = REGISTRUM.block(
            "void_energy_collector",
            VoidEnergyCollectorBlock::new
        )
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never).explosionResistance(1200F))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .simpleItem()
        .recipe(RegistrumBlockRecipeLoader::voidEnergyCollector)
        .register();

    public static final BlockEntry<MagnetoElectricCoreBlock> MAGNETO_ELECTRIC_CORE_BLOCK = REGISTRUM.block(
            "magnetoelectric_core",
            MagnetoElectricCoreBlock::new
        )
        .initialProperties(() -> Blocks.COPPER_BLOCK)
        .properties(properties -> properties.lightLevel((blockState) -> 6).noOcclusion().isValidSpawn(Blocks::never))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .recipe(RegistrumBlockRecipeLoader::magnetoElectricCoreBlock)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_STONE_TOOL)
        .register();

    public static final BlockEntry<PlasmaJetsBlock> PLASMA_JETS = REGISTRUM.block("plasma_jets", PlasmaJetsBlock::new)
        .properties(properties -> properties.lightLevel(state -> 16)
            .emissiveRendering(ModBlocks::never)
            .isViewBlocking(ModBlocks::never)
            .isValidSpawn(ModBlocks::never)
            .isRedstoneConductor(ModBlocks::never)
            .noTerrainParticles()
            .replaceable()
            .noCollision()
            .strength(-1.0F, 3600000.0F)
            .noLootTable())
        .blockstate(DataGenUtil::noExtraModelOrState)
        .register();

    public static final BlockEntry<PropelPistonBlock> PROPEL_PISTON = REGISTRUM.block("propel_piston", PropelPistonBlock::new)
        .properties(properties -> {
            return properties.mapColor(MapColor.TERRACOTTA_WHITE).requiresCorrectToolForDrops().strength(1.5F).noOcclusion();
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .loot((tables, block) -> {
            tables.add(
                block,
                LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.PROPEL_PISTON))
                        .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                            .include(ModComponents.STORED_ENERGY)))
            );
        })
        .recipe(RegistrumBlockRecipeLoader::propelPiston)
        .simpleItem()
        .register();

    static {
        REGISTRUM.defaultCreativeTab(ModItemGroups.ANVILCRAFT_BUILD_BLOCK.getKey());
    }

    public static final BlockEntry<? extends Block> ROYAL_STEEL_BLOCK = REGISTRUM.block("royal_steel_block", Block::new)
        .lang("Block of Royal Steel")
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS)
        .build()
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.BEACON_BASE_BLOCKS,
            ModBlockTags.OVERSEER_BASE,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.HAMMER_REMOVABLE
        )
        .recipe(RegistrumBlockRecipeLoader::royalSteelBlock)
        .register();

    public static final BlockEntry<? extends Block> SMOOTH_ROYAL_STEEL_BLOCK = REGISTRUM.block("smooth_royal_steel_block", Block::new)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, ModBlockTags.OVERSEER_BASE, ModBlockTags.HAMMER_REMOVABLE)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .simpleItem()
        .recipe(RegistrumBlockRecipeLoader::smoothRoyalSteelBlock)
        .register();

    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_BLOCK = REGISTRUM.block("cut_royal_steel_block", Block::new)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, ModBlockTags.OVERSEER_BASE, ModBlockTags.HAMMER_REMOVABLE)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .simpleItem()
        .recipe(RegistrumBlockRecipeLoader::cutRoyalSteelBlock)
        .register();

    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_PILLAR = REGISTRUM.block(
            "cut_royal_steel_pillar",
            RotatedPillarBlock::new
        )
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, ModBlockTags.OVERSEER_BASE, ModBlockTags.HAMMER_REMOVABLE)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .recipe(RegistrumBlockRecipeLoader::cutRoyalSteelPillar)
        .register();

    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_SLAB = REGISTRUM.block("cut_royal_steel_slab", SlabBlock::new)
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.SLABS,
            ModBlockTags.OVERSEER_BASE,
            ModBlockTags.HAMMER_REMOVABLE
        )
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .blockstate(() -> DataGenUtil.slabBlock(
            _ -> new Material(AnvilCraft.of("block/cut_royal_steel_block")),
            _ -> new Material(AnvilCraft.of("block/cut_royal_steel_block")),
            _ -> new Material(AnvilCraft.of("block/cut_royal_steel_block")),
            _ -> AnvilCraft.of("block/cut_royal_steel_block")
        ))
        .item()
        .tag(ItemTags.SLABS)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createSlabItemTable(block)))
        .recipe(RegistrumBlockRecipeLoader::cutRoyalSteelSlab)
        .register();

    public static final BlockEntry<? extends Block> CUT_ROYAL_STEEL_STAIRS = REGISTRUM.block(
            "cut_royal_steel_stairs",
            properties -> new StairBlock(ModBlocks.CUT_ROYAL_STEEL_BLOCK.getDefaultState(), properties)
        )
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.STAIRS,
            ModBlockTags.OVERSEER_BASE,
            ModBlockTags.HAMMER_REMOVABLE
        )
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .blockstate(() -> DataGenUtil.stairsBlock(AnvilCraft.of("block/cut_royal_steel_block")))
        .item()
        .tag(ItemTags.STAIRS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::cutRoyalSteelStairs)
        .register();

    public static final BlockEntry<FrostMetalBlock> FROST_METAL_BLOCK = REGISTRUM.block(
            "frost_metal_block",
            FrostMetalBlock::new
        )
        .lang("Block of Frost Metal")
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(
            properties -> properties
                .lightLevel(state -> 9)
                .noOcclusion()
                .emissiveRendering(ModBlocks::always)
                .explosionResistance(1200)
        )
        .tag(
            BlockTags.BEACON_BASE_BLOCKS,
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            ModBlockTags.OVERSEER_BASE,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_FROST_METAL
        )
        .blockstate(DataGenUtil::onlyState)
        .item(FrostMetalBlockItem::new)
        .tag(Tags.Items.STORAGE_BLOCKS, ModItemTags.STORAGE_BLOCKS_FROST_METAL, ModItemTags.EXPLOSION_PROOF)
        .build()
        .recipe(RegistrumBlockRecipeLoader::frostMetalBlock)
        .defaultLoot()
        .register();

    public static final BlockEntry<FrostMetalBlock> CUT_FROST_METAL_BLOCK = REGISTRUM.block(
            "cut_frost_metal_block",
            FrostMetalBlock::new
        )
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            ModBlockTags.OVERSEER_BASE
        )
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(
            properties -> properties
                .lightLevel(state -> 9)
                .noOcclusion()
                .emissiveRendering(ModBlocks::always)
                .explosionResistance(1200)
        )
        .blockstate(DataGenUtil::onlyState)
        .item()
        .tag(ModItemTags.EXPLOSION_PROOF)
        .build()
        .recipe(RegistrumBlockRecipeLoader::cutFrostMetalBlock)
        .defaultLoot()
        .register();

    public static final BlockEntry<FrostMetalPillarBlock> CUT_FROST_METAL_PILLAR = REGISTRUM.block(
            "cut_frost_metal_pillar",
            FrostMetalPillarBlock::new
        )
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            ModBlockTags.OVERSEER_BASE
        )
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(
            properties -> properties
                .lightLevel(state -> 9)
                .noOcclusion()
                .emissiveRendering(ModBlocks::always)
                .explosionResistance(1200)
        )
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .tag(ModItemTags.EXPLOSION_PROOF)
        .build()
        .recipe(RegistrumBlockRecipeLoader::cutFrostMetalPillar)
        .defaultLoot()
        .register();

    public static final BlockEntry<FrostMetalSlabBlock> CUT_FROST_METAL_SLAB = REGISTRUM.block(
            "cut_frost_metal_slab",
            FrostMetalSlabBlock::new
        )
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.SLABS,
            ModBlockTags.OVERSEER_BASE
        )
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(
            properties -> properties
                .lightLevel(state -> 9)
                .noOcclusion()
                .emissiveRendering(ModBlocks::always)
                .explosionResistance(1200)
        )
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .tag(ItemTags.SLABS, ModItemTags.EXPLOSION_PROOF)
        .build()
        .recipe(RegistrumBlockRecipeLoader::cutFrostMetalSlab)
        .loot((tables, block) -> tables.add(block, tables.createSlabItemTable(block)))
        .register();

    public static final BlockEntry<FrostMetalStairBlock> CUT_FROST_METAL_STAIRS = REGISTRUM.block(
            "cut_frost_metal_stairs",
            properties -> new FrostMetalStairBlock(ModBlocks.CUT_FROST_METAL_BLOCK.getDefaultState(), properties)
        )
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.STAIRS,
            ModBlockTags.OVERSEER_BASE
        )
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(
            properties -> properties
                .lightLevel(state -> 9)
                .noOcclusion()
                .emissiveRendering(ModBlocks::always)
                .explosionResistance(1200)
        )
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .tag(ItemTags.STAIRS, ModItemTags.EXPLOSION_PROOF)
        .build()
        .recipe(RegistrumBlockRecipeLoader::cutFrostMetalStairs)
        .register();

    public static final BlockEntry<EmberMetalBlock> EMBER_METAL_BLOCK = REGISTRUM.block(
            "ember_metal_block",
            properties -> new EmberMetalBlock(properties, 0.5d)
        )
        .lang("Block of Ember Metal")
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 9).noOcclusion().emissiveRendering(ModBlocks::always))
        .tag(
            BlockTags.BEACON_BASE_BLOCKS,
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.HEATABLE_BLOCKS
        )
        .blockstate(DataGenUtil::onlyState)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(Tags.Items.STORAGE_BLOCKS, ModItemTags.HEATABLE_BLOCKS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::emberMetalBlock)
        .defaultLoot()
        .register();

    public static final BlockEntry<EmberMetalBlock> CUT_EMBER_METAL_BLOCK = REGISTRUM.block(
            "cut_ember_metal_block",
            properties -> new EmberMetalBlock(properties, 0.1d)
        )
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_DIAMOND_TOOL, BlockTags.WITHER_IMMUNE, BlockTags.DRAGON_IMMUNE)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 9).noOcclusion().emissiveRendering(ModBlocks::always))
        .blockstate(DataGenUtil::onlyState)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .recipe(RegistrumBlockRecipeLoader::cutEmberMetalBlock)
        .defaultLoot()
        .register();

    public static final BlockEntry<? extends Block> CUT_EMBER_METAL_PILLAR = REGISTRUM.block(
            "cut_ember_metal_pillar",
            EmberMetalPillarBlock::new
        )
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_DIAMOND_TOOL, BlockTags.WITHER_IMMUNE, BlockTags.DRAGON_IMMUNE)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 9).noOcclusion().emissiveRendering(ModBlocks::always))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .recipe(RegistrumBlockRecipeLoader::cutEmberMetalPillar)
        .register();

    public static final BlockEntry<EmberMetalSlabBlock> CUT_EMBER_METAL_SLAB = REGISTRUM.block(
            "cut_ember_metal_slab",
            EmberMetalSlabBlock::new
        )
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            BlockTags.SLABS,
            BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE
        )
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 9).noOcclusion().emissiveRendering(ModBlocks::always))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ItemTags.SLABS)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createSlabItemTable(block)))
        .recipe(RegistrumBlockRecipeLoader::cutEmberMetalSlab)
        .register();

    public static final BlockEntry<EmberMetalStairBlock> CUT_EMBER_METAL_STAIRS = REGISTRUM.block(
            "cut_ember_metal_stairs",
            properties -> new EmberMetalStairBlock(ModBlocks.CUT_EMBER_METAL_BLOCK.getDefaultState(), properties)
        )
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            BlockTags.STAIRS,
            BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE
        )
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 9).noOcclusion().emissiveRendering(ModBlocks::always))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ItemTags.STAIRS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::cutEmberMetalStairs)
        .register();

    public static final BlockEntry<TranscendiumBlock> TRANSCENDIUM_BLOCK = REGISTRUM.block("transcendium_block", TranscendiumBlock::new)
        .lang("Block of Transcendium")
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(properties -> properties.lightLevel(state -> 7).noOcclusion().emissiveRendering(ModBlocks::always))
        .tag(
            BlockTags.BEACON_BASE_BLOCKS,
            BlockTags.MINEABLE_WITH_PICKAXE,
            Tags.Blocks.NEEDS_NETHERITE_TOOL,
            BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_TRANSCENDIUM,
            ModBlockTags.COLLISION_IMMUNE
        )
        .blockstate(DataGenUtil::onlyState)
        .item()
        .properties(Item.Properties::fireResistant)
        .tag(ModItemTags.EXPLOSION_PROOF, Tags.Items.STORAGE_BLOCKS, ModItemTags.STORAGE_BLOCKS_TRANSCENDIUM)
        .build()
        .recipe(RegistrumBlockRecipeLoader::transcendiumBlock)
        .register();

    public static final BlockEntry<? extends Block> HEAVY_IRON_BLOCK = REGISTRUM.block("heavy_iron_block", Block::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.noOcclusion().strength(5.0F, 1200F))
        .blockstate(DataGenUtil::onlyState)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
        .recipe(RegistrumBlockRecipeLoader::heavyIronBlock)
        .register();

    public static final BlockEntry<? extends Block> POLISHED_HEAVY_IRON_BLOCK = REGISTRUM.block("polished_heavy_iron_block", Block::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
        .recipe(RegistrumBlockRecipeLoader::polishedHeavyIronBlock)
        .register();

    public static final BlockEntry<? extends Block> POLISHED_HEAVY_IRON_SLAB = REGISTRUM.block("polished_heavy_iron_slab", SlabBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .blockstate(() -> DataGenUtil.slabBlock(
            _ -> new Material(AnvilCraft.of("block/polished_heavy_iron_block")),
            _ -> new Material(AnvilCraft.of("block/polished_heavy_iron_block")),
            _ -> new Material(AnvilCraft.of("block/polished_heavy_iron_block")),
            ctx -> AnvilCraft.of("block/polished_heavy_iron_block")
        ))
        .item()
        .tag(ItemTags.SLABS)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createSlabItemTable(block)))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, BlockTags.SLABS)
        .recipe(RegistrumBlockRecipeLoader::polishedHeavyIronSlab)
        .register();

    public static final BlockEntry<? extends Block> POLISHED_HEAVY_IRON_STAIRS = REGISTRUM.block(
            "polished_heavy_iron_stairs",
            properties -> new StairBlock(ModBlocks.POLISHED_HEAVY_IRON_BLOCK.getDefaultState(), properties)
        )
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .blockstate(() -> DataGenUtil.stairsBlock(AnvilCraft.of("block/polished_heavy_iron_block")))
        .item()
        .tag(ItemTags.STAIRS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, BlockTags.STAIRS)
        .recipe(RegistrumBlockRecipeLoader::polishedHeavyIronStairs)
        .register();

    public static final BlockEntry<? extends Block> CUT_HEAVY_IRON_BLOCK = REGISTRUM.block("cut_heavy_iron_block", Block::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
        .recipe(RegistrumBlockRecipeLoader::cutHeavyIronBlock)
        .register();

    public static final BlockEntry<? extends Block> CUT_HEAVY_IRON_SLAB = REGISTRUM.block("cut_heavy_iron_slab", SlabBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .blockstate(() -> DataGenUtil.slabBlock(
           _ -> new Material(AnvilCraft.of("block/cut_heavy_iron_block")),
           _ -> new Material(AnvilCraft.of("block/cut_heavy_iron_block")),
           _ -> new Material(AnvilCraft.of("block/cut_heavy_iron_block")),
           _ -> AnvilCraft.of("block/cut_heavy_iron_block")
            ))
        .item()
        .tag(ItemTags.SLABS)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createSlabItemTable(block)))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, BlockTags.SLABS)
        .recipe(RegistrumBlockRecipeLoader::cutHeavyIronSlab)
        .register();

    public static final BlockEntry<? extends Block> CUT_HEAVY_IRON_STAIRS = REGISTRUM.block(
            "cut_heavy_iron_stairs",
            properties -> new StairBlock(ModBlocks.CUT_HEAVY_IRON_BLOCK.getDefaultState(), properties)
        )
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .blockstate(() -> DataGenUtil.stairsBlock(AnvilCraft.of("block/cut_heavy_iron_block")))
        .item()
        .tag(ItemTags.STAIRS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, BlockTags.STAIRS)
        .recipe(RegistrumBlockRecipeLoader::cutHeavyIronStairs)
        .register();

    public static final BlockEntry<? extends Block> HEAVY_IRON_PLATE = REGISTRUM.block("heavy_iron_plate", HeavyIronPlateBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
        .recipe(RegistrumBlockRecipeLoader::heavyIronPlate)
        .register();

    public static final BlockEntry<? extends Block> HEAVY_IRON_COLUMN = REGISTRUM.block("heavy_iron_column", Block::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
        .recipe(RegistrumBlockRecipeLoader::heavyIronColumn)
        .register();

    public static final BlockEntry<? extends Block> HEAVY_IRON_BEAM = REGISTRUM.block("heavy_iron_beam", HeavyIronBeamBlock::new)
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.strength(5.0F, 1200F))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item()
        .model(DataGenUtil::onlyInfo)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
        .recipe(RegistrumBlockRecipeLoader::heavyIronBeam)
        .register();

    @SuppressWarnings("Convert2Lambda")
    public static final BlockEntry<HeavyIronWallBlock> HEAVY_IRON_WALL = REGISTRUM.block("heavy_iron_wall", HeavyIronWallBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties.strength(5.0F, 1200F).noOcclusion())
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, BlockTags.WALLS)
        .recipe(RegistrumBlockRecipeLoader::heavyIronWall)
        .item()
        .tag(ItemTags.WALLS)
        .model(() -> new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, BlockItem> ctx, RegistrumItemModelGenerator generator) {
                generator.createWithExistingModel(ctx.get(), AnvilCraft.of("block/heavy_iron_wall_inventory"));
            }
        })
        .build()
        .register();

    public static final BlockEntry<HeavyIronDoorBlock> HEAVY_IRON_DOOR = REGISTRUM.block("heavy_iron_door", HeavyIronDoorBlock::new)
        .initialProperties(() -> Blocks.IRON_DOOR)
        .properties(properties -> properties.strength(5.0F, 1200F))
        .loot((l, b) -> {
            l.add(b, l.createDoorTable(b));
        })
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, BlockTags.DOORS)
        .recipe(RegistrumBlockRecipeLoader::heavyIronDoor)
        .item()
        .tag(ItemTags.DOORS)
        .model(DataGenUtil::flatItem)
        .build()
        .register();

    public static final BlockEntry<HeavyIronTrapdoorBlock> HEAVY_IRON_TRAPDOOR = REGISTRUM.block(
            "heavy_iron_trapdoor",
            HeavyIronTrapdoorBlock::new
        )
        .initialProperties(() -> Blocks.IRON_TRAPDOOR)
        .properties(properties -> properties.strength(5.0F, 1200F))
        .defaultLoot()
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, BlockTags.TRAPDOORS)
        .recipe(RegistrumBlockRecipeLoader::heavyIronTrapdoor)
        .item()
        .tag(ItemTags.TRAPDOORS)
        .model(() -> DataGenUtil.blockItem("_bottom"))
        .build()
        .register();

    public static final BlockEntry<? extends Block> CURSED_GOLD_BLOCK = REGISTRUM.block("cursed_gold_block", Block::new)
        .lang("Block of Cursed Gold")
        .initialProperties(() -> Blocks.GOLD_BLOCK)
        .item(CursedBlockItem::new)
        .tag(ItemTags.PIGLIN_LOVED, Tags.Items.STORAGE_BLOCKS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, BlockTags.BEACON_BASE_BLOCKS)
        .recipe(RegistrumBlockRecipeLoader::cursedGoldBlock)
        .register();

    public static final BlockEntry<? extends Block> ZINC_BLOCK = REGISTRUM.block("zinc_block", Block::new)
        .lang("Block of Zinc")
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS, Tags.Blocks.STORAGE_BLOCKS, ModBlockTags.STORAGE_BLOCKS_ZINC)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_ZINC, Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::zincBlock)
        .register();

    public static final BlockEntry<? extends Block> TIN_BLOCK = REGISTRUM.block("tin_block", Block::new)
        .lang("Block of Tin")
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS, Tags.Blocks.STORAGE_BLOCKS, ModBlockTags.STORAGE_BLOCKS_TIN)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_TIN, Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::tinBlock)
        .register();

    public static final BlockEntry<? extends Block> TITANIUM_BLOCK = REGISTRUM.block("titanium_block", Block::new)
        .lang("Block of Titanium")
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_TITANIUM
        )
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_TITANIUM, Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::titaniumBlock)
        .register();

    public static final BlockEntry<NormalBlock> TUNGSTEN_BLOCK = REGISTRUM.block("tungsten_block", NormalBlock::new)
        .lang("Block of Tungsten")
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.STORAGE_BLOCKS_TUNGSTEN, Tags.Items.STORAGE_BLOCKS, ModItemTags.HEATABLE_BLOCKS)
        .build()
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_TUNGSTEN,
            ModBlockTags.HEATABLE_BLOCKS
        )
        .recipe(RegistrumBlockRecipeLoader::tungstenBlock)
        .register();

    public static final BlockEntry<? extends Block> LEAD_BLOCK = REGISTRUM.block("lead_block", Block::new)
        .lang("Block of Lead")
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS, Tags.Blocks.STORAGE_BLOCKS, ModBlockTags.STORAGE_BLOCKS_LEAD)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_LEAD, Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::leadBlock)
        .register();

    public static final BlockEntry<? extends Block> SILVER_BLOCK = REGISTRUM.block("silver_block", Block::new)
        .lang("Block of Silver")
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS, Tags.Blocks.STORAGE_BLOCKS, ModBlockTags.STORAGE_BLOCKS_SILVER)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_SILVER, Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::silverBlock)
        .register();

    public static final BlockEntry<? extends Block> URANIUM_BLOCK = REGISTRUM.block("uranium_block", Block::new)
        .lang("Block of Uranium")
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS, Tags.Blocks.STORAGE_BLOCKS, ModBlockTags.STORAGE_BLOCKS_URANIUM)
        .item(RadiationBlockItem::new)
        .tag(ModItemTags.STORAGE_BLOCKS_URANIUM, Tags.Items.STORAGE_BLOCKS, ModItemTags.RADIATIONS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::uraniumBlock)
        .register();

    public static final BlockEntry<? extends Block> PLUTONIUM_BLOCK = REGISTRUM.block("plutonium_block", Block::new)
        .lang("Block of Plutonium")
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_PLUTONIUM
        )
        .item(RadiationBlockItem::new)
        .tag(ModItemTags.STORAGE_BLOCKS_PLUTONIUM, Tags.Items.STORAGE_BLOCKS, ModItemTags.RADIATIONS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::plutoniumBlock)
        .register();

    public static final BlockEntry<? extends Block> BRONZE_BLOCK = REGISTRUM.block("bronze_block", Block::new)
        .lang("Block of Bronze")
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS, Tags.Blocks.STORAGE_BLOCKS, ModBlockTags.STORAGE_BLOCKS_BRONZE)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_BRONZE, Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::bronzeBlock)
        .register();

    public static final BlockEntry<? extends Block> BRASS_BLOCK = REGISTRUM.block("brass_block", Block::new)
        .lang("Block of Brass")
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS, Tags.Blocks.STORAGE_BLOCKS, ModBlockTags.STORAGE_BLOCKS_BRASS)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_BRASS, Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::brassBlock)
        .register();

    public static final BlockEntry<? extends Block> TOPAZ_BLOCK = REGISTRUM.block("topaz_block", Block::new)
        .lang("Block of Topaz")
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS, ModItemTags.STORAGE_BLOCKS_TOPAZ)
        .build()
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_TOPAZ
        )
        .recipe(RegistrumBlockRecipeLoader::topazBlock)
        .register();

    public static final BlockEntry<? extends Block> RUBY_BLOCK = REGISTRUM.block("ruby_block", Block::new)
        .lang("Block of Ruby")
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS, ModItemTags.STORAGE_BLOCKS_RUBY)
        .build()
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_RUBY
        )
        .recipe(RegistrumBlockRecipeLoader::rubyBlock)
        .register();

    public static final BlockEntry<? extends Block> SAPPHIRE_BLOCK = REGISTRUM.block("sapphire_block", Block::new)
        .lang("Block of Sapphire")
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS, ModItemTags.STORAGE_BLOCKS_SAPPHIRE)
        .build()
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_SAPPHIRE
        )
        .recipe(RegistrumBlockRecipeLoader::sapphireBlock)
        .register();

    public static final BlockEntry<? extends Block> CHROMATIC_STONE = REGISTRUM.block("chromatic_stone", Block::new)
        .lang("Chromatic Stone")
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
        .simpleItem()
        .register();

    public static final BlockEntry<? extends Block> EXP_GEM_BLOCK = REGISTRUM.block("exp_gem_block", Block::new)
        .lang("Block of Experience Gem")
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS, ModItemTags.STORAGE_BLOCKS_EXP_GEM)
        .build()
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_IRON_TOOL,
            BlockTags.BEACON_BASE_BLOCKS,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_EXP_GEM
        )
        .recipe(RegistrumBlockRecipeLoader::expGemBlock)
        .register();

    public static final BlockEntry<? extends Block> RESIN_BLOCK = REGISTRUM.block("resin_block", ResinBlock::new)
        .lang("Block of Resin")
        .initialProperties(() -> Blocks.SLIME_BLOCK)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .properties(properties -> properties.sound(SoundType.HONEY_BLOCK))
        .item(ResinBlockItem::new)
        .tag(Tags.Items.STORAGE_BLOCKS, ModItemTags.STORAGE_BLOCKS_RESIN)
        .build()
        .tag(Tags.Blocks.STORAGE_BLOCKS, ModBlockTags.STORAGE_BLOCKS_RESIN)
        .recipe(RegistrumBlockRecipeLoader::resinBlock)
        .register();

    public static final BlockEntry<? extends Block> AMBER_BLOCK = REGISTRUM.block("amber_block", AmberBlock::new)
        .lang("Block of Amber")
        .initialProperties(() -> Blocks.EMERALD_BLOCK)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .properties(properties -> properties.noOcclusion().pushReaction(PushReaction.DESTROY))
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS, ModItemTags.STORAGE_BLOCKS_AMBER)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, Tags.Blocks.STORAGE_BLOCKS, ModBlockTags.STORAGE_BLOCKS_AMBER)
        .recipe(RegistrumBlockRecipeLoader::amberBlock)
        .register();

    public static final BlockEntry<MobAmberBlock> MOB_AMBER_BLOCK = REGISTRUM.block("mob_amber_block", MobAmberBlock::new)
        .lang("Block of Amber with Mob")
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item(HasMobBlockItem::new)
        .build()
        .initialProperties(ModBlocks.AMBER_BLOCK)
        .loot((ctx, prov) -> {
            LootTable.Builder builder = LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(ModBlocks.MOB_AMBER_BLOCK))
                    .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                        .include(ModComponents.SAVED_ENTITY)));
            ctx.add(prov, builder);
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();

    public static final BlockEntry<ResentfulAmberBlock> RESENTFUL_AMBER_BLOCK = REGISTRUM.block(
            "resentful_amber_block",
            ResentfulAmberBlock::new
        )
        .lang("Resentful Block of Amber")
        .blockstate(DataGenUtil::noExtraModelOrState)
        .item(HasMobBlockItem::new)
        .build()
        .initialProperties(ModBlocks.AMBER_BLOCK)
        .loot((ctx, prov) -> {
            LootTable.Builder builder = LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(ModBlocks.RESENTFUL_AMBER_BLOCK))
                    .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                        .include(ModComponents.SAVED_ENTITY)));
            ctx.add(prov, builder);
        })
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .register();
    public static final BlockEntry<? extends Block> TEMPERING_GLASS = REGISTRUM.block("tempering_glass", TransparentBlock::new)
        .initialProperties(() -> Blocks.GLASS)
        .properties(properties -> properties.explosionResistance(1200.0F)
            .noOcclusion()
            .isValidSpawn(ModBlocks::never)
            .isRedstoneConductor(ModBlocks::never)
            .isSuffocating(ModBlocks::never)
            .isViewBlocking(ModBlocks::never))
        .blockstate(DataGenUtil::transparentBlock)
        .item()
        .tag(Tags.Items.GLASS_BLOCKS)
        .build()
        .tag(Tags.Blocks.GLASS_BLOCKS)
        .register();

    public static final BlockEntry<? extends Block> FROST_GLASS = REGISTRUM.block("frost_glass", TransparentBlock::new)
        .initialProperties(() -> Blocks.GLASS)
        .properties(properties -> properties.explosionResistance(1200)
            .noOcclusion()
            .isValidSpawn(ModBlocks::never)
            .isRedstoneConductor(ModBlocks::never)
            .isSuffocating(ModBlocks::never)
            .isViewBlocking(ModBlocks::never))
        .blockstate(DataGenUtil::transparentBlock)
        .tag(Tags.Blocks.GLASS_BLOCKS)
        .item()
        .tag(Tags.Items.GLASS_BLOCKS)
        .build()
        .register();

    public static final BlockEntry<? extends Block> EMBER_GLASS = REGISTRUM.block("ember_glass", TransparentBlock::new)
        .initialProperties(() -> Blocks.GLASS)
        .properties(properties -> properties.explosionResistance(1200)
            .noOcclusion()
            .isValidSpawn(ModBlocks::never)
            .isRedstoneConductor(ModBlocks::never)
            .isSuffocating(ModBlocks::never)
            .isViewBlocking(ModBlocks::never))
        .blockstate(DataGenUtil::transparentBlock)
        .tag(BlockTags.WITHER_IMMUNE, BlockTags.DRAGON_IMMUNE, Tags.Blocks.GLASS_BLOCKS)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(Tags.Items.GLASS_BLOCKS)
        .build()
        .register();

    public static final BlockEntry<? extends Block> CINERITE = REGISTRUM.block(
            "cinerite",
            b -> new ColoredFallingBlock(new ColorRGBA(0xDEDEDE), b)
        )
        .initialProperties(() -> Blocks.SAND)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<? extends Block> QUARTZ_SAND = REGISTRUM.block(
            "quartz_sand",
            b -> new ColoredFallingBlock(new ColorRGBA(0xFFFFCD), b)
        )
        .initialProperties(() -> Blocks.SAND)
        .simpleItem()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<LevitationPowderBlock> LEVITATION_POWDER_BLOCK = REGISTRUM.block(
            "levitation_powder_block",
            LevitationPowderBlock::new
        )
        .lang("Block of Levitation Powder")
        .initialProperties(() -> Blocks.SAND)
        .item(LevitationBlockItem::new)
        .tag(ModItemTags.LEVITATIONALS)
        .recipe(RegistrumBlockRecipeLoader::levitationPowderBlock)
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL, ModBlockTags.NEUTRONIUM_CANNOT_PASS_THROUGH)
        .register();

    public static final BlockEntry<ControllableSandBlock> CONTROLLABLE_SAND = REGISTRUM.block(
            "controllable_sand",
            ControllableSandBlock::new
        )
        .initialProperties(() -> Blocks.SAND)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate(DataGenUtil::onlyState)
        .simpleItem()
        .recipe(RegistrumBlockRecipeLoader::controllableSand)
        .tag(BlockTags.MINEABLE_WITH_SHOVEL, ModBlockTags.NEUTRONIUM_CANNOT_PASS_THROUGH)
        .register();

    public static final BlockEntry<ColoredFallingBlock> NETHER_DUST = REGISTRUM.block(
            "nether_dust",
            b -> new ColoredFallingBlock(new ColorRGBA(0x8B0000), b)
        )
        .simpleItem()
        .initialProperties(() -> Blocks.BLACK_CONCRETE_POWDER)
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<EndDustBlock> END_DUST = REGISTRUM.block("end_dust", EndDustBlock::new)
        .item(EndDustBlockItem::new)
        .build()
        .initialProperties(() -> Blocks.BLACK_CONCRETE_POWDER)
        .tag(BlockTags.MINEABLE_WITH_SHOVEL, ModBlockTags.VOID_DECAY_PRODUCTS)
        .register();

    public static final BlockEntry<ArrowBlock> ARROW = REGISTRUM.block("arrow", ArrowBlock::new)
        .initialProperties(() -> Blocks.STONE)
        .properties(p -> p.noOcclusion().noCollision().lightLevel(bs -> 10))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .register();

    public static final BlockEntry<CakeBaseBlock> CAKE_BASE_BLOCK = REGISTRUM.block("cake_base_block", CakeBaseBlock::new)
        .initialProperties(() -> Blocks.CAKE)
        .item()
        .tag(Tags.Items.FOODS, Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<CreamBlock> CREAM_BLOCK = REGISTRUM.block("cream_block", CreamBlock::new)
        .initialProperties(() -> Blocks.CAKE)
        .item()
        .tag(Tags.Items.FOODS, Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<BerryCreamBlock> BERRY_CREAM_BLOCK = REGISTRUM.block("berry_cream_block", BerryCreamBlock::new)
        .initialProperties(() -> Blocks.CAKE)
        .item()
        .tag(Tags.Items.FOODS, Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<ChocolateCreamBlock> CHOCOLATE_CREAM_BLOCK = REGISTRUM.block(
            "chocolate_cream_block",
            ChocolateCreamBlock::new
        )
        .initialProperties(() -> Blocks.CAKE)
        .item()
        .tag(Tags.Items.FOODS, Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<CakeBlock> CAKE_BLOCK = REGISTRUM.block("cake_block", CakeBlock::new)
        .initialProperties(() -> Blocks.CAKE)
        .blockstate(DataGenUtil::onlyState)
        .item()
        .tag(Tags.Items.FOODS, Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<BerryCakeBlock> BERRY_CAKE_BLOCK = REGISTRUM.block("berry_cake_block", BerryCakeBlock::new)
        .initialProperties(() -> Blocks.CAKE)
        .blockstate(DataGenUtil::onlyState)
        .item()
        .tag(Tags.Items.FOODS, Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<ChocolateCakeBlock> CHOCOLATE_CAKE_BLOCK = REGISTRUM.block(
            "chocolate_cake_block",
            ChocolateCakeBlock::new
        )
        .initialProperties(() -> Blocks.CAKE)
        .blockstate(DataGenUtil::onlyState)
        .item()
        .tag(Tags.Items.FOODS, Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .tag(BlockTags.MINEABLE_WITH_SHOVEL)
        .register();

    public static final BlockEntry<LargeCakeBlock> LARGE_CAKE = REGISTRUM.block("large_cake", LargeCakeBlock::new)
        .initialProperties(() -> Blocks.CAKE)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .loot((ctx, prov) -> {
            LootTable.Builder builder = LootTable.lootTable().setRandomSequence(Identifier.withDefaultNamespace("blocks/large_cake"));
            ctx.add(prov, builder);
        })
        .item(SimpleMultiPartBlockItem<Cube3x3PartHalf>::new)
        .properties(properties -> properties.stacksTo(16))
        .tag(Tags.Items.FOODS, Tags.Items.FOODS_EDIBLE_WHEN_PLACED)
        .build()
        .register();

    public static final BlockEntry<StepEffectBlock> CHOCOLATE_BLOCK = REGISTRUM.block(
            "chocolate_block",
            p -> new StepEffectBlock(p, StepEffectBlock::stepOnChocolateBlock)
        )
        .lang("Block of Chocolate")
        .initialProperties(() -> Blocks.STONE)
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, Tags.Blocks.STORAGE_BLOCKS)
        .recipe(RegistrumBlockRecipeLoader::chocolateBlock)
        .register();

    public static final BlockEntry<StepEffectBlock> BLACK_CHOCOLATE_BLOCK = REGISTRUM.block(
            "black_chocolate_block",
            p -> new StepEffectBlock(p, StepEffectBlock::stepOnBlackChocolateBlock)
        )
        .lang("Block of Black Chocolate")
        .initialProperties(() -> Blocks.STONE)
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, Tags.Blocks.STORAGE_BLOCKS)
        .recipe(RegistrumBlockRecipeLoader::blackChocolateBlock)
        .register();

    public static final BlockEntry<StepEffectBlock> WHITE_CHOCOLATE_BLOCK = REGISTRUM.block(
            "white_chocolate_block",
            p -> new StepEffectBlock(p, StepEffectBlock::stepOnWhiteChocolateBlock)
        )
        .lang("Block of White Chocolate")
        .initialProperties(() -> Blocks.STONE)
        .item()
        .tag(Tags.Items.STORAGE_BLOCKS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, Tags.Blocks.STORAGE_BLOCKS)
        .recipe(RegistrumBlockRecipeLoader::whiteChocolateBlock)
        .register();

    public static final BlockEntry<StepEffectSlabBlock> CHOCOLATE_SLAB = REGISTRUM.block(
            "chocolate_slab",
            p -> new StepEffectSlabBlock(p, StepEffectBlock::stepOnChocolateBlock)
        )
        .initialProperties(() -> Blocks.STONE)
        .item()
        .tag(ItemTags.SLABS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.SLABS)
        .blockstate(() -> DataGenUtil.slabBlock(AnvilCraft.of("block/chocolate_block")))
        .recipe(RegistrumBlockRecipeLoader::chocolateSlab)
        .register();

    public static final BlockEntry<StepEffectSlabBlock> BLACK_CHOCOLATE_SLAB = REGISTRUM.block(
            "black_chocolate_slab",
            p -> new StepEffectSlabBlock(p, StepEffectBlock::stepOnBlackChocolateBlock)
        )
        .initialProperties(() -> Blocks.STONE)
        .item()
        .tag(ItemTags.SLABS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.SLABS)
        .blockstate(() -> DataGenUtil.slabBlock(AnvilCraft.of("block/black_chocolate_block")))
        .recipe(RegistrumBlockRecipeLoader::blackChocolateSlab)
        .register();

    public static final BlockEntry<StepEffectSlabBlock> WHITE_CHOCOLATE_SLAB = REGISTRUM.block(
            "white_chocolate_slab",
            p -> new StepEffectSlabBlock(p, StepEffectBlock::stepOnWhiteChocolateBlock)
        )
        .initialProperties(() -> Blocks.STONE)
        .item()
        .tag(ItemTags.SLABS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.SLABS)
        .blockstate(() -> DataGenUtil.slabBlock(AnvilCraft.of("block/white_chocolate_block")))
        .recipe(RegistrumBlockRecipeLoader::whiteChocolateSlab)
        .register();

    public static final BlockEntry<StepEffectStairBlock> CHOCOLATE_STAIRS = REGISTRUM.block(
            "chocolate_stairs",
            p -> new StepEffectStairBlock(ModBlocks.CHOCOLATE_BLOCK.getDefaultState(), p, StepEffectBlock::stepOnChocolateBlock)
        )
        .initialProperties(() -> Blocks.STONE)
        .item()
        .tag(ItemTags.STAIRS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.STAIRS)
        .blockstate(() -> DataGenUtil.stairsBlock(AnvilCraft.of("block/chocolate_block")))
        .recipe(RegistrumBlockRecipeLoader::chocolateStairs)
        .register();

    public static final BlockEntry<StepEffectStairBlock> BLACK_CHOCOLATE_STAIRS = REGISTRUM.block(
            "black_chocolate_stairs",
            p -> new StepEffectStairBlock(ModBlocks.BLACK_CHOCOLATE_BLOCK.getDefaultState(), p, StepEffectBlock::stepOnBlackChocolateBlock)
        )
        .initialProperties(() -> Blocks.STONE)
        .item()
        .tag(ItemTags.STAIRS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.STAIRS)
        .blockstate(() -> DataGenUtil.stairsBlock(AnvilCraft.of("block/black_chocolate_block")))
        .recipe(RegistrumBlockRecipeLoader::blackChocolateStairs)
        .register();

    public static final BlockEntry<StepEffectStairBlock> WHITE_CHOCOLATE_STAIRS = REGISTRUM.block(
            "white_chocolate_stairs",
            p -> new StepEffectStairBlock(ModBlocks.WHITE_CHOCOLATE_BLOCK.getDefaultState(), p, StepEffectBlock::stepOnWhiteChocolateBlock)
        )
        .initialProperties(() -> Blocks.STONE)
        .item()
        .tag(ItemTags.STAIRS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.STAIRS)
        .blockstate(() -> DataGenUtil.stairsBlock(AnvilCraft.of("block/white_chocolate_block")))
        .recipe(RegistrumBlockRecipeLoader::whiteChocolateStairs)
        .register();

    public static final Object2ObjectMap<Color, BlockEntry<ReinforcedConcreteBlock>> REINFORCED_CONCRETES = registerReinforcedConcretes();
    public static final Object2ObjectMap<Color, BlockEntry<SlabBlock>> REINFORCED_CONCRETE_SLABS = registerReinforcedConcreteSlabs();
    public static final Object2ObjectMap<Color, BlockEntry<StairBlock>> REINFORCED_CONCRETE_STAIRS = registerReinforcedConcreteStairs();
    public static final Object2ObjectMap<Color, BlockEntry<WallBlock>> REINFORCED_CONCRETE_WALLS = registerReinforcedConcreteWalls();

    public static final BlockEntry<HeatedBlock> HEATED_NETHERITE_BLOCK = REGISTRUM.block("heated_netherite_block", HeatedBlock::new)
        .lang("Heated Block of Netherite")
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .item(HeatableBlockItem::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.HEATABLE_BLOCKS)
        .build()
        .loot((tables, block) -> DataGenUtil.dropOtherAndSelfWhenSilkTouch(tables, block, Blocks.NETHERITE_BLOCK))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_DIAMOND_TOOL, ModBlockTags.HEATABLE_BLOCKS, ModBlockTags.HEATED_BLOCKS)
        .register();

    public static final BlockEntry<HeatedBlock> HEATED_TUNGSTEN_BLOCK = REGISTRUM.block("heated_tungsten_block", HeatedBlock::new)
        .lang("Heated Block of Tungsten")
        .initialProperties(ModBlocks.TUNGSTEN_BLOCK)
        .item(HeatableBlockItem::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.HEATABLE_BLOCKS)
        .build()
        .loot((tables, block) -> DataGenUtil.dropOtherAndSelfWhenSilkTouch(tables, block, ModBlocks.TUNGSTEN_BLOCK))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.HEATABLE_BLOCKS, ModBlockTags.HEATED_BLOCKS)
        .register();

    public static final BlockEntry<RedhotBlock> REDHOT_NETHERITE_BLOCK = REGISTRUM.block("redhot_netherite_block", RedhotBlock::new)
        .lang("Redhot Block of Netherite")
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.lightLevel(it -> 3))
        .item(HeatableBlockItem::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.HEATABLE_BLOCKS)
        .build()
        .loot((tables, block) -> DataGenUtil.dropOtherAndSelfWhenSilkTouch(tables, block, ModBlocks.HEATED_NETHERITE_BLOCK))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_DIAMOND_TOOL, ModBlockTags.HEATABLE_BLOCKS, ModBlockTags.REDHOT_BLOCKS)
        .register();

    public static final BlockEntry<RedhotBlock> REDHOT_TUNGSTEN_BLOCK = REGISTRUM.block("redhot_tungsten_block", RedhotBlock::new)
        .lang("Redhot Block of Tungsten")
        .initialProperties(ModBlocks.TUNGSTEN_BLOCK)
        .properties(p -> p.lightLevel(it -> 3))
        .item(HeatableBlockItem::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.HEATABLE_BLOCKS)
        .build()
        .loot((tables, block) -> DataGenUtil.dropOtherAndSelfWhenSilkTouch(tables, block, ModBlocks.HEATED_TUNGSTEN_BLOCK))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.HEATABLE_BLOCKS, ModBlockTags.REDHOT_BLOCKS)
        .register();

    public static final BlockEntry<GlowingBlock> GLOWING_NETHERITE_BLOCK = REGISTRUM.block("glowing_netherite_block", GlowingBlock::new)
        .lang("Glowing Block of Netherite")
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.lightLevel(it -> 7))
        .item(HeatableBlockItem::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.HEATABLE_BLOCKS)
        .build()
        .loot((tables, block) -> DataGenUtil.dropOtherAndSelfWhenSilkTouch(tables, block, ModBlocks.REDHOT_NETHERITE_BLOCK))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_DIAMOND_TOOL, ModBlockTags.HEATABLE_BLOCKS, ModBlockTags.GLOWING_BLOCKS)
        .register();

    public static final BlockEntry<GlowingBlock> GLOWING_TUNGSTEN_BLOCK = REGISTRUM.block("glowing_tungsten_block", GlowingBlock::new)
        .lang("Glowing Block of Tungsten")
        .initialProperties(ModBlocks.TUNGSTEN_BLOCK)
        .properties(p -> p.lightLevel(it -> 7))
        .item(HeatableBlockItem::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.HEATABLE_BLOCKS)
        .build()
        .loot((tables, block) -> DataGenUtil.dropOtherAndSelfWhenSilkTouch(tables, block, ModBlocks.REDHOT_TUNGSTEN_BLOCK))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.HEATABLE_BLOCKS, ModBlockTags.GLOWING_BLOCKS)
        .register();

    public static final BlockEntry<IncandescentBlock> INCANDESCENT_NETHERITE_BLOCK = REGISTRUM.block(
            "incandescent_netherite_block",
            IncandescentBlock::new
        )
        .lang("Incandescent Block of Netherite")
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .properties(p -> p.lightLevel(it -> 15))
        .item(HeatableBlockItem::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.HEATABLE_BLOCKS)
        .build()
        .loot((tables, block) -> DataGenUtil.dropOtherAndSelfWhenSilkTouch(tables, block, ModBlocks.GLOWING_NETHERITE_BLOCK))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_DIAMOND_TOOL, ModBlockTags.HEATABLE_BLOCKS, ModBlockTags.INCANDESCENT_BLOCKS)
        .register();

    public static final BlockEntry<IncandescentBlock> INCANDESCENT_TUNGSTEN_BLOCK = REGISTRUM.block(
            "incandescent_tungsten_block",
            IncandescentBlock::new
        )
        .lang("Incandescent Block of Tungsten")
        .initialProperties(ModBlocks.TUNGSTEN_BLOCK)
        .properties(p -> p.lightLevel(it -> 15))
        .item(HeatableBlockItem::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.HEATABLE_BLOCKS)
        .build()
        .loot((tables, block) -> DataGenUtil.dropOtherAndSelfWhenSilkTouch(tables, block, ModBlocks.GLOWING_TUNGSTEN_BLOCK))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.HEATABLE_BLOCKS, ModBlockTags.INCANDESCENT_BLOCKS)
        .register();

    public static final BlockEntry<OverheatedEmberMetalBlock> OVERHEATED_EMBER_METAL_BLOCK = REGISTRUM.block(
            "overheated_ember_metal_block",
            OverheatedEmberMetalBlock::new
        )
        .lang("Overheated Block of Ember Metal")
        .initialProperties(ModBlocks.EMBER_METAL_BLOCK)
        .blockstate(DataGenUtil::onlyState)
        .item(HeatableBlockItem::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.HEATABLE_BLOCKS, ModItemTags.EXPLOSION_PROOF)
        .build()
        .loot((tables, block) -> DataGenUtil.dropOtherAndSelfWhenSilkTouch(tables, block, ModBlocks.EMBER_METAL_BLOCK))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            BlockTags.WITHER_IMMUNE,
            BlockTags.DRAGON_IMMUNE,
            ModBlockTags.HEATABLE_BLOCKS,
            ModBlockTags.OVERHEATED_BLOCKS
        )
        .register();

    public static final BlockEntry<Block> RAW_ZINC_BLOCK = REGISTRUM.block("raw_zinc_block", Block::new)
        .lang("Block of Raw Zinc")
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe(RegistrumBlockRecipeLoader::rawZincBlock)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, Tags.Blocks.STORAGE_BLOCKS, ModBlockTags.STORAGE_BLOCKS_RAW_ZINC)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_RAW_ZINC, Tags.Items.STORAGE_BLOCKS)
        .build()
        .register();

    public static final BlockEntry<Block> RAW_TIN_BLOCK = REGISTRUM.block("raw_tin_block", Block::new)
        .lang("Block of Raw Tin")
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe(RegistrumBlockRecipeLoader::rawTinBlock)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.STORAGE_BLOCKS_RAW_TIN)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_RAW_TIN, Tags.Items.STORAGE_BLOCKS)
        .build()
        .register();

    public static final BlockEntry<Block> RAW_TITANIUM_BLOCK = REGISTRUM.block("raw_titanium_block", Block::new)
        .lang("Block of Raw Titanium")
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe(RegistrumBlockRecipeLoader::rawTitaniumBlock)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.STORAGE_BLOCKS_RAW_TITANIUM)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_RAW_TITANIUM, Tags.Items.STORAGE_BLOCKS)
        .build()
        .register();

    public static final BlockEntry<Block> RAW_TUNGSTEN_BLOCK = REGISTRUM.block("raw_tungsten_block", Block::new)
        .lang("Block of Raw Tungsten")
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe(RegistrumBlockRecipeLoader::rawTungstenBlock)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.STORAGE_BLOCKS_RAW_TUNGSTEN, Tags.Items.STORAGE_BLOCKS)
        .build()
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.STORAGE_BLOCKS_RAW_TUNGSTEN)
        .register();

    public static final BlockEntry<Block> RAW_LEAD_BLOCK = REGISTRUM.block("raw_lead_block", Block::new)
        .lang("Block of Raw Lead")
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe(RegistrumBlockRecipeLoader::rawLeadBlock)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.STORAGE_BLOCKS_RAW_LEAD)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_RAW_LEAD, Tags.Items.STORAGE_BLOCKS)
        .build()
        .register();

    public static final BlockEntry<Block> RAW_SILVER_BLOCK = REGISTRUM.block("raw_silver_block", Block::new)
        .lang("Block of Raw Silver")
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe(RegistrumBlockRecipeLoader::rawSilverBlock)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.STORAGE_BLOCKS_RAW_SILVER)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_RAW_SILVER, Tags.Items.STORAGE_BLOCKS)
        .build()
        .register();

    public static final BlockEntry<Block> RAW_URANIUM_BLOCK = REGISTRUM.block("raw_uranium_block", Block::new)
        .lang("Block of Raw Uranium")
        .initialProperties(() -> Blocks.RAW_IRON_BLOCK)
        .recipe(RegistrumBlockRecipeLoader::rawUraniumBlock)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.STORAGE_BLOCKS_RAW_URANIUM)
        .item(RadiationBlockItem::new)
        .tag(ModItemTags.STORAGE_BLOCKS_RAW_URANIUM, Tags.Items.STORAGE_BLOCKS, ModItemTags.RADIATIONS)
        .build()
        .register();

    public static final BlockEntry<Block> DEEPSLATE_ZINC_ORE = REGISTRUM.block("deepslate_zinc_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .item()
        .tag(Tags.Items.ORES, ModItemTags.ZINC_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_ZINC.get())))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.ORES_ZINC,
            Tags.Blocks.ORES,
            Tags.Blocks.ORES_IN_GROUND_DEEPSLATE,
            ModBlockTags.VOID_DECAY_PRODUCTS
        )
        .register();

    public static final BlockEntry<Block> DEEPSLATE_TIN_ORE = REGISTRUM.block("deepslate_tin_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .item()
        .tag(Tags.Items.ORES, ModItemTags.TIN_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_TIN.get())))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.ORES_TIN,
            Tags.Blocks.ORES,
            Tags.Blocks.ORES_IN_GROUND_DEEPSLATE,
            ModBlockTags.VOID_DECAY_PRODUCTS
        )
        .register();

    public static final BlockEntry<Block> DEEPSLATE_TITANIUM_ORE = REGISTRUM.block("deepslate_titanium_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .item()
        .tag(Tags.Items.ORES, ModItemTags.TITANIUM_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_TITANIUM.get())))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.ORES_TITANIUM, Tags.Blocks.ORES, Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
        .register();

    public static final BlockEntry<Block> DEEPSLATE_TUNGSTEN_ORE = REGISTRUM.block("deepslate_tungsten_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .item()
        .tag(Tags.Items.ORES, ModItemTags.TUNGSTEN_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_TUNGSTEN.get())))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.ORES_TUNGSTEN, Tags.Blocks.ORES, Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
        .register();

    public static final BlockEntry<Block> DEEPSLATE_LEAD_ORE = REGISTRUM.block("deepslate_lead_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .item()
        .tag(Tags.Items.ORES, ModItemTags.LEAD_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_LEAD.get())))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.ORES_LEAD,
            Tags.Blocks.ORES,
            Tags.Blocks.ORES_IN_GROUND_DEEPSLATE,
            ModBlockTags.VOID_DECAY_PRODUCTS
        )
        .register();

    public static final BlockEntry<Block> DEEPSLATE_SILVER_ORE = REGISTRUM.block("deepslate_silver_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .item()
        .tag(Tags.Items.ORES, ModItemTags.SILVER_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_SILVER.get())))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.ORES_SILVER, Tags.Blocks.ORES, Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
        .register();

    public static final BlockEntry<Block> DEEPSLATE_URANIUM_ORE = REGISTRUM.block("deepslate_uranium_ore", Block::new)
        .initialProperties(() -> Blocks.DEEPSLATE_IRON_ORE)
        .item(RadiationBlockItem::new)
        .tag(Tags.Items.ORES, ModItemTags.URANIUM_ORES, ModItemTags.RADIATIONS)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.RAW_URANIUM.get())))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.ORES_URANIUM, Tags.Blocks.ORES, Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
        .register();

    public static final BlockEntry<Block> VOID_STONE = REGISTRUM.block("void_stone", Block::new)
        .initialProperties(() -> Blocks.DIAMOND_BLOCK)
        .item()
        .tag(Tags.Items.ORES, ModItemTags.VOID_RESISTANT, ModItemTags.VOID_MATTER_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.VOID_MATTER.get())))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            ModBlockTags.ORES_VOID_MATTER,
            Tags.Blocks.ORES,
            Tags.Blocks.ORES_IN_GROUND_DEEPSLATE,
            ModBlockTags.VOID_DECAY_PRODUCTS
        )
        .register();

    public static final BlockEntry<Block> EARTH_CORE_SHARD_ORE = REGISTRUM.block("earth_core_shard_ore", Block::new)
        .initialProperties(() -> Blocks.DIAMOND_BLOCK)
        .properties(properties -> properties.explosionResistance(1200))
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(Tags.Items.ORES, ModItemTags.EARTH_CORE_SHARD_ORES)
        .build()
        .loot((tables, block) -> tables.add(block, tables.createOreDrop(block, ModItems.EARTH_CORE_SHARD.get())))
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            ModBlockTags.ORES_EARTH_CORE_SHARD,
            Tags.Blocks.ORES,
            Tags.Blocks.ORES_IN_GROUND_DEEPSLATE
        )
        .register();

    public static final BlockEntry<Block> STURDY_DEEPSLATE = REGISTRUM.block("sturdy_deepslate", Block::new)
        .initialProperties(() -> Blocks.REINFORCED_DEEPSLATE)
        .properties(properties -> properties.noLootTable().pushReaction(PushReaction.BLOCK))
        .simpleItem()
        .loot((tables, block) -> {
        })
        .tag(BlockTags.WITHER_IMMUNE, BlockTags.DRAGON_IMMUNE)
        .register();

    public static final BlockEntry<VoidMatterBlock> VOID_MATTER_BLOCK = REGISTRUM.block("void_matter_block", VoidMatterBlock::new)
        .lang("Block of Void Matter")
        .initialProperties(() -> Blocks.DIAMOND_BLOCK)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .blockstate(DataGenUtil::onlyState)
        .recipe(RegistrumBlockRecipeLoader::voidMatterBlock)
        .item()
        .tag(ModItemTags.VOID_RESISTANT, ModItemTags.STORAGE_BLOCKS_VOID_MATTER, Tags.Items.STORAGE_BLOCKS)
        .build()
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            ModBlockTags.STORAGE_BLOCKS_VOID_MATTER,
            Tags.Blocks.STORAGE_BLOCKS
        )
        .register();

    public static final BlockEntry<Block> EARTH_CORE_SHARD_BLOCK = REGISTRUM.block("earth_core_shard_block", Block::new)
        .lang("Block of Earth Core Shard")
        .initialProperties(() -> Blocks.DIAMOND_BLOCK)
        .properties(properties -> properties.explosionResistance(1200))
        .recipe(RegistrumBlockRecipeLoader::earthCoreShardBlock)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.STORAGE_BLOCKS_EARTH_CORE_SHARD, Tags.Items.STORAGE_BLOCKS)
        .build()
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.NEEDS_DIAMOND_TOOL,
            ModBlockTags.STORAGE_BLOCKS_EARTH_CORE_SHARD,
            Tags.Blocks.STORAGE_BLOCKS
        )
        .register();

    public static final BlockEntry<? extends Block> MULTIPHASE_MATTER_BLOCK = REGISTRUM
        .block("multiphase_matter_block", Block::new)
        .lang("Block of Multiphase Matter")
        .initialProperties(() -> Blocks.DIAMOND_BLOCK)
        .blockstate(DataGenUtil::onlyState)
        .item(MultiphaseMatterBlockItem::new)
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(Tags.Items.STORAGE_BLOCKS, ModItemTags.STORAGE_BLOCKS_MULTIPHASE_MATTER)
        .build()
        .tag(
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.STORAGE_BLOCKS_MULTIPHASE_MATTER,
            BlockTags.NEEDS_DIAMOND_TOOL,
            BlockTags.MINEABLE_WITH_PICKAXE
        )
        .recipe(RegistrumBlockRecipeLoader::multiphaseMatterBlock)
        .register();

    public static final BlockEntry<NegativeMatterBlock> NEGATIVE_MATTER_BLOCK = REGISTRUM.block(
            "negative_matter_block",
            NegativeMatterBlock::new
        )
        .lang("Block of Negative Matter")
        .initialProperties(() -> Blocks.NETHERITE_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_DIAMOND_TOOL, Tags.Blocks.STORAGE_BLOCKS)
        .properties(properties -> properties.lightLevel(state -> 7).noOcclusion().emissiveRendering(ModBlocks::always))
        .blockstate(DataGenUtil::onlyState)
        .item()
        .initialProperties(Item.Properties::new)
        .tag(Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::negativeMatterBlock)
        .defaultLoot()
        .register();

    public static final BlockEntry<LavaCauldronBlock> LAVA_CAULDRON = REGISTRUM.block("lava_cauldron", LavaCauldronBlock::new)
        .initialProperties(() -> Blocks.LAVA_CAULDRON)
        .properties(properties -> properties.lightLevel(blockState -> blockState.getValue(LavaCauldronBlock.LEVEL) * 4))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.CAULDRONS)
        .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
        .register();

    public static final BlockEntry<MeltGemCauldronBlock> MELT_GEM_CAULDRON = REGISTRUM.block("melt_gem_cauldron", MeltGemCauldronBlock::new)
        .initialProperties(() -> Blocks.CAULDRON)
        .properties(p -> p.lightLevel(s -> 15))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.CAULDRONS)
        .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
        .register();

    public static final BlockEntry<HoneyCauldronBlock> HONEY_CAULDRON = REGISTRUM.block("honey_cauldron", HoneyCauldronBlock::new)
        .initialProperties(() -> Blocks.CAULDRON)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.CAULDRONS)
        .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
        .register();

    public static final BlockEntry<ObsidianCauldronBlock> OBSIDIAN_CAULDRON = REGISTRUM
        .block("obsidian_cauldron", ObsidianCauldronBlock::new)
        .initialProperties(() -> Blocks.OBSIDIAN)
        .properties(it -> it.pushReaction(PushReaction.BLOCK))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.CAULDRONS)
        .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
        .register();

    public static final BlockEntry<ExpFluidCauldronBlock> EXP_FLUID_CAULDRON = REGISTRUM
        .block("exp_fluid_cauldron", ExpFluidCauldronBlock::new)
        .initialProperties(() -> Blocks.CAULDRON)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.CAULDRONS)
        .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
        .register();

    public static final BlockEntry<OilCauldronBlock> OIL_CAULDRON = REGISTRUM.block("oil_cauldron", OilCauldronBlock::new)
        .initialProperties(() -> Blocks.CAULDRON)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.CAULDRONS)
        .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
        .register();

    public static final Object2ObjectMap<Color, BlockEntry<CementCauldronBlock>> CEMENT_CAULDRONS = registerAllCementCauldrons();

    private static Object2ObjectMap<Color, BlockEntry<ReinforcedConcreteBlock>> registerReinforcedConcretes() {
        Object2ObjectMap<Color, BlockEntry<ReinforcedConcreteBlock>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var entry = registerReinforcedConcreteBlock(color);
            map.put(color, entry);
        }
        return map;
    }

    private static BlockEntry<ReinforcedConcreteBlock> registerReinforcedConcreteBlock(Color color) {
        // noinspection Convert2Lambda
        return REGISTRUM.block(
            color + "_reinforced_concrete",
                p -> new ReinforcedConcreteBlock(p, color)
            ).initialProperties(() -> Blocks.TERRACOTTA)
            .properties(properties -> properties.destroyTime(2.0F).explosionResistance(15.0F))
            .item()
            .tag(ModItemTags.REINFORCED_CONCRETE, Tags.Items.DYED, ModItemTags.DYED_COLORS.get(color))
            .build()
            .blockstate(() -> new NonNullBiConsumer<>() {
                @Override
                public void accept(
                    DataGenContext<Block, ReinforcedConcreteBlock> ctx,
                    RegistrumBlockModelGenerator generator
                ) {
                    Identifier singleModel = generator.withParent(ModelTemplates.CUBE_ALL)
                        .texture(TextureSlot.ALL, generator.modLoc("block/%s_reinforced_concrete".formatted(color)), false)
                        .build(generator.modLoc("block/%s_reinforced_concrete".formatted(color)));
                    Identifier topModel = generator.withParent(ModelTemplates.CUBE_COLUMN)
                        .texture(TextureSlot.END, generator.modLoc("block/%s_reinforced_concrete".formatted(color)), false)
                        .texture(TextureSlot.SIDE, generator.modLoc("block/%s_reinforced_concrete_top".formatted(color)), false)
                        .build(generator.modLoc("block/%s_reinforced_concrete_top".formatted(color)));
                    Identifier bottomModel = generator.withParent(ModelTemplates.CUBE_COLUMN)
                        .texture(TextureSlot.END, generator.modLoc("block/%s_reinforced_concrete".formatted(color)), false)
                        .texture(TextureSlot.SIDE, generator.modLoc("block/%s_reinforced_concrete_bottom".formatted(color)), false)
                        .build(generator.modLoc("block/%s_reinforced_concrete_bottom".formatted(color)));
                    generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(ctx.get())
                                                          .with(
                                                              PropertyDispatchWrap.initial(ReinforcedConcreteBlock.HALF)
                                                                  .select(
                                                                      ReinforcedConcreteHalf.TOP,
                                                                      BlockModelGenerators.plainVariant(topModel)
                                                                  )
                                                                  .select(
                                                                      ReinforcedConcreteHalf.SINGLE,
                                                                      BlockModelGenerators.plainVariant(singleModel)
                                                                  )
                                                                  .select(
                                                                      ReinforcedConcreteHalf.BOTTOM,
                                                                      BlockModelGenerators.plainVariant(bottomModel)
                                                                  )
                                                                  .dispatch()
                                                          ));
                }
            })
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, Tags.Blocks.DYED, ModBlockTags.DYED_COLORS.get(color))
            .register();
    }

    private static Object2ObjectMap<Color, BlockEntry<SlabBlock>> registerReinforcedConcreteSlabs() {
        Object2ObjectMap<Color, BlockEntry<SlabBlock>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var entry = registerReinforcedConcreteSlabBlock(color, REINFORCED_CONCRETES.get(color));
            map.put(color, entry);
        }
        return map;
    }

    private static BlockEntry<SlabBlock> registerReinforcedConcreteSlabBlock(Color color, BlockEntry<ReinforcedConcreteBlock> parent) {
        return REGISTRUM.block(color + "_reinforced_concrete_slab", SlabBlock::new)
            .initialProperties(() -> Blocks.TERRACOTTA)
            .properties(properties -> properties.destroyTime(2.0F).explosionResistance(15.0F))
            .item()
            .tag(ModItemTags.REINFORCED_CONCRETE, ItemTags.SLABS, Tags.Items.DYED, ModItemTags.DYED_COLORS.get(color))
            .build()
            .blockstate(() -> DataGenUtil.slabBlock(AnvilCraft.of("block/%s_reinforced_concrete".formatted(color))))
            .loot((tables, block) -> tables.add(block, tables.createSlabItemTable(block)))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.SLABS, Tags.Blocks.DYED, ModBlockTags.DYED_COLORS.get(color))
            .recipe(RegistrumBlockRecipeLoader.reinforcedConcreteSlab(parent))
            .register();
    }

    private static Object2ObjectMap<Color, BlockEntry<StairBlock>> registerReinforcedConcreteStairs() {
        Object2ObjectMap<Color, BlockEntry<StairBlock>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var entry = registerReinforcedConcreteStairBlock(color, REINFORCED_CONCRETES.get(color));
            map.put(color, entry);
        }
        return map;
    }

    private static BlockEntry<StairBlock> registerReinforcedConcreteStairBlock(Color color, BlockEntry<ReinforcedConcreteBlock> parent) {
        return REGISTRUM.block(
                color + "_reinforced_concrete_stair",
                properties -> new StairBlock(parent.getDefaultState(), properties)
            )
            .initialProperties(() -> Blocks.TERRACOTTA)
            .properties(properties -> properties.destroyTime(2.0F).explosionResistance(15.0F))
            .item()
            .tag(ModItemTags.REINFORCED_CONCRETE, ItemTags.STAIRS, Tags.Items.DYED, ModItemTags.DYED_COLORS.get(color))
            .build()
            .blockstate(() -> DataGenUtil.stairsBlock(AnvilCraft.of("block/%s_reinforced_concrete".formatted(color))))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.STAIRS, Tags.Blocks.DYED, ModBlockTags.DYED_COLORS.get(color))
            .recipe(RegistrumBlockRecipeLoader.reinforcedConcreteStair(parent))
            .register();
    }

    private static Object2ObjectMap<Color, BlockEntry<WallBlock>> registerReinforcedConcreteWalls() {
        Object2ObjectMap<Color, BlockEntry<WallBlock>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var entry = registerReinforcedConcreteWallBlock(color, REINFORCED_CONCRETES.get(color));
            map.put(color, entry);
        }
        return map;
    }

    private static BlockEntry<WallBlock> registerReinforcedConcreteWallBlock(Color color, BlockEntry<ReinforcedConcreteBlock> parent) {
        return REGISTRUM.block(color + "_reinforced_concrete_wall", WallBlock::new)
            .initialProperties(() -> Blocks.TERRACOTTA)
            .properties(properties -> properties.destroyTime(2.0F).explosionResistance(15.0F))
            .blockstate(() -> DataGenUtil.wallBlock(AnvilCraft.of("block/%s_reinforced_concrete_wall".formatted(color))))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.WALLS, Tags.Blocks.DYED, ModBlockTags.DYED_COLORS.get(color))
            .recipe(RegistrumBlockRecipeLoader.reinforcedConcreteWall(parent))
            .item()
            .tag(ModItemTags.REINFORCED_CONCRETE, ItemTags.WALLS, Tags.Items.DYED, ModItemTags.DYED_COLORS.get(color))
            .build()
            .register();
    }

    private static Object2ObjectMap<Color, BlockEntry<CementCauldronBlock>> registerAllCementCauldrons() {
        Object2ObjectMap<Color, BlockEntry<CementCauldronBlock>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var entry = registerCementCauldron(color);
            map.put(color, entry);
        }
        return map;
    }

    @SuppressWarnings("Convert2Lambda")
    private static BlockEntry<CementCauldronBlock> registerCementCauldron(Color color) {
        return REGISTRUM.block(color + "_cement_cauldron", p -> new CementCauldronBlock(p, color))
            .initialProperties(() -> Blocks.CAULDRON)
            .blockstate(() -> new NonNullBiConsumer<>() {
                @Override
                public void accept(
                    DataGenContext<Block, CementCauldronBlock> ctx,
                    RegistrumBlockModelGenerator generator
                ) {
                    TextureMapping mapping = new TextureMapping()
                        .put(TextureSlot.BOTTOM, new Material(generator.mcLoc("block/cauldron_bottom")))
                        .put(TextureSlot.INSIDE, new Material(generator.mcLoc("block/cauldron_inner")))
                        .put(TextureSlot.SIDE, new Material(generator.mcLoc("block/cauldron_side")))
                        .put(TextureSlot.TOP, new Material(generator.mcLoc("block/cauldron_top")))
                        .put(TextureSlot.PARTICLE, new Material(generator.mcLoc("block/cauldron_side")))
                        .put(TextureSlot.CONTENT, new Material(generator.modLoc("block/%s_cement".formatted(color))));
                    generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(
                        ctx.get(),
                        BlockModelGenerators.plainVariant(generator.withParent(ModelTemplates.CAULDRON_FULL, mapping).build(ctx.get()))
                    ));
                }
            })
            .loot((tables, block) -> tables.dropOther(block, Items.CAULDRON))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.CAULDRONS)
            .onRegister(block -> Item.BY_BLOCK.put(block, Items.CAULDRON))
            .register();
    }

    private static BlockEntry<? extends TimeCountedPressurePlateBlock> registerOtherCopperPressurePlate(
        String prefix,
        Block block,
        int tickCount
    ) {
        Identifier location = BuiltInRegistries.BLOCK.getKey(block);
        String id = prefix + "copper_pressure_plate";
        return REGISTRUM.block(id, properties -> new TimeCountedPressurePlateBlock(BlockSetType.IRON, properties, tickCount))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.PRESSURE_PLATES)
            .initialProperties(() -> block)
            .properties(properties -> properties.forceSolidOn()
                .instrument(NoteBlockInstrument.BASS)
                .noCollision()
                .strength(0.5F)
                .pushReaction(PushReaction.DESTROY))
            .blockstate(() -> DataGenUtil.pressurePlateBlock(location.withPrefix("block/")))
            .item()
            .tag(ModItemTags.PLATES, ModItemTags.COPPER_PLATES)
            .build()
            .register();
    }

    @SuppressWarnings("SameParameterValue")
    private static BlockEntry<? extends PowerLevelPressurePlateBlock> registerPressurePlate(
        String type,
        Supplier<? extends Block> block,
        NonNullFunction<BlockBehaviour.Properties, ? extends PowerLevelPressurePlateBlock> plateBlockFactory,
        Item... ingredients
    ) {
        Identifier location;
        if (block instanceof BlockEntry<? extends Block> entry) {
            location = entry.getId().withPrefix("block/");
        } else {
            location = BuiltInRegistries.BLOCK.getKey(block.get()).withPrefix("block/");
        }
        String id = type + "_pressure_plate";
        return REGISTRUM.block(id, plateBlockFactory)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.PRESSURE_PLATES)
            .initialProperties(block::get)
            .properties(properties -> properties.forceSolidOn()
                .instrument(NoteBlockInstrument.BASS)
                .noCollision()
                .strength(0.5F)
                .pushReaction(PushReaction.DESTROY))
            .blockstate(() -> DataGenUtil.leveledPressurePlateBlock(location))
            .item()
            .tag(ModItemTags.PLATES, ModItemTags.bindC("plates/" + type))
            .build()
            .recipe(RegistrumBlockRecipeLoader.pressurePlateItems(id, ingredients))
            .register();
    }

    @SafeVarargs
    private static BlockEntry<? extends PowerLevelPressurePlateBlock> registerPressurePlate(
        String type,
        Supplier<? extends Block> block,
        NonNullFunction<BlockBehaviour.Properties, ? extends PowerLevelPressurePlateBlock> plateBlockFactory,
        TagKey<Item>... ingredients
    ) {
        Identifier location;
        if (block instanceof BlockEntry<? extends Block> entry) {
            location = entry.getId().withPrefix("block/");
        } else {
            location = BuiltInRegistries.BLOCK.getKey(block.get()).withPrefix("block/");
        }
        String id = type + "_pressure_plate";
        return REGISTRUM.block(id, plateBlockFactory)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.PRESSURE_PLATES)
            .initialProperties(block::get)
            .properties(properties -> properties.forceSolidOn()
                .instrument(NoteBlockInstrument.BASS)
                .noCollision()
                .strength(0.5F)
                .pushReaction(PushReaction.DESTROY))
            .blockstate(() -> DataGenUtil.leveledPressurePlateBlock(location))
            .item()
            .tag(ModItemTags.PLATES, ModItemTags.bindC("plates/" + type), ModItemTags.PLATES)
            .initialProperties(() -> type.equals("tungsten") ? new Item.Properties().fireResistant() : new Item.Properties())
            .build()
            .recipe(RegistrumBlockRecipeLoader.pressurePlateTags(id, ingredients))
            .register();
    }

    public static final BlockEntry<NestingShulkerBoxBlock> NESTING_SHULKER_BOX = REGISTRUM.block(
            "nesting_shulker_box",
            NestingShulkerBoxBlock::new
        )
        .initialProperties(() -> Blocks.SHULKER_BOX)
        .loot(DataGenUtil::nestingShulkerBoxLoot)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .item(UncontainableBlockItem::new)
        .properties(properties -> properties
            .stacksTo(1)
            .component(ModComponents.OVER_LIMIT_CONTAINER, OverLimitItemContainerContents.EMPTY)
        )
        .model(DataGenUtil::blockItem)
        .build()
        .register();

    public static final BlockEntry<OverNestingShulkerBoxBlock> OVER_NESTING_SHULKER_BOX = REGISTRUM.block(
            "over_nesting_shulker_box",
            OverNestingShulkerBoxBlock::new
        )
        .initialProperties(() -> Blocks.SHULKER_BOX)
        .loot(DataGenUtil::nestingShulkerBoxLoot)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .item(UncontainableBlockItem::new)
        .properties(properties -> properties
            .stacksTo(1)
            .component(ModComponents.OVER_LIMIT_CONTAINER, OverLimitItemContainerContents.EMPTY)
        )
        .model(DataGenUtil::blockItem)
        .build()
        .register();

    public static final BlockEntry<SupercriticalNestingShulkerBoxBlock> SUPERCRITICAL_NESTING_SHULKER_BOX = REGISTRUM.block(
            "supercritical_nesting_shulker_box",
            SupercriticalNestingShulkerBoxBlock::new
        )
        .initialProperties(() -> Blocks.SHULKER_BOX)
        .loot(DataGenUtil::nestingShulkerBoxLoot)
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .item(UncontainableBlockItem::new)
        .properties(properties -> properties
            .stacksTo(1)
            .component(ModComponents.OVER_LIMIT_CONTAINER, OverLimitItemContainerContents.EMPTY)
        )
        .model(DataGenUtil::blockItem)
        .build()
        .register();

    public static final BlockEntry<ExpFluidBlock> EXP_FLUID = REGISTRUM.block(
            "exp_fluid",
            p -> new ExpFluidBlock(ModFluids.EXP_FLUID.get(), p)
        )
        .blockstate(ModelProviderUtil::liquid)
        .properties(it -> it.mapColor(MapColor.COLOR_GREEN)
            .replaceable()
            .noCollision()
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid()
            .sound(SoundType.EMPTY)
            .strength(100.0F))
        .register();

    public static final BlockEntry<LiquidBlock> OIL = REGISTRUM.block(
            "oil", p -> new LiquidBlock(ModFluids.OIL.get(), p))
        .properties(it -> it.mapColor(MapColor.TERRACOTTA_BLACK)
            .replaceable()
            .noCollision()
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid()
            .sound(SoundType.EMPTY)
            .strength(100.0F))
        .blockstate(ModelProviderUtil::liquid)
        .register();

    public static final Object2ObjectMap<Color, BlockEntry<LiquidBlock>> CEMENTS = registerAllCementLiquidBlock();

    private static Object2ObjectMap<Color, BlockEntry<LiquidBlock>> registerAllCementLiquidBlock() {
        Object2ObjectMap<Color, BlockEntry<LiquidBlock>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var entry = registerCementLiquidBlock(color);
            map.put(color, entry);
        }
        return map;
    }

    private static BlockEntry<LiquidBlock> registerCementLiquidBlock(Color color) {
        return REGISTRUM.block("%s_cement".formatted(color), p -> new LiquidBlock(ModFluids.SOURCE_CEMENTS.get(color).get(), p))
            .properties(it -> it.mapColor(DyeColor.byName(color.getSerializedName(), DyeColor.GRAY))
                .replaceable()
                .noCollision()
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY)
                .strength(100.0F))
            .blockstate(ModelProviderUtil::liquid)
            .register();
    }

    public static BlockEntry<LiquidBlock> MELT_GEM = REGISTRUM
        .block("melt_gem", p -> new LiquidBlock(ModFluids.MELT_GEM.get(), p))
        .properties(it -> it.mapColor(MapColor.EMERALD)
            .lightLevel(s -> 15)
            .replaceable()
            .noCollision()
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid()
            .sound(SoundType.EMPTY)
            .strength(100.0F))
        .blockstate(ModelProviderUtil::liquid)
        .register();

    public static BlockEntry<SimpleConfinementAnvilonBlock> CONFINED_TIME_ANVILON = REGISTRUM.block(
            "confined_time_anvilon",
            SimpleConfinementAnvilonBlock::new
        )
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.DRAGON_IMMUNE, BlockTags.WITHER_IMMUNE)
        .properties(PropertiesProviderUtil::confinedAnvilon)
        .blockstate(DataGenUtil::onlyState)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.EXPLOSION_PROOF)
        .build()
        .register();

    public static BlockEntry<SimpleConfinementAnvilonBlock> CONFINED_SPACE_ANVILON = REGISTRUM.block(
            "confined_space_anvilon",
            SimpleConfinementAnvilonBlock::new
        )
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.DRAGON_IMMUNE, BlockTags.WITHER_IMMUNE)
        .properties(PropertiesProviderUtil::confinedAnvilon)
        .blockstate(DataGenUtil::onlyState)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.EXPLOSION_PROOF)
        .build()
        .register();

    public static BlockEntry<SimpleConfinementAnvilonBlock> CONFINED_MASS_ANVILON = REGISTRUM.block(
            "confined_mass_anvilon",
            SimpleConfinementAnvilonBlock::new
        )
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.DRAGON_IMMUNE, BlockTags.WITHER_IMMUNE)
        .properties(PropertiesProviderUtil::confinedAnvilon)
        .blockstate(DataGenUtil::onlyState)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.EXPLOSION_PROOF)
        .build()
        .register();

    public static BlockEntry<SimpleConfinementAnvilonBlock> CONFINED_ENERGY_ANVILON = REGISTRUM.block(
            "confined_energy_anvilon",
            SimpleConfinementAnvilonBlock::new
        )
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.DRAGON_IMMUNE, BlockTags.WITHER_IMMUNE)
        .properties(PropertiesProviderUtil::confinedAnvilon)
        .blockstate(DataGenUtil::onlyState)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.EXPLOSION_PROOF)
        .build()
        .register();

    public static final BlockEntry<SimpleConfinementAnvilonBlock> CONFINED_NEUTRONIUM_INGOT_BLOCK = REGISTRUM.block(
            "confined_neutronium_ingot",
            SimpleConfinementAnvilonBlock::new
        )
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .properties(PropertiesProviderUtil::confinedAnvilon)
        .blockstate(DataGenUtil::onlyState)
        .item(SuperHeavyBlockItem::new)
        .initialProperties(() -> new Item.Properties().fireResistant().stacksTo(16))
        .tag(ModItemTags.EXPLOSION_PROOF)
        .build()
        .recipe(RegistrumBlockRecipeLoader::confinedNeutroniumIngotBlock)
        .register();

    public static BlockEntry<ConfinementChamberBlock> CONFINEMENT_CHAMBER = REGISTRUM.block(
            "confinement_chamber",
            ConfinementChamberBlock::new
        )
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.DRAGON_IMMUNE, BlockTags.WITHER_IMMUNE)
        .properties(PropertiesProviderUtil::confinedAnvilon)
        .blockstate(DataGenUtil::onlyState)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant())
        .tag(ModItemTags.EXPLOSION_PROOF)
        .build()
        .recipe(RegistrumBlockRecipeLoader::confinementChamber)
        .register();

    public static final BlockEntry<Block> SINGULARITY_CRYSTAL = REGISTRUM.block("singularity_crystal", Block::new)
        .initialProperties(() -> ModBlocks.CONFINEMENT_CHAMBER.get())
        .blockstate(DataGenUtil::onlyState)
        .properties(properties -> properties.pushReaction(PushReaction.BLOCK)
            .lightLevel(state -> 15)
            .noOcclusion()
            .strength(50F, 1200.0F)
            .requiresCorrectToolForDrops())
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.NEEDS_TRANSCENDIUM_TOOL)
        .item()
        .initialProperties(() -> new Item.Properties().fireResistant().stacksTo(1))
        .tag(ModItemTags.EXPLOSION_PROOF)
        .build()
        .recipe(RegistrumBlockRecipeLoader::singularityCrystal)
        .register();

    @SuppressWarnings("Convert2Lambda")
    public static final BlockEntry<SugarBlock> SUGAR_BLOCK = REGISTRUM.block("sugar_block", SugarBlock::new)
        .initialProperties(() -> Blocks.LAPIS_BLOCK)
        .loot(SugarBlock::loot)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.STORAGE_BLOCKS_SUGAR, Tags.Blocks.STORAGE_BLOCKS)
        .blockstate(() -> new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Block, SugarBlock> ctx, RegistrumBlockModelGenerator generator) {
                Identifier block = ModelLocationUtils.getModelLocation(ctx.get());
                Identifier stage0 = ModelTemplates.CUBE_ALL.create(
                    block,
                    new TextureMapping().put(TextureSlot.ALL, new Material(generator.modLoc("block/sugar_block"), false)),
                    generator.modelOutput
                );
                Identifier stage1 = ModelTemplates.CUBE_ALL.create(
                    block.withSuffix("_1"),
                    new TextureMapping().put(TextureSlot.ALL, new Material(generator.modLoc("block/sugar_block_1"), false)),
                    generator.modelOutput
                );
                Identifier stage2 = ModelTemplates.CUBE_ALL.create(
                    block.withSuffix("_2"),
                    new TextureMapping().put(TextureSlot.ALL, new Material(generator.modLoc("block/sugar_block_2"), false)),
                    generator.modelOutput
                );
                Identifier stage3 = ModelTemplates.CUBE_ALL.create(
                    block.withSuffix("_3"),
                    new TextureMapping().put(TextureSlot.ALL, new Material(generator.modLoc("block/sugar_block_3"), false)),
                    generator.modelOutput
                );
                generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(ctx.get()).with(
                    PropertyDispatchWrap.initial(SugarBlock.FRAGMENTATION_DEGREE)
                        .select(FragmentationDegree.ZERO, BlockModelGenerators.plainVariant(stage0))
                        .select(FragmentationDegree.ONE, BlockModelGenerators.plainVariant(stage1))
                        .select(FragmentationDegree.TWO, BlockModelGenerators.plainVariant(stage2))
                        .select(FragmentationDegree.THREE, BlockModelGenerators.plainVariant(stage3))
                        .dispatch()
                ));
            }
        })
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_SUGAR, Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::sugarBlock)
        .register();

    public static final BlockEntry<GunpowderBlock> GUNPOWER_BLOCK = REGISTRUM.block("gunpowder_block", GunpowderBlock::new)
        .initialProperties(() -> Blocks.LAPIS_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, ModBlockTags.STORAGE_BLOCKS_GUNPOWDER, Tags.Blocks.STORAGE_BLOCKS)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_GUNPOWDER, Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::gunpowerBlock)
        .register();

    public static final BlockEntry<RottenFleshBlock> ROTTEN_FLESH_BLOCK = REGISTRUM.block("rotten_flesh_block", RottenFleshBlock::new)
        .initialProperties(() -> Blocks.NETHER_WART_BLOCK)
        .tag(BlockTags.MINEABLE_WITH_HOE, ModBlockTags.STORAGE_BLOCKS_ROTTEN_FLESH, Tags.Blocks.STORAGE_BLOCKS)
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_ROTTEN_FLESH, Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::rottenFleshBlock)
        .register();

    public static final BlockEntry<FlintBlock> FLINT_BLOCK = REGISTRUM.block("flint_block", FlintBlock::new)
        .initialProperties(() -> Blocks.QUARTZ_BLOCK)
        .tag(
            BlockTags.MINEABLE_WITH_PICKAXE,
            ModBlockTags.STORAGE_BLOCKS_FLINT,
            Tags.Blocks.STORAGE_BLOCKS,
            ModBlockTags.VOID_DECAY_PRODUCTS
        )
        .item()
        .tag(ModItemTags.STORAGE_BLOCKS_FLINT, Tags.Items.STORAGE_BLOCKS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::flintBlock)
        .register();

    public static final BlockEntry<Block> POLISHED_FLINT_BLOCK = REGISTRUM.block("polished_flint_block", Block::new)
        .initialProperties(FLINT_BLOCK::get)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .simpleItem()
        .recipe(RegistrumBlockRecipeLoader::polishedFlintBlock)
        .register();

    public static final BlockEntry<Block> CUT_FLINT_BLOCK = REGISTRUM.block("cut_flint_block", Block::new)
        .initialProperties(FLINT_BLOCK::get)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .simpleItem()
        .recipe(RegistrumBlockRecipeLoader::cutFlintBlock)
        .register();

    public static final BlockEntry<SlabBlock> CUT_FLINT_SLAB_BLOCK = REGISTRUM.block("cut_flint_slab", SlabBlock::new)
        .initialProperties(FLINT_BLOCK::get)
        .blockstate(() -> DataGenUtil.slabBlock(
            _ -> new Material(AnvilCraft.of("block/cut_flint_block")),
            _ -> new Material(AnvilCraft.of("block/cut_flint_block")),
            _ -> new Material(AnvilCraft.of("block/cut_flint_block")),
            _ -> AnvilCraft.of("block/cut_flint_block")
        ))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.SLABS)
        .item()
        .tag(ItemTags.SLABS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::cutFlintSlabBlock)
        .register();

    public static final BlockEntry<StairBlock> CUT_FLINT_STAIRS_BLOCK = REGISTRUM.block(
            "cut_flint_stairs",
            properties -> new StairBlock(FLINT_BLOCK.getDefaultState(), properties)
        )
        .initialProperties(FLINT_BLOCK::get)
        .blockstate(() -> DataGenUtil.stairsBlock(AnvilCraft.of("block/cut_flint_block")))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.STAIRS)
        .item()
        .tag(ItemTags.STAIRS)
        .build()
        .recipe(RegistrumBlockRecipeLoader::cutFlintStairsBlock)
        .register();

    public static final BlockEntry<RotatedPillarBlock> CUT_FLINT_PILLAR_BLOCK = REGISTRUM.block(
            "cut_flint_pillar",
            RotatedPillarBlock::new
        )
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .simpleItem()
        .initialProperties(FLINT_BLOCK::get)
        .blockstate(() -> DataGenUtil.columnBlock(AnvilCraft.of("block/cut_flint_pillar"), AnvilCraft.of("block/cut_flint_pillar_top")))
        .recipe(RegistrumBlockRecipeLoader::cutFlintPillarBlock)
        .register();

    static {
        REGISTRUM.defaultCreativeTab(ModItemGroups.ANVILCRAFT_FUNCTION_BLOCK.getKey());
    }

    public static final BlockEntry<PulseGeneratorBlock> PULSE_GENERATOR = REGISTRUM.block("pulse_generator", PulseGeneratorBlock::new)
        .properties(properties -> properties.strength(3.0F, 3.5F).sound(SoundType.STONE).noOcclusion())
        .blockstate(() -> DataGenUtil.horizontalFacingBlockInverted(
            PulseGeneratorBlock.POWERED,
            ctx -> ctx.getId().withPrefix("block/").withSuffix("_on"),
            ctx -> ctx.getId().withPrefix("block/")
        ))
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .simpleItem()
        .recipe(RegistrumBlockRecipeLoader::pulseGenerator)
        .register();

    public static final BlockEntry<AdvancedComparatorBlock> ADVANCED_COMPARATOR = REGISTRUM.block(
            "advanced_comparator",
            AdvancedComparatorBlock::new
        )
        .properties(properties -> properties.strength(3.0F, 3.5F).sound(SoundType.STONE).noOcclusion())
        .blockstate(DataGenUtil::noExtraModelOrState)
        .tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .simpleItem()
        .recipe(RegistrumBlockRecipeLoader::advancedComparator)
        .register();

    public static final BlockEntry<? extends TimeCountedPressurePlateBlock> COPPER_PRESSURE_PLATE = REGISTRUM.block(
            "copper_pressure_plate",
            properties -> new TimeCountedPressurePlateBlock(BlockSetType.IRON, properties, 10)
        )
        .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.PRESSURE_PLATES)
        .initialProperties(() -> Blocks.COPPER_BLOCK)
        .properties(properties -> properties.forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(0.5F)
            .pushReaction(PushReaction.DESTROY))
        .blockstate(() -> DataGenUtil.pressurePlateBlock(Identifier.withDefaultNamespace("block/copper_block")))
        .item()
        .tag(ModItemTags.PLATES, ModItemTags.COPPER_PLATES)
        .build()
        .recipe(RegistrumBlockRecipeLoader::copperPressurePlate)
        .register();

    public static final BlockEntry<? extends TimeCountedPressurePlateBlock> EXPOSED_COPPER_PRESSURE_PLATE =
        registerOtherCopperPressurePlate("exposed_", Blocks.EXPOSED_COPPER, 20);
    public static final BlockEntry<? extends TimeCountedPressurePlateBlock> WEATHERED_COPPER_PRESSURE_PLATE =
        registerOtherCopperPressurePlate("weathered_", Blocks.WEATHERED_COPPER, 40);
    public static final BlockEntry<? extends TimeCountedPressurePlateBlock> OXIDIZED_COPPER_PRESSURE_PLATE =
        registerOtherCopperPressurePlate("oxidized_", Blocks.OXIDIZED_COPPER, 80);
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> TUNGSTEN_PRESSURE_PLATE = registerPressurePlate(
        "tungsten",
        TUNGSTEN_BLOCK,
        FireImmunePressurePlateBlock::new,
        ModItemTags.TUNGSTEN_INGOTS
    );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> TITANIUM_PRESSURE_PLATE = registerPressurePlate(
        "titanium",
        TITANIUM_BLOCK,
        properties -> new ItemDurabilityPressurePlateBlock(properties, false),
        ModItemTags.TITANIUM_INGOTS
    );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> ZINC_PRESSURE_PLATE = registerPressurePlate(
        "zinc",
        ZINC_BLOCK,
        properties -> new HealthPercentPressurePlateBlock(properties, false),
        ModItemTags.ZINC_INGOTS
    );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> TIN_PRESSURE_PLATE = registerPressurePlate(
        "tin",
        TIN_BLOCK,
        properties -> new HealthPercentPressurePlateBlock(properties, true),
        ModItemTags.TIN_INGOTS
    );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> LEAD_PRESSURE_PLATE = registerPressurePlate(
        "lead",
        LEAD_BLOCK,
        EntityTypePressurePlateBlock::new,
        ModItemTags.LEAD_INGOTS
    );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> SILVER_PRESSURE_PLATE = registerPressurePlate(
        "silver", SILVER_BLOCK, properties -> new EntityCountPressurePlateBlock(
            properties,
            entity -> entity.typeHolder().is(TagKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace("undead")))
        ), ModItemTags.SILVER_INGOTS
    );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> URANIUM_PRESSURE_PLATE = registerPressurePlate(
        "uranium",
        URANIUM_BLOCK,
        properties -> new ItemDurabilityPressurePlateBlock(properties, true),
        ModItemTags.URANIUM_INGOTS
    );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> PLUTONIUM_PRESSURE_PLATE = registerPressurePlate(
        "plutonium",
        PLUTONIUM_BLOCK,
        PlayerInHandItemDurabilityPressurePlateBlock::new,
        ModItemTags.PLUTONIUM_INGOTS
    );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> BRASS_PRESSURE_PLATE = registerPressurePlate(
        "brass",
        BRASS_BLOCK,
        PlayerInventoryPressurePlateBlock::new,
        ModItemTags.BRASS_INGOTS
    );
    public static final BlockEntry<? extends PowerLevelPressurePlateBlock> BRONZE_PRESSURE_PLATE = registerPressurePlate(
        "bronze",
        BRONZE_BLOCK,
        PlayerHungerPressurePlateBlock::new,
        ModItemTags.BRONZE_INGOTS
    );

    public static final BlockEntry<BlackHoleBlock> BLACK_HOLE = REGISTRUM.block("black_hole", BlackHoleBlock::new)
        .initialProperties(() -> Blocks.OBSIDIAN)
        .properties(p -> p.strength(10000.0F, 10000.0F).lightLevel(state -> 15).emissiveRendering(ModBlocks::always))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .register();

    public static final BlockEntry<WhiteHoleBlock> WHITE_HOLE = REGISTRUM.block("white_hole", WhiteHoleBlock::new)
        .initialProperties(() -> Blocks.OBSIDIAN)
        .properties(p -> p.strength(10000.0F, 10000.0F).lightLevel(state -> 15).emissiveRendering(ModBlocks::always))
        .blockstate(DataGenUtil::noExtraModelOrState)
        .simpleItem()
        .register();

    public static void register() {
    }

    public static boolean never(BlockState state, BlockGetter blockGetter, BlockPos pos, EntityType<?> entity) {
        return false;
    }

    public static boolean never(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        return false;
    }

    public static boolean always(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        return true;
    }
}
