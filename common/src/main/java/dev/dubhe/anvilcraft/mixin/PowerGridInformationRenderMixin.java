package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.IPowerStorage;
import dev.dubhe.anvilcraft.api.power.IPowerTransmitter;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.level.block.entity.BlockEntity;
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

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult) hit).getBlockPos();
            if (minecraft.level == null) return;
            BlockEntity state = minecraft.level.getBlockEntity(blockPos);
            if (state == null) return;
            if (state instanceof IPowerComponent ipc) {
                if (state.getBlockState().hasProperty(IPowerComponent.OVERLOAD)) {
                    overloaded = state.getBlockState()
                            .getValues()
                            .getOrDefault(IPowerComponent.OVERLOAD, true)
                            .equals(Boolean.TRUE);
                }
                powerComponent = ipc;
                pos = blockPos;
            }
        }
        if (powerComponent == null) return;
        List<PowerGrid.SimplePowerGrid> powerGrids = PowerGrid.SimplePowerGrid.findPowerGrid(pos);
        if (powerGrids.isEmpty()) return;
        PowerGrid.SimplePowerGrid grid = powerGrids.get(0);
        if (grid == null) return;
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
                    producer.getOutputPower()
            ).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        } else if (powerComponent instanceof IPowerConsumer consumer) {
            lines.add(Component.translatable("tooltip.anvilcraft.grid_information.consumer_stats")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE))
            );
            lines.add(Component.translatable(
                    "tooltip.anvilcraft.grid_information.input_power",
                    consumer.getInputPower()
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
        guiGraphics.renderComponentTooltip(
                this.getFont(),
                lines,
                tooltipPosX,
                tooltipPosY
        );
    }
}
