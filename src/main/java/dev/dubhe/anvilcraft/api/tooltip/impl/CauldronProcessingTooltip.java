package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity.RecipePreview;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 铁砧锤按槽位、Jade 按资源展示库存，两者共用下一次落砧的原料消耗。 */
public final class CauldronProcessingTooltip {
    private static final String PREFIX = "tooltip.anvilcraft.large_cauldron.";

    private CauldronProcessingTooltip() {
    }

    /** Jade 合并相同资源，查找注释时保留组件以区分同名材料。 */
    public record Annotations(Map<ItemResource, Component> items, Map<FluidResource, Component> fluids) {
    }

    /** 左栏保留库存，预览独占右栏；流体行还提供绘制液量条所需的存量和容量。 */
    public record Row(Component label, @Nullable Component preview, FluidStack fluid, int capacity) {
        private static Row text(Component label) {
            return new Row(label, null, FluidStack.EMPTY, 0);
        }
    }

    public static Annotations annotations(BlockEntity entity) {
        Contents contents = Contents.of(entity);
        if (contents == null) {
            return new Annotations(Map.of(), Map.of());
        }
        Map<ItemResource, Use> items = new HashMap<>();
        Map<FluidResource, Use> fluids = new HashMap<>();
        for (RecipePreview preview : contents.previews) {
            preview.inputCounts().forEach((slot, count) -> add(items, contents.input.getResource(slot), preview, count));
            preview.outputCounts().forEach((slot, count) -> add(items, contents.output.getResource(slot), preview, count));
            preview.fluidAmounts().forEach((fluid, amount) -> add(fluids, fluid, preview, amount));
        }
        return new Annotations(labels(items, "x"), labels(fluids, "mB"));
    }

    public static List<Row> rows(BlockEntity entity, boolean exact) {
        Contents contents = Contents.of(entity);
        if (contents == null) {
            return List.of();
        }
        Map<Integer, Use> inputs = new HashMap<>();
        Map<ItemResource, Use> outputs = new HashMap<>();
        Map<FluidResource, Use> fluids = new HashMap<>();
        for (RecipePreview preview : contents.previews) {
            preview.inputCounts().forEach((slot, count) -> add(inputs, slot, preview, count));
            preview.outputCounts().forEach((slot, count) -> add(outputs, contents.output.getResource(slot), preview, count));
            preview.fluidAmounts().forEach((fluid, amount) -> add(fluids, fluid, preview, amount));
        }
        List<Row> rows = new ArrayList<>();
        rows.add(heading("inputs"));
        for (int slot = 0; slot < contents.input.size(); slot++) {
            ItemResource item = contents.input.getResource(slot);
            if (!item.isEmpty()) {
                rows.add(itemRow(item, contents.input.getAmountAsInt(slot), inputs.get(slot)));
            }
        }
        rows.add(heading("outputs"));
        Map<ItemResource, Integer> stacks = new LinkedHashMap<>();
        for (int slot = 0; slot < contents.output.size(); slot++) {
            ItemResource item = contents.output.getResource(slot);
            if (!item.isEmpty()) {
                stacks.merge(item, contents.output.getAmountAsInt(slot), Integer::sum);
            }
        }
        stacks.forEach((item, count) -> rows.add(itemRow(item, count, outputs.get(item))));
        rows.add(heading("fluids"));
        boolean top = true;
        for (int layer = contents.fluids.size() - 1; layer >= 0; layer--) {
            FluidResource resource = contents.fluids.getResource(layer);
            if (resource.isEmpty()) {
                continue;
            }
            FluidStack fluid = resource.toStack(contents.fluids.getAmountAsInt(layer));
            var label = Component.empty().append(fluid.getHoverName()).append(" " + UnitUtil.fluidUnit(fluid.getAmount(), exact));
            if (top && contents.burning) {
                label.append(Component.translatable(PREFIX + "burning").withStyle(ChatFormatting.RED));
            }
            top = false;
            Use use = fluids.get(resource);
            rows.add(new Row(label, use == null ? null : use.label("mB"), fluid, contents.fluids.getCapacityAsInt(layer, resource)));
        }
        if (entity instanceof FishTankBlockEntity) {
            FluidResource resource = contents.fluids.getResource(0);
            rows.add(Row.text(Component.translatable("tooltip.anvilcraft.fluid_tank.capacity").withStyle(ChatFormatting.BLUE)));
            rows.add(Row.text(Component.translatable("tooltip.anvilcraft.fluid_tank.capacity.value",
                UnitUtil.fluidUnit(contents.fluids.getAmountAsLong(0), exact),
                UnitUtil.fluidUnit(contents.fluids.getCapacityAsLong(0, resource), exact)).withStyle(ChatFormatting.GRAY)));
        }
        return rows;
    }

    private static Row heading(String key) {
        return Row.text(Component.translatable(PREFIX + key).withStyle(ChatFormatting.BLUE));
    }

    private static Row itemRow(ItemResource item, int count, @Nullable Use use) {
        Component label = ITooltipProvider.withIndentAndMerge(item.toStack().getHoverName(), Component.literal(" x" + count))
            .copy().withStyle(ChatFormatting.GRAY);
        return new Row(label, use == null ? null : use.label("x"), FluidStack.EMPTY, 0);
    }

    private static <K> void add(Map<K, Use> uses, K key, RecipePreview preview, int amount) {
        // 同一行只提示最先执行的类型。同类型的批次可在一次落砧中消耗同一行的多份材料。
        uses.merge(key, new Use(preview.categoryPath(), amount), (first, next) ->
            first.category.equals(next.category) ? new Use(first.category, first.amount + next.amount) : first);
    }

    private static <K> Map<K, Component> labels(Map<K, Use> uses, String unit) {
        Map<K, Component> labels = new HashMap<>();
        uses.forEach((key, use) -> labels.put(key, use.label(unit)));
        return labels;
    }

    private record Use(String category, int amount) {
        private Component label(String unit) {
            String fallback = FormattingUtil.toEnglishName(this.category.replace('.', '_').replace('/', '_').replace('-', '_'));
            var type = Component.translatableWithFallback("gui.anvilcraft.category." + this.category, fallback);
            String quantity = this.amount + unit;
            return Component.translatable(PREFIX + "will_process", quantity, type).withStyle(ChatFormatting.GOLD);
        }
    }

    private record Contents(
        ResourceHandler<ItemResource> input, ResourceHandler<ItemResource> output, ResourceHandler<FluidResource> fluids,
        boolean burning, List<RecipePreview> previews
    ) {
        private static @Nullable Contents of(BlockEntity entity) {
            if (entity instanceof LargeCauldronBlockEntity part) {
                LargeCauldronBlockEntity pot = part.getMainPart();
                return new Contents(pot.getInputHandler(), pot.getOutputHandler(), pot.getFluids(),
                    pot.isIgnited(), pot.getRecipePreviews());
            }
            if (entity instanceof FishTankBlockEntity tank) {
                return new Contents(tank.getInputHandler(), tank.getOutputHandler(), tank.getFluidHandler(),
                    tank.isIgnited(), tank.getRecipePreviews());
            }
            return null;
        }
    }
}
