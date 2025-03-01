package dev.dubhe.anvilcraft.integration.curios;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.integration.Integration;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.integration.curios.renderer.GogglesCurioRenderer;
import dev.dubhe.anvilcraft.integration.curios.renderer.IonoCraftBackpackCurioRenderer;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.List;

@Integration("curios")
public class CuriosIntegration {
    public void apply() {
        AnvilCraft.MOD_BUS.addListener(this::setup);
        AnvilCraft.MOD_BUS.addListener(this::onLayerRegister);
        AnvilCraft.MOD_BUS.addListener(this::registerCapabilities);
    }

    private void setup(FMLCommonSetupEvent event) {
        AnvilHammerItem.addIsWearingPredicate(player ->
            CuriosApi.getCuriosInventory(player).map(this::isAnvilHammerWearing).orElse(false)
        );
        IonoCraftBackpackItem.addStackProvider(player ->
            CuriosApi.getCuriosInventory(player).map(this::getIonoCraftBackpackWearing).orElse(ItemStack.EMPTY)
        );
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
            CuriosCapability.ITEM,
            (stack, context) -> () -> stack,
            ModItems.ANVIL_HAMMER,
            ModItems.ROYAL_ANVIL_HAMMER,
            ModItems.EMBER_ANVIL_HAMMER,
            ModItems.IONOCRAFT_BACKPACK
        );
    }

    private boolean isAnvilHammerWearing(ICuriosItemHandler itemHandler) {
        return !itemHandler.findCurios(it -> it.getItem() instanceof AnvilHammerItem).isEmpty();
    }

    private ItemStack getIonoCraftBackpackWearing(ICuriosItemHandler itemHandler) {
        List<SlotResult> curios = itemHandler.findCurios(it -> it.getItem() instanceof IonoCraftBackpackItem);
        if (!curios.isEmpty()) {
            return curios.getFirst().stack();
        }
        return ItemStack.EMPTY;
    }

    public void applyClient() {
        CuriosRendererRegistry.register(
            ModItems.ANVIL_HAMMER.get(),
            () -> new GogglesCurioRenderer(Minecraft.getInstance().getEntityModels().bakeLayer(GogglesCurioRenderer.LAYER))
        );
        CuriosRendererRegistry.register(
            ModItems.ROYAL_ANVIL_HAMMER.get(),
            () -> new GogglesCurioRenderer(Minecraft.getInstance().getEntityModels().bakeLayer(GogglesCurioRenderer.LAYER))
        );
        CuriosRendererRegistry.register(
            ModItems.EMBER_ANVIL_HAMMER.get(),
            () -> new GogglesCurioRenderer(Minecraft.getInstance().getEntityModels().bakeLayer(GogglesCurioRenderer.LAYER))
        );
        CuriosRendererRegistry.register(
            ModItems.IONOCRAFT_BACKPACK.get(),
            IonoCraftBackpackCurioRenderer::new
        );
    }

    public void onLayerRegister(final EntityRenderersEvent.@NotNull RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
            GogglesCurioRenderer.LAYER,
            () -> LayerDefinition.create(GogglesCurioRenderer.mesh(), 1, 1)
        );
    }
}
