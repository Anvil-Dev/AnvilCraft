package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.client.renderer.ui.TooltipRenderHelper;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.item.IEngineerGoggles;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
    private int screenHeight;

    @Shadow
    private int screenWidth;

    @Shadow
    @Final
    private Minecraft minecraft;

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
        if (!(minecraft.player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof IEngineerGoggles)) return;
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
        List<SimplePowerGrid> powerGrids = SimplePowerGrid.findPowerGrid(pos);
        if (powerGrids.isEmpty()) return;
        SimplePowerGrid grid = powerGrids.get(0);
        if (grid == null) return;
        PowerComponentInfo componentInfo = grid.getMappedPowerComponentInfo().get(pos);
        final int tooltipPosX = screenWidth / 2 + 10;
        final int tooltipPosY = screenHeight / 2 + 10;
        overloaded |= grid.getConsume() > grid.getGenerate();
        List<Component> lines = new ArrayList<>();

        PowerComponentType type = powerComponent.getComponentType();

        if (type == PowerComponentType.PRODUCER) {
            IPowerProducer producer = (IPowerProducer) powerComponent;
            lines.add(Component.translatable("tooltip.anvilcraft.grid_information.producer_stats")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE))
            );
            lines.add(Component.translatable(
                    "tooltip.anvilcraft.grid_information.output_power",
                    componentInfo == null ? producer.getOutputPower() : componentInfo.produces()
            ).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        } else if (type == PowerComponentType.CONSUMER) {
            IPowerConsumer consumer = (IPowerConsumer) powerComponent;
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
        TooltipRenderHelper.renderTooltipWithItemIcon(
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
}
