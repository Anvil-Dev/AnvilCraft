package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.integration.jade.provider.CauldronItemStorage;
import dev.dubhe.anvilcraft.integration.jade.provider.CauldronProcessingProvider;
import dev.dubhe.anvilcraft.integration.jade.provider.LargeFluidTankProvider;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.ResizeableElement;
import snownee.jade.api.ui.TextElement;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.ViewGroup;
import snownee.jade.impl.ui.ItemStackElement;
import snownee.jade.impl.ui.ProgressElement;
import snownee.jade.overlay.DisplayHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** 在 Jade 已显示的库存行内标注下一次加工消耗的原料。 */
public enum CauldronProcessingClientProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final int GAP = 6;
    private static final StreamCodec<RegistryFriendlyByteBuf, Map.Entry<Identifier, List<ViewGroup<ItemStack>>>> ITEMS =
        ViewGroup.listCodec(ItemStack.OPTIONAL_STREAM_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, Map.Entry<Identifier, List<ViewGroup<FluidView.Data>>>> FLUIDS =
        ViewGroup.listCodec(FluidView.Data.STREAM_CODEC);

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var annotations = CauldronProcessingProvider.readAnnotations(accessor);
        if (!annotations.items().isEmpty()) {
            var group = readGroup(accessor, JadeIds.UNIVERSAL_ITEM_STORAGE, ITEMS);
            if (group != null && (group.getKey().equals(CauldronItemStorage.UID)
                || group.getKey().equals(JadeIds.UNIVERSAL_ITEM_STORAGE_DEFAULT))) {
                var items = group.getValue().views.iterator();
                tooltip.replace(JadeIds.UNIVERSAL_ITEM_STORAGE, lines -> itemRows(lines, items, annotations.items()));
            }
        }
        if (!annotations.fluids().isEmpty()) {
            var group = readGroup(accessor, JadeIds.UNIVERSAL_FLUID_STORAGE, FLUIDS);
            if (group != null && (group.getKey().equals(JadeIds.UNIVERSAL_FLUID_STORAGE_DEFAULT)
                || group.getKey().equals(LargeFluidTankProvider.UID))) {
                var fluids = group.getValue().views.stream().filter(data -> data.capacity() > 0 && data.fluids().size() <= 1)
                    .map(data -> {
                        if (data.fluids().isEmpty()) {
                            return FluidResource.EMPTY;
                        }
                        var fluid = data.fluids().getFirst();
                        return FluidResource.of(fluid.typeHolder(), fluid.getComponents());
                    }).iterator();
                IWailaConfig.HandlerDisplayStyle style = config.getEnum(JadeIds.UNIVERSAL_FLUID_STORAGE_STYLE);
                tooltip.replace(JadeIds.UNIVERSAL_FLUID_STORAGE, lines -> {
                    for (var line : lines) {
                        if (!fluids.hasNext()) {
                            break;
                        }
                        boolean fluidRow = switch (style) {
                            case PROGRESS_BAR -> line.size() == 1 && line.getFirst() instanceof ProgressElement;
                            case ICON -> line.size() == 2 && line.getLast() instanceof TextElement;
                            case PLAIN_TEXT -> line.size() == 1 && line.getFirst() instanceof TextElement;
                        };
                        if (!fluidRow) {
                            continue;
                        }
                        Component label = annotations.fluids().get(fluids.next());
                        if (label == null) {
                            continue;
                        }
                        if (line.size() == 1 && line.getFirst() instanceof ProgressElement bar) {
                            line.set(0, new AnnotatedBar(bar, label));
                        } else {
                            appendLabel(line, label);
                        }
                    }
                    return lines;
                });
            }
        }
    }

    private static <T> Map.@Nullable Entry<Identifier, ViewGroup<T>> readGroup(
        BlockAccessor accessor, Identifier id, StreamCodec<RegistryFriendlyByteBuf, Map.Entry<Identifier, List<ViewGroup<T>>>> codec
    ) {
        var encoded = accessor.getServerData().get(id.toString());
        if (encoded == null) {
            return null;
        }
        return accessor.decodeFromNbt(codec, encoded).filter(entry -> entry.getValue().size() == 1)
            .map(entry -> Map.entry(entry.getKey(), entry.getValue().getFirst())).orElse(null);
    }

    private static List<List<LayoutElement>> itemRows(
        List<List<LayoutElement>> lines, Iterator<ItemStack> items, Map<ItemResource, Component> labels
    ) {
        List<List<LayoutElement>> result = new ArrayList<>();
        for (var line : lines) {
            if (line.size() >= 2 && line.getLast() instanceof TextElement && items.hasNext()) {
                Component label = labels.get(ItemResource.of(items.next()));
                if (label != null) {
                    appendLabel(line, label);
                }
                result.add(line);
            } else if (!line.isEmpty() && line.stream().allMatch(ItemStackElement.class::isInstance)) {
                // 紧凑视图一行可以有多个图标。单独排出带预览的图标，避免说明指向相邻物品。
                List<LayoutElement> icons = new ArrayList<>();
                for (var icon : line) {
                    Component label = labels.get(ItemResource.of(((ItemStackElement) icon).getItem()));
                    if (label == null) {
                        icons.add(icon);
                        continue;
                    }
                    if (!icons.isEmpty()) {
                        result.add(icons);
                        icons = new ArrayList<>();
                    }
                    List<LayoutElement> row = new ArrayList<>();
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

    private static void appendLabel(List<LayoutElement> line, Component label) {
        line.add(JadeUI.spacer(GAP, 0).flexGrow(1));
        line.add(JadeUI.text(label));
    }

    @Override
    public int getDefaultPriority() {
        return TooltipPosition.TAIL;
    }

    @Override
    public Identifier getUid() {
        return CauldronProcessingProvider.UID;
    }

    private static final class AnnotatedBar extends ResizeableElement {
        private final ProgressElement bar;
        private final Component label;
        private final int labelWidth;

        private AnnotatedBar(ProgressElement bar, Component label) {
            this.bar = bar;
            this.label = label;
            this.labelWidth = DisplayHelper.font().width(label);
            this.width = bar.getWidth() + GAP + this.labelWidth;
            this.height = bar.getHeight();
            this.setFlexGrow(1);
        }

        @Override
        public void setFreeSpace(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public @Nullable Component getNarration() {
            Component material = this.bar.getNarration();
            return material == null ? this.label : material.copy().append(" ").append(this.label);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            this.bar.setX(this.getX());
            this.bar.setY(this.getY());
            this.bar.setFreeSpace(this.width, this.height);
            this.bar.extractRenderState(graphics, mouseX, mouseY, partialTick);
            IDisplayHelper.get().drawText(graphics, this.label, this.getX() + this.width - 4 - this.labelWidth, this.getY() + 3,
                IThemeHelper.get().getNormalColor());
        }
    }
}
