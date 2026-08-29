package dev.dubhe.anvilcraft.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.integration.IntegrationHook;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderDispatcher;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModModelLayers;
import dev.dubhe.anvilcraft.client.init.ModPostEffects;
import dev.dubhe.anvilcraft.client.particle.IonoCraftBackpackExhaustParticle;
import dev.dubhe.anvilcraft.client.particle.OverseerTrailParticle;
import dev.dubhe.anvilcraft.client.particle.PlasmaJetsParticle;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CFARenderer;
import dev.dubhe.anvilcraft.client.renderer.item.ItemUseAnimationTransform;
import dev.dubhe.anvilcraft.client.renderer.laser.CachedLaserBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.support.InspectionSupport;
import dev.dubhe.anvilcraft.client.support.PillSelectorSupport;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
import dev.dubhe.anvilcraft.init.ModParticles;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdItem;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdMode;
import dev.dubhe.anvilcraft.item.tool.trascendence.TranscendenceResonatorItem;
import dev.dubhe.anvilcraft.item.weapon.AnvilRailgunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.particle.FlyTowardsPositionParticle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Mod(value = AnvilCraft.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class AnvilCraftClient {
    public static IEventBus modEventBus = null;
    public static ModContainer modContainer = null;
    public static final AnvilCraftClientConfig CONFIG = AnvilCraft.CLIENT_CONFIG;
    public static PillSelectorSupport pillSelectorSupport = PillSelectorSupport.INSTANCE;

    public AnvilCraftClient(IEventBus modBus, ModContainer container) {
        AnvilCraftClient.modEventBus = modBus;
        AnvilCraftClient.modContainer = container;
        InspectionSupport.initializeClient();
        
        IntegrationHook.setModEventBus(modBus);
        IntegrationHook.setModContainer(container);
        AnvilCraft.getINTEGRATION_MANAGER().loadAllClientIntegrations();
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            CachedBlockEntityRenderDispatcher.INSTANCE.registerRenderer(
                ModBlockEntities.RUBY_LASER.get(),
                new CachedLaserBlockEntityRenderer<>()
            );
            CachedBlockEntityRenderDispatcher.INSTANCE.registerRenderer(
                ModBlockEntities.RUBY_PRISM.get(),
                new CachedLaserBlockEntityRenderer<>()
            );
            CachedBlockEntityRenderDispatcher.INSTANCE.registerRenderer(
                ModBlockEntities.CELESTIAL_FORGING_ANVIL_LASER_INTERFACE.get(),
                new CachedLaserBlockEntityRenderer<>()
            );
            CachedBlockEntityRenderDispatcher.INSTANCE.registerRenderer(
                ModBlockEntities.CELESTIAL_FORGING_ANVIL_PORTAL.get(),
                new CachedLaserBlockEntityRenderer<>()
            );
            CachedBlockEntityRenderDispatcher.INSTANCE.registerRenderer(
                ModBlockEntities.LARGE_LASER.get(),
                new CachedLaserBlockEntityRenderer<>()
            );
            CachedBlockEntityRenderDispatcher.INSTANCE.registerRenderer(
                ModBlockEntities.LENS.get(),
                new CachedLaserBlockEntityRenderer<>()
            );
        });
    }

    @SubscribeEvent
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
        e.registerItem(
            new HeavyHalberdExtensionImpl(),
            ModItems.FROST_METAL_HEAVY_HALBERD,
            ModItems.EMBER_METAL_HEAVY_HALBERD,
            ModItems.TRANSCENDENCE_HEAVY_HALBERD
        );
        e.registerItem(new TranscendenceResonatorExtensionImpl(), ModItems.TRANSCENDENCE_RESONATOR);
    }

    @SubscribeEvent
    public static void registerCustomItemDecorations(RegisterItemDecorationsEvent e) {
        // IonocraftBackpackDecoration has been removed - decoration was migrated to armor renderer
    }

    @SubscribeEvent
    public static void on(RenderLevelStageEvent.AfterLevel event) {
        if (!RenderState.isLensEffectEnabled()) {
            return;
        }
        RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        if (ModPostEffects.getGravitationalLensPostEffect() == null) {
            return;
        }
        ModPostEffects.getGravitationalLensPostEffect().process(
            mainTarget.getColorTextureView(),
            mainTarget,
            mainTarget.width,
            mainTarget.height,
            event.getLevelRenderState()
        );
    }

    @SubscribeEvent
    public static void renderDeferredBeams(RenderLevelStageEvent.AfterLevel event) {
        PoseStack pose = event.getPoseStack();
        RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        RenderTarget weatherTarget = event.getLevelRenderer().getWeatherTarget();
        if (weatherTarget != null) {
            mainTarget.copyDepthFrom(weatherTarget);
        }
        MultiBufferSource.BufferSource bufferSource = event.getLevelRenderer().renderBuffers.bufferSource();
        pose.pushPose();
        pose.last().pose().mul(event.getModelViewMatrix());
        CFARenderer.renderDeferredTractorBeams(
            pose,
            bufferSource,
            event.getLevelRenderState().cameraRenderState.pos
        );
        pose.popPose();
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(ModParticles.PLASMA_JETS.get(), PlasmaJetsParticle.Provider::new);
        e.registerSpriteSet(ModParticles.ANVILON_ENERGY.get(), FlyTowardsPositionParticle.EnchantProvider::new);
        e.registerSpriteSet(ModParticles.ANVILON_MASS.get(), FlyTowardsPositionParticle.EnchantProvider::new);
        e.registerSpriteSet(ModParticles.ANVILON_SPACE.get(), FlyTowardsPositionParticle.EnchantProvider::new);
        e.registerSpriteSet(ModParticles.ANVILON_TIME.get(), FlyTowardsPositionParticle.EnchantProvider::new);
        e.registerSpriteSet(
            ModParticles.IONOCRAFT_BACKPACK_EXHAUST.get(),
            IonoCraftBackpackExhaustParticle.Provider::new
        );
        e.registerSpriteSet(ModParticles.OVERSEER_TRAIL.get(), OverseerTrailParticle.Provider::new);
    }

    public static class ItemExtensionImpl implements IClientItemExtensions {
        @Override
        public Model<?> getHumanoidArmorModel(
            ItemStack itemStack, EquipmentClientInfo.LayerType layerType, Model original
        ) {
            if (itemStack.is(ModItems.IONOCRAFT_BACKPACK)) {
                return Objects.requireNonNull(ModModelLayers.getIonocraftBackpackModel());
            }
            return IClientItemExtensions.super.getHumanoidArmorModel(itemStack, layerType, original);
        }

        @Override
        public @Nullable Identifier getArmorTexture(
            ItemStack itemStack,
            EquipmentClientInfo.LayerType type,
            EquipmentClientInfo.Layer layer,
            Identifier defaultId
        ) {
            if (itemStack.is(ModItems.IONOCRAFT_BACKPACK)) {
                if (IonoCraftBackpackItem.getEnergyStored(itemStack) > 0) {
                    return IonoCraftBackpackItem.TEXTURE;
                }
                return IonoCraftBackpackItem.TEXTURE_OFF;
            }
            return IClientItemExtensions.super.getArmorTexture(itemStack, type, layer, defaultId);
        }
    }

    public static class EnergyWeaponExtensionImpl implements IClientItemExtensions {
        @Override
        public HumanoidModel.@Nullable ArmPose getArmPose(
            LivingEntity entity,
            InteractionHand hand,
            ItemStack stack
        ) {
            if (!entity.isUsingItem() || entity.getUseItem().getItem() != stack.getItem()) return null;
            if (stack.getItem() instanceof AnvilRailgunItem
                && entity instanceof Player player
                && AnvilRailgunItem.isLoading(player, stack, hand)
            ) {
                return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            }
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
    }

    public static class HeavyHalberdExtensionImpl implements IClientItemExtensions {
        @Override
        public boolean applyForgeHandTransform(
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack stack,
            float partialTick,
            float equipProgress,
            float swingProgress
        ) {
            if (!(stack.getItem() instanceof HeavyHalberdItem)
                || HeavyHalberdItem.getMode(stack) != HeavyHalberdMode.SWORD) {
                return false;
            }
            return ItemUseAnimationTransform.applySwordBlock(poseStack, player, arm, equipProgress);
        }
    }

    public static class TranscendenceResonatorExtensionImpl implements IClientItemExtensions {
        @Override
        public boolean applyForgeHandTransform(
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack stack,
            float partialTick,
            float equipProgress,
            float swingProgress
        ) {
            return ItemUseAnimationTransform.applyCrossbowCharge(
                poseStack,
                player,
                arm,
                stack,
                partialTick,
                equipProgress,
                TranscendenceResonatorItem.RESONANCE_MINING_TICKS
            );
        }
    }
}
