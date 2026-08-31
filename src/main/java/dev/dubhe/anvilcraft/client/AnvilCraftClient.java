package dev.dubhe.anvilcraft.client;

import dev.anvilcraft.lib.v2.integration.IntegrationHook;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.event.GuiLayerRegistrationEventListener;
import dev.dubhe.anvilcraft.client.init.ModCreativeVariantGroups;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.client.init.ModModelLayers;
import dev.dubhe.anvilcraft.client.init.ModShaders;
import dev.dubhe.anvilcraft.client.init.ModTooltipComponents;
import dev.dubhe.anvilcraft.client.particle.IonocraftBackpackExhaustParticle;
import dev.dubhe.anvilcraft.client.particle.OverseerTrailParticle;
import dev.dubhe.anvilcraft.client.particle.PlasmaJetsParticle;
import dev.dubhe.anvilcraft.client.renderer.OverworldLikeOrbitalSkyRenderer;
import dev.dubhe.anvilcraft.client.renderer.item.ItemSlotClipping;
import dev.dubhe.anvilcraft.client.renderer.item.decoration.IonocraftBackpackDecoration;
import dev.dubhe.anvilcraft.client.renderer.item.decoration.TerminalInsertionDecoration;
import dev.dubhe.anvilcraft.client.support.InspectionSupport;
import dev.dubhe.anvilcraft.client.support.PillSelectorSupport;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
import dev.dubhe.anvilcraft.init.ModParticles;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.curios.client.renderer.GogglesCurioRenderer;
import dev.dubhe.anvilcraft.item.weapon.AnvilRailgunItem;
import dev.dubhe.anvilcraft.item.weapon.LaserGunItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.FlyTowardsPositionParticle;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.Objects;
import javax.annotation.Nullable;

@Mod(value = AnvilCraft.MOD_ID, dist = Dist.CLIENT)
public class AnvilCraftClient {
    public static @Nullable IEventBus modEventBus = null;
    public static @Nullable ModContainer modContainer = null;
    public static final AnvilCraftClientConfig CONFIG = AnvilCraft.CLIENT_CONFIG;
    public static PillSelectorSupport pillSelectorSupport = PillSelectorSupport.INSTANCE;

    public AnvilCraftClient(IEventBus modBus, ModContainer container) {
        modEventBus = modBus;
        modContainer = container;
        modBus.addListener(GuiLayerRegistrationEventListener::onRegister);
        modBus.addListener(ModKeyMappings::register);
        modBus.addListener(AnvilCraftClient::registerClientExtensions);
        modBus.addListener(AnvilCraftClient::registerCustomItemDecorations);
        modBus.addListener(AnvilCraftClient::registerParticleProviders);
        modBus.addListener(ModShaders::register);
        modBus.addListener(ModModelLayers::register);
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> event.registerLayerDefinition(
            GogglesCurioRenderer.LAYER,
            () -> LayerDefinition.create(GogglesCurioRenderer.mesh(), 1, 1)
        ));
        modBus.addListener(ModModelLayers::createModel);
        modBus.addListener(ModTooltipComponents::register);
        modBus.addListener(OverworldLikeOrbitalSkyRenderer::cacheModels);
        modBus.addListener(AnvilCraftClient::clientSetup);
        AnvilCraftRecipeComponentFactories.RECIPE_COMPONENT_FACTORIES.register(modEventBus);
        InspectionSupport.initializeClient();
    }

    public static void clientSetup(FMLClientSetupEvent event) {
        IntegrationHook.setModEventBus(Objects.requireNonNull(modEventBus));
        IntegrationHook.setModContainer(Objects.requireNonNull(modContainer));
        AnvilCraft.getINTEGRATION_MANAGER().loadAllClientIntegrations();
        ModCreativeVariantGroups.register();
        ItemSlotClipping.register(ModItems.FROST_METAL_RESONATOR.get());
        ItemSlotClipping.register(ModItems.EMBER_METAL_RESONATOR.get());
        ItemSlotClipping.register(ModItems.TRANSCENDENCE_RESONATOR.get());
        ItemSlotClipping.register(ModItems.FROST_METAL_HEAVY_HALBERD.get());
        ItemSlotClipping.register(ModItems.EMBER_METAL_HEAVY_HALBERD.get());
        ItemSlotClipping.register(ModItems.TRANSCENDENCE_HEAVY_HALBERD.get());
    }

    public static void registerClientExtensions(RegisterClientExtensionsEvent e) {
        ModFluids.onRegisterFluidType(e);
        ItemExtensionImpl itemExtensionInstance = new ItemExtensionImpl();
        e.registerItem(itemExtensionInstance, ModItems.IONOCRAFT_BACKPACK);
        e.registerItem(
            new EnergyWeaponExtensionImpl(),
            ModItems.ANVIL_RAILGUN,
            ModItems.CORRUPTED_BEACON_ACTIVATOR,
            ModItems.TESLA_GUN,
            ModItems.LASER_GUN
        );
    }

    public static void registerCustomItemDecorations(RegisterItemDecorationsEvent e) {
        e.register(ModItems.IONOCRAFT_BACKPACK, new IonocraftBackpackDecoration());
        e.register(ModItems.HYPERDIMENSION_TERMINAL, new TerminalInsertionDecoration());
        e.register(ModItems.LOCAL_TERMINAL, new TerminalInsertionDecoration());
        e.register(ModItems.SHULKER_TERMINAL, new TerminalInsertionDecoration());
    }

    public static void registerParticleProviders(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(ModParticles.PLASMA_JETS.get(), PlasmaJetsParticle.Provider::new);
        e.registerSpriteSet(ModParticles.ANVILON_ENERGY.get(), FlyTowardsPositionParticle.EnchantProvider::new);
        e.registerSpriteSet(ModParticles.ANVILON_MASS.get(), FlyTowardsPositionParticle.EnchantProvider::new);
        e.registerSpriteSet(ModParticles.ANVILON_SPACE.get(), FlyTowardsPositionParticle.EnchantProvider::new);
        e.registerSpriteSet(ModParticles.ANVILON_TIME.get(), FlyTowardsPositionParticle.EnchantProvider::new);
        e.registerSpriteSet(ModParticles.IONOCRAFT_BACKPACK_EXHAUST.get(), IonocraftBackpackExhaustParticle.Provider::new);
        e.registerSpriteSet(ModParticles.OVERSEER_TRAIL.get(), OverseerTrailParticle.Provider::new);
    }

    public static class ItemExtensionImpl implements IClientItemExtensions {
        @Override
        public HumanoidModel<?> getHumanoidArmorModel(
            LivingEntity livingEntity,
            ItemStack itemStack,
            EquipmentSlot equipmentSlot,
            HumanoidModel<?> original
        ) {
            if (itemStack.is(ModItems.IONOCRAFT_BACKPACK)) {
                return ModModelLayers.getIonocraftBackpackModel();
            }
            return IClientItemExtensions.super.getHumanoidArmorModel(livingEntity, itemStack, equipmentSlot, original);
        }
    }

    public static class EnergyWeaponExtensionImpl implements IClientItemExtensions {
        @Nullable
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            if (!entity.isUsingItem() || entity.getUseItem().getItem() != stack.getItem()) {
                return IClientItemExtensions.super.getArmPose(entity, hand, stack);
            }
            if (stack.getItem() instanceof AnvilRailgunItem && entity instanceof Player player
                && AnvilRailgunItem.isLoading(player, stack, hand)) {
                return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            }
            if (stack.getItem() instanceof LaserGunItem) {
                return HumanoidModel.ArmPose.BOW_AND_ARROW;
            }
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
    }
}
