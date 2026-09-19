package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.SmartBlockPlacerRenderer;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class SmartBlockPlacerItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Map<LivingEntity, DanceAnimation> ANIMATIONS = new WeakHashMap<>();
    private static @Nullable LivingEntity currentWearer;

    private final SmartBlockPlacerBlockEntity preview = new SmartBlockPlacerBlockEntity(
        BlockPos.ZERO, ModBlocks.SMART_BLOCK_PLACER.getDefaultState()
    );

    private SmartBlockPlacerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static void withWearer(LivingEntity wearer, Runnable render) {
        LivingEntity previousWearer = currentWearer;
        currentWearer = wearer;
        try {
            render.run();
        } finally {
            currentWearer = previousWearer;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ANIMATIONS.clear();
            return;
        }
        ANIMATIONS.keySet().removeIf(wearer -> wearer.level() != minecraft.level || wearer.isRemoved()
            || !wearer.getItemBySlot(EquipmentSlot.HEAD).is(ModBlocks.SMART_BLOCK_PLACER.asItem()));
        if (minecraft.isPaused()) return;
        for (var player : minecraft.level.players()) {
            if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModBlocks.SMART_BLOCK_PLACER.asItem())) continue;
            ANIMATIONS.computeIfAbsent(player, wearer -> new DanceAnimation()).tick(hasPower(player));
        }
    }

    private static boolean hasPower(LivingEntity wearer) {
        return wearer.getData(ModDataAttachments.IN_POWER_GRID) && !wearer.getData(ModDataAttachments.POWER_GRID_OVERLOADED);
    }

    @Override
    public void renderByItem(
        ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffer, int light, int overlay
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (context != ItemDisplayContext.HEAD) {
            ItemRenderer renderer = minecraft.getItemRenderer();
            BakedModel model = renderer.getItemModelShaper().getItemModel(stack);
            FilterItemRenderer.renderModel(renderer, stack, pose, buffer, light, overlay, model);
            return;
        }
        LivingEntity wearer = currentWearer;
        boolean powered = wearer != null && hasPower(wearer);
        pose.pushPose();
        pose.translate(0, 0.9, 0);
        minecraft.getBlockRenderer().renderSingleBlock(
            this.preview.getBlockState().setValue(SmartBlockPlacerBlock.OVERLOAD, !powered),
            pose, buffer, light, overlay, ModelData.EMPTY, null
        );
        var renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(this.preview);
        if (renderer instanceof SmartBlockPlacerRenderer placerRenderer) {
            DanceAnimation animation = wearer == null ? null : ANIMATIONS.get(wearer);
            float partialTick = minecraft.isPaused() ? 1 : minecraft.getTimer().getGameTimeDeltaPartialTick(false);
            double renderTick = animation == null ? 0 : animation.getRenderTime(partialTick, powered);
            placerRenderer.renderDancingArm(renderTick, pose, buffer, light, overlay);
        }
        pose.popPose();
    }

    private static class DanceAnimation {
        private long ticks;
        private boolean playing;

        void tick(boolean powered) {
            this.playing = powered;
            if (powered) this.ticks++;
        }

        double getRenderTime(float partialTick, boolean powered) {
            return this.ticks - (this.playing && powered ? 1 - partialTick : 0);
        }
    }

    public static class ItemExtensions implements IClientItemExtensions {
        private @Nullable SmartBlockPlacerItemRenderer renderer;

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) this.renderer = new SmartBlockPlacerItemRenderer();
            return this.renderer;
        }
    }
}
