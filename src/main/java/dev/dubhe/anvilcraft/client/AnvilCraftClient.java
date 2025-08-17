package dev.dubhe.anvilcraft.client;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.event.GuiLayerRegistrationEventListener;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.client.init.ModModelLayers;
import dev.dubhe.anvilcraft.client.init.ModShaders;
import dev.dubhe.anvilcraft.client.init.ModTooltipComponents;
import dev.dubhe.anvilcraft.client.particle.PlasmaJetsParticle;
import dev.dubhe.anvilcraft.client.renderer.item.decoration.IonoCraftBackpackDecoration;
import dev.dubhe.anvilcraft.client.support.InspectionSupport;
import dev.dubhe.anvilcraft.config.AnvilCraftConfig;
import dev.dubhe.anvilcraft.init.ModFluids;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModParticles;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import javax.annotation.ParametersAreNonnullByDefault;

@Mod(value = AnvilCraft.MOD_ID, dist = Dist.CLIENT)
public class AnvilCraftClient {

    public AnvilCraftClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(GuiLayerRegistrationEventListener::onRegister);
        container.registerExtensionPoint(
            IConfigScreenFactory.class,
            (c, s) -> AutoConfig.getConfigScreen(AnvilCraftConfig.class, s).get()
        );
        modBus.addListener(ModKeyMappings::register);
        modBus.addListener(AnvilCraftClient::registerClientExtensions);
        modBus.addListener(AnvilCraftClient::registerCustomItemDecorations);
        modBus.addListener(AnvilCraftClient::registerParticleProviders);
        modBus.addListener(ModShaders::register);
        modBus.addListener(ModModelLayers::register);
        modBus.addListener(ModModelLayers::createModel);
        modBus.addListener(ModTooltipComponents::register);
        modBus.addListener(AnvilCraftClient::clientSetup);
        InspectionSupport.initializeClient();
    }

    public static void clientSetup(FMLClientSetupEvent event) {
        AnvilCraft.getIntegrationManager().loadAllClientIntegrations();
    }

    public static void registerClientExtensions(RegisterClientExtensionsEvent e) {
        ModFluids.onRegisterFluidType(e);
        e.registerItem(new ItemExtensionImpl(), ModItems.IONOCRAFT_BACKPACK);
    }

    public static void registerCustomItemDecorations(RegisterItemDecorationsEvent e) {
        e.register(ModItems.IONOCRAFT_BACKPACK, new IonoCraftBackpackDecoration());
    }

    public static void registerParticleProviders(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(ModParticles.PLASMA_JETS.get(), PlasmaJetsParticle.Provider::new);
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
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
}
