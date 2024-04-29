package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(Gui.class)
public abstract class PowerGridInformationRenderMixin {

    @Shadow
    public abstract Font getFont();

    @Shadow
    protected int screenHeight;

    @Shadow
    protected int screenWidth;

    @Shadow
    @Final
    protected Minecraft minecraft;

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;renderCrosshair(Lnet/minecraft/client/gui/GuiGraphics;)V",
                    shift = At.Shift.AFTER
            )
    )
    void onHudRender(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        if (minecraft.player == null || minecraft.isPaused()) return;
        if (minecraft.screen != null) return;
        if (!minecraft.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ANVIL_HAMMER.get())
                && !minecraft.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ROYAL_ANVIL_HAMMER.get())
        ) return;

        HitResult hit = minecraft.hitResult;
        if (hit == null) return;
        BlockPos pos = null;
        boolean overloaded = false;
        IPowerComponent powerComponent = null;
        Block block;
        BlockState state = null;
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult) hit).getBlockPos();
            if (minecraft.level == null) return;
            BlockEntity e = minecraft.level.getBlockEntity(blockPos);
            if (e == null) return;
            if (e instanceof IPowerComponent ipc) {
                if (e.getBlockState().hasProperty(IPowerComponent.OVERLOAD)) {
                    overloaded = e.getBlockState()
                            .getValues()
                            .getOrDefault(IPowerComponent.OVERLOAD, true)
                            .equals(Boolean.TRUE);
                }
                state = e.getBlockState();
                powerComponent = ipc;
                pos = blockPos;
            }
        }
        if (powerComponent == null) return;
        List<PowerGrid.SimplePowerGrid> powerGrids = PowerGrid.SimplePowerGrid.findPowerGrid(pos);
        if (powerGrids.isEmpty()) return;
        PowerGrid.SimplePowerGrid grid = powerGrids.get(0);
        if (grid == null) return;
        PowerComponentInfo componentInfo = grid.getMappedPowerComponentInfo().get(pos);
        final int tooltipPosX = screenWidth / 2 + 10;
        final int tooltipPosY = screenHeight / 2 + 10;
        overloaded |= grid.getConsume() > grid.getGenerate();
        List<Component> lines = new ArrayList<>();

        if (powerComponent instanceof IPowerProducer producer) {
            lines.add(Component.translatable("tooltip.anvilcraft.grid_information.producer_stats")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE))
            );
            lines.add(Component.translatable(
                    "tooltip.anvilcraft.grid_information.output_power",
                    componentInfo == null ? producer.getOutputPower() : componentInfo.produces()
            ).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        } else if (powerComponent instanceof IPowerConsumer consumer) {
            lines.add(Component.translatable("tooltip.anvilcraft.grid_information.consumer_stats")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE))
            );
            lines.add(Component.translatable(
                    "tooltip.anvilcraft.grid_information.input_power",
                    componentInfo == null ? consumer.getInputPower() : componentInfo.consumes()
            ).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        }
        List<Component> tooltipLines = List.of(
                Component.translatable("tooltip.anvilcraft.grid_information.title")
                        .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)),
                Component.translatable(
                        "tooltip.anvilcraft.grid_information.total_consumed",
                        grid.getConsume()
                ).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)),
                Component.translatable(
                        "tooltip.anvilcraft.grid_information.total_generated",
                        grid.getGenerate()
                ).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY))
        );
        lines.addAll(tooltipLines);
        if (overloaded) {
            for (int i = 1; i <= 3; i++) {
                lines.add(Component.translatable("tooltip.anvilcraft.grid_information.overloaded" + i));
            }

        }
        anvilCraft$renderInfo(
                guiGraphics,
                this.getFont(),
                state != null
                        ? state.getBlock().asItem().getDefaultInstance()
                        : ModItems.MAGNETOELECTRIC_CORE.asStack(),
                lines,
                tooltipPosX,
                tooltipPosY
        );
    }

    @Unique
    private static void anvilCraft$renderInfo(
        GuiGraphics thiz, Font font, ItemStack itemStack, @NotNull List<Component> lines, int x, int y
    ) {
        ClientTooltipPositioner tooltipPositioner = DefaultTooltipPositioner.INSTANCE;
        List<ClientTooltipComponent> components = lines.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();
        if (components.isEmpty()) return;
        int width = 0;
        int height = components.size() == 1 ? -2 : 0;

        for (ClientTooltipComponent component : components) {
            width = Math.max(component.getWidth(font), width);
            height += component.getHeight();
        }

        Vector2ic vector2ic = tooltipPositioner.positionTooltip(
                thiz.guiWidth(),
                thiz.guiHeight(),
                x,
                y,
                width,
                height
        );
        int vx = vector2ic.x();
        int vy = vector2ic.y();
        thiz.pose().pushPose();

        int finalVy = vy;
        int finalWidth = width;
        int finalHeight = height + 16;
        TooltipRenderUtil.renderTooltipBackground(
                thiz,
                vx,
                finalVy,
                finalWidth,
                finalHeight,
                400
        );

        thiz.pose().translate(0.0F, 0.0F, 400.0F);

        thiz.renderFakeItem(itemStack, vx, vy);

        vy += 16;

        ClientTooltipComponent component;
        for (int i = 0, q = vy; i < components.size(); ++i) {
            component = components.get(i);
            component.renderText(font, vx, q, thiz.pose().last().pose(), thiz.bufferSource());
            q += component.getHeight() + (i == 0 ? 2 : 0);
        }

        for (int i = 0, q = vy; i < components.size(); ++i) {
            component = components.get(i);
            component.renderImage(font, vx, q, thiz);
            q += component.getHeight() + (i == 0 ? 2 : 0);
        }

        thiz.pose().popPose();
    }
}
