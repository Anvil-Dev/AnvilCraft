package dev.dubhe.anvilcraft.integration.curios;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import dev.anvilcraft.lib.integration.Integration;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.curios.renderer.GogglesCurioRenderer;
import dev.dubhe.anvilcraft.integration.curios.renderer.IonoCraftBackpackCurioRenderer;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.util.InventoryUtil;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.List;
import java.util.Optional;

@Integration("curios")
public class CuriosIntegration {
    public void apply() {
        AnvilCraft.MOD_BUS.addListener(this::setup);
        AnvilCraft.MOD_BUS.addListener(this::onLayerRegister);
        AnvilCraft.MOD_BUS.addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(this::onPlayerWearAnvilHammerInCurioSlot);
    }

    private void setup(FMLCommonSetupEvent event) {
        AnvilHammerItem.addIsWearingPredicate(player ->
            CuriosApi.getCuriosInventory(player).map(this::isAnvilHammerWearing).orElse(false)
        );
        IonoCraftBackpackItem.addStackProvider(player ->
            CuriosApi.getCuriosInventory(player).map(this::getIonoCraftBackpackWearing).orElse(ItemStack.EMPTY)
        );
        InventoryUtil.compatConsumer = InventoryUtil.compatConsumer.andThen(
            (items, living) -> CuriosApi.getCuriosInventory(living).ifPresent(
                handler -> handler.findCurios(stack -> true)
                    .forEach(result -> items.add(result.stack()))
            )
        );
        AmuletManager.INSTANCE.registerFinders((player, holders) -> {
            if (CuriosApi.getCuriosInventory(player).isPresent()) {
                List<SlotResult> results = CuriosApi.getCuriosInventory(player).get()
                    .findCurios(stack -> stack.is(ModItemTags.AMULET));
                for (SlotResult result : results) {
                    AmuletManager.processFoundStack(result.stack(), holders);
                }
            }
        });
        if (ModList.get().isLoaded("create")) {
            GogglesItem.addIsWearingPredicate(player ->
                CuriosApi.getCuriosInventory(player).map(this::isAnvilHammerWearing).orElse(false)
            );
        }
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
//        if (AnvilCraft.config.goggleMode != GoggleMode.WEARING_HAMMER) return false;
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

    public void onPlayerWearAnvilHammerInCurioSlot(CurioChangeEvent event) {
        LivingEntity entity = event.getEntity();
        ItemStack eventTo = event.getTo();
        if (entity instanceof Player && eventTo.getItem() instanceof AnvilHammerItem) {
            Optional<PowerGrid> powerGrid = PowerGrid.findPowerGridContains(entity.level(), entity.position());
            if (powerGrid.isPresent() && powerGrid.get().isWorking()) {
                TriggerUtil.playerWearAnvilHammer(entity.level(), BlockPos.containing(entity.position()));
            }
        }
    }
}
