package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler.ResourceKey;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity.FluidKey;
import dev.dubhe.anvilcraft.integration.jade.provider.CauldronProcessingProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.FluidTankProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.ITextElement;
import snownee.jade.api.view.ViewGroup;
import snownee.jade.impl.ui.ItemStackElement;
import snownee.jade.impl.ui.ProgressElement;
import snownee.jade.overlay.DisplayHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** 在 Jade 已显示的库存行内标注下一次加工消耗的原料。 */
public enum CauldronProcessingClientProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final int GAP = 6;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var annotations = CauldronProcessingProvider.readAnnotations(accessor);
        if (!annotations.items().isEmpty()) {
            tooltip.replace(JadeIds.UNIVERSAL_ITEM_STORAGE, lines -> itemRows(lines, annotations.items()));
        }
        if (annotations.fluids().isEmpty()) {
            return;
        }
        var data = accessor.getServerData();
        var provider = ResourceLocation.tryParse(data.getString("JadeFluidStorageUid"));
        if (!JadeIds.UNIVERSAL_FLUID_STORAGE_DEFAULT.equals(provider) && !FluidTankProvider.UID.equals(provider)) {
            return;
        }
        var groups = ViewGroup.readList(data, "JadeFluidStorage", Function.identity());
        if (groups == null || groups.size() != 1) {
            return;
        }
        var fluids = groups.getFirst().views.stream().filter(tag -> tag.getLong("capacity") > 0)
            .flatMap(tag -> JadeFluidObject.CODEC.parse(accessor.nbtOps(), tag.get("fluid")).result().stream()).iterator();
        IWailaConfig.HandlerDisplayStyle style = config.getEnum(JadeIds.UNIVERSAL_FLUID_STORAGE_STYLE);
        tooltip.replace(JadeIds.UNIVERSAL_FLUID_STORAGE, lines -> {
            for (var line : lines) {
                if (!fluids.hasNext()) {
                    break;
                }
                boolean fluidRow = switch (style) {
                    case PROGRESS_BAR -> line.size() == 1 && line.getFirst() instanceof ProgressElement;
                    case ICON -> line.size() == 2 && line.getLast() instanceof ITextElement;
                    case PLAIN_TEXT -> line.size() == 1 && line.getFirst() instanceof ITextElement;
                };
                if (!fluidRow) {
                    continue;
                }
                var fluid = fluids.next();
                var key = new FluidKey(fluid.getType().builtInRegistryHolder(), fluid.getComponents());
                Component label = annotations.fluids().get(key);
                if (label == null) {
                    continue;
                }
                if (line.getFirst() instanceof ProgressElement bar) {
                    line.set(0, new AnnotatedBar(bar, label));
                } else {
                    appendLabel(line, label);
                }
            }
            return lines;
        });
    }

    private static List<List<IElement>> itemRows(
        List<List<IElement>> lines, Map<ResourceKey, Component> labels
    ) {
        List<List<IElement>> result = new ArrayList<>();
        for (var line : lines) {
            if (line.size() >= 2 && line.getLast() instanceof ITextElement && line.getFirst() instanceof ItemStackElement icon) {
                Component label = labels.get(ResourceKey.of(icon.getItem()));
                if (label != null) {
                    appendLabel(line, label);
                }
                result.add(line);
            } else if (!line.isEmpty() && line.stream().allMatch(ItemStackElement.class::isInstance)) {
                // 紧凑视图一行可以有多个图标。单独排出带预览的图标，避免说明指向相邻物品。
                List<IElement> icons = new ArrayList<>();
                for (var icon : line) {
                    Component label = labels.get(ResourceKey.of(((ItemStackElement) icon).getItem()));
                    if (label == null) {
                        icons.add(icon);
                        continue;
                    }
                    if (!icons.isEmpty()) {
                        result.add(icons);
                        icons = new ArrayList<>();
                    }
                    List<IElement> row = new ArrayList<>();
                    row.add(icon);
                    appendLabel(row, label);
                    result.add(row);
                }
                if (!icons.isEmpty()) {
                    result.add(icons);
                }
            } else {
                result.add(line);
            }
        }
        return result;
    }

    private static void appendLabel(List<IElement> line, Component label) {
        var helper = IElementHelper.get();
        line.add(helper.spacer(GAP, 0));
        line.add(helper.text(label).align(IElement.Align.RIGHT));
    }

    @Override
    public int getDefaultPriority() {
        return TooltipPosition.TAIL;
    }

    @Override
    public ResourceLocation getUid() {
        return CauldronProcessingProvider.UID;
    }

    private static final class AnnotatedBar extends Element {
        private final ProgressElement bar;
        private final Component label;
        private final int labelWidth;

        private AnnotatedBar(ProgressElement bar, Component label) {
            this.bar = bar;
            this.label = label;
            this.labelWidth = DisplayHelper.font().width(label);
        }

        @Override
        public Vec2 getSize() {
            Vec2 size = this.bar.getCachedSize();
            return new Vec2(size.x + GAP + this.labelWidth, size.y);
        }

        @Override
        public String getMessage() {
            @Nullable String material = this.bar.getCachedMessage();
            return material == null ? this.label.getString() : material + " " + this.label.getString();
        }

        @Override
        public void render(GuiGraphics graphics, float x, float y, float maxX, float maxY) {
            this.bar.render(graphics, x, y, maxX, maxY);
            IDisplayHelper.get().drawText(graphics, this.label, maxX - 4 - this.labelWidth, y + 3,
                IThemeHelper.get().getNormalColor());
        }
    }
}
