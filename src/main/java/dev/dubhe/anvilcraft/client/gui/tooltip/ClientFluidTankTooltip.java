package dev.dubhe.anvilcraft.client.gui.tooltip;

import dev.dubhe.anvilcraft.inventory.tooltip.FluidTankTooltip;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 流体储罐物品 tooltip：每行流体以 <b>16×16 图标 + 名称/数量文字</b> 的形式渲染
 * （图标位于文字之前，同一行内对齐），并附带流体标题与容量行。
 *
 * <p>文字经 {@link #renderText} 绘制、图标经 {@link #renderImage} 绘制，二者基于
 * 相同的行起点与行高增量，因此图标与文字落在同一行。
 */
public class ClientFluidTankTooltip implements ClientTooltipComponent {
    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_FLUIDS = "Fluids";
    private static final String TAG_ENHANCED = "Enhanced";
    private static final String TAG_INFINITE = "Infinite";
    private static final int ICON_SIZE = 16;
    private static final int LINE_HEIGHT = 10;
    private static final int ICON_GAP = 1;
    private static final int MAX_VISIBLE_FLUIDS = 5;

    private final int capacity;
    private final boolean infiniteCapacity;
    private final boolean showCapacity;
    private final List<FluidStack> fluids;
    private final List<Boolean> infiniteFlags;
    private final int remainingFluids;

    public ClientFluidTankTooltip(FluidTankTooltip tooltip) {
        this.capacity = tooltip.capacity();
        this.infiniteCapacity = tooltip.infiniteCapacity();
        this.showCapacity = tooltip.showCapacity();
        CompoundTag tankTag = tooltip.tankTag();
        boolean enhanced = tankTag.getBoolean(TAG_ENHANCED);
        List<FluidStack> fluids = new ArrayList<>();
        List<Boolean> infiniteFlags = new ArrayList<>();
        int remainingFluids = 0;
        HolderLookup.Provider registries = registries();
        if (registries != null) {
            if (tooltip.multi()) {
                ListTag list = tankTag.getList(TAG_FLUIDS, Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag entry = list.getCompound(i);
                    FluidStack fluid = FluidStack.parseOptional(registries, entry.getCompound(TAG_FLUID));
                    if (fluid.isEmpty()) continue;
                    if (fluids.size() < MAX_VISIBLE_FLUIDS) {
                        fluids.add(fluid);
                        infiniteFlags.add(enhanced && entry.getBoolean(TAG_INFINITE));
                    } else {
                        remainingFluids++;
                    }
                }
            } else if (tankTag.contains(TAG_FLUID, Tag.TAG_COMPOUND)) {
                FluidStack fluid = FluidStack.parseOptional(registries, tankTag.getCompound(TAG_FLUID));
                if (!fluid.isEmpty()) {
                    fluids.add(fluid);
                    // 单流体：每行流体无限状态 = 整罐无限容量（创造流体储罐恒为 true）
                    infiniteFlags.add(tooltip.infiniteCapacity());
                }
            }
        }
        this.fluids = fluids;
        this.infiniteFlags = infiniteFlags;
        this.remainingFluids = remainingFluids;
    }

    private static HolderLookup.Provider registries() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            return minecraft.level.registryAccess();
        }
        if (minecraft.getConnection() != null) {
            return minecraft.getConnection().registryAccess();
        }
        return null;
    }

    private long totalAmount() {
        long amount = 0;
        for (FluidStack fluid : fluids) {
            amount += fluid.getAmount();
        }
        return amount;
    }

    @Override
    public int getHeight() {
        int height = fluids.isEmpty() ? 0 : LINE_HEIGHT;
        height += fluids.size() * ICON_SIZE;
        if (remainingFluids > 0) {
            height += LINE_HEIGHT;
        }
        if (showCapacity) {
            height += LINE_HEIGHT * 2; // 容量标题 + 容量值
        }
        return height;
    }

    @Override
    public int getWidth(Font font) {
        int width = 0;
        if (!fluids.isEmpty()) {
            width = Math.max(width, font.width(Component.translatable("tooltip.anvilcraft.fluid_tank.fluid")));
        }
        for (int i = 0; i < fluids.size(); i++) {
            width = Math.max(width, ICON_SIZE + ICON_GAP + font.width(fluidLine(i)));
        }
        if (remainingFluids > 0) {
            width = Math.max(width, font.width(moreLine()));
        }
        if (showCapacity) {
            width = Math.max(width, font.width(Component.translatable("tooltip.anvilcraft.fluid_tank.capacity")));
            width = Math.max(width, font.width(capacityLine()));
        }
        return width;
    }

    @Override
    public void renderText(
        Font font,
        int mouseX,
        int mouseY,
        Matrix4f matrix,
        MultiBufferSource.BufferSource bufferSource
    ) {
        int rowY = mouseY;
        if (!fluids.isEmpty()) {
            font.drawInBatch(
                Component.translatable("tooltip.anvilcraft.fluid_tank.fluid").withStyle(ChatFormatting.BLUE),
                mouseX, rowY, -1, true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880
            );
            rowY += LINE_HEIGHT;
        }
        for (int i = 0; i < fluids.size(); i++) {
            font.drawInBatch(
                fluidLine(i),
                mouseX + ICON_SIZE + ICON_GAP, rowY + (ICON_SIZE - LINE_HEIGHT) / 2,
                -1, true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880
            );
            rowY += ICON_SIZE;
        }
        if (remainingFluids > 0) {
            font.drawInBatch(
                moreLine(),
                mouseX, rowY, -1, true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880
            );
            rowY += LINE_HEIGHT;
        }
        if (showCapacity) {
            font.drawInBatch(
                Component.translatable("tooltip.anvilcraft.fluid_tank.capacity").withStyle(ChatFormatting.BLUE),
                mouseX, rowY, -1, true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880
            );
            rowY += LINE_HEIGHT;
            font.drawInBatch(
                capacityLine(),
                mouseX, rowY, -1, true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880
            );
        }
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        int rowY = y + (fluids.isEmpty() ? 0 : LINE_HEIGHT);
        for (int i = 0; i < fluids.size(); i++) {
            renderFluidIcon(guiGraphics, fluids.get(i), x, rowY);
            rowY += ICON_SIZE;
        }
    }

    /** 第 i 行流体：名称 + 数量（灰色）。 */
    private Component fluidLine(int i) {
        FluidStack fluid = fluids.get(i);
        String amount = infiniteFlags.get(i)
            ? UnitUtil.INFINITE_POWER
            : UnitUtil.fluidUnit(fluid.getAmount(), false);
        return Component.literal("")
            .append(fluid.getHoverName())
            .append(Component.literal(" " + amount))
            .withStyle(ChatFormatting.GRAY);
    }

    private Component moreLine() {
        return Component.translatable("tooltip.anvilcraft.fluid_tank.more", remainingFluids)
            .withStyle(ChatFormatting.GRAY);
    }

    private Component capacityLine() {
        Component value = infiniteCapacity
            ? Component.translatable(
                "tooltip.anvilcraft.fluid_tank.capacity.value.infinity",
                UnitUtil.fluidUnit(totalAmount(), false)
            )
            : Component.translatable(
                "tooltip.anvilcraft.fluid_tank.capacity.value",
                UnitUtil.fluidUnit(totalAmount(), false),
                UnitUtil.fluidUnit(capacity, false)
            );
        return value.copy().withStyle(ChatFormatting.GRAY);
    }

    /** 在 (x,y) 处画 16×16 的流体静止贴图（取自方块图集，带流体色调）。 */
    private static void renderFluidIcon(GuiGraphics guiGraphics, FluidStack fluid, int x, int y) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(ext.getStillTexture(fluid));
        int tint = ext.getTintColor(fluid);
        guiGraphics.blit(
            x, y, 0, ICON_SIZE, ICON_SIZE, sprite,
            FastColor.ARGB32.red(tint) / 255f,
            FastColor.ARGB32.green(tint) / 255f,
            FastColor.ARGB32.blue(tint) / 255f,
            1.0f
        );
    }
}
