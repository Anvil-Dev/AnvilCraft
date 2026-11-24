package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler.ResourceKey;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity.FluidKey;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity.RecipePreview;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
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
    public record Annotations(Map<ResourceKey, Component> items, Map<FluidKey, Component> fluids) {
    }

    /** 左右两栏分别显示库存和预览。流体行同时携带绘制液量条所需的存量与容量。 */
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
        Map<ResourceKey, Use> items = new HashMap<>();
        Map<FluidKey, Use> fluids = new HashMap<>();
        for (RecipePreview preview : contents.previews) {
            preview.inputCounts().forEach((slot, count) ->
                add(items, ResourceKey.of(contents.input.getStackInSlot(slot)), preview, count));
            preview.outputCounts().forEach((slot, count) ->
                add(items, ResourceKey.of(contents.output.getStackInSlot(slot)), preview, count));
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
        Map<ResourceKey, Use> outputs = new HashMap<>();
        Map<FluidKey, Use> fluids = new HashMap<>();
        for (RecipePreview preview : contents.previews) {
            preview.inputCounts().forEach((slot, count) -> add(inputs, slot, preview, count));
            preview.outputCounts().forEach((slot, count) ->
                add(outputs, ResourceKey.of(contents.output.getStackInSlot(slot)), preview, count));
            preview.fluidAmounts().forEach((fluid, amount) -> add(fluids, fluid, preview, amount));
        }
        List<Row> rows = new ArrayList<>();
        rows.add(heading("inputs"));
        for (int slot = 0; slot < contents.input.getSlots(); slot++) {
            ItemStack item = contents.input.getStackInSlot(slot);
            if (!item.isEmpty()) {
                rows.add(itemRow(item, item.getCount(), inputs.get(slot)));
            }
        }
        rows.add(heading("outputs"));
        Map<ResourceKey, Integer> stacks = new LinkedHashMap<>();
        for (int slot = 0; slot < contents.output.getSlots(); slot++) {
            ItemStack item = contents.output.getStackInSlot(slot);
            if (!item.isEmpty()) {
                stacks.merge(ResourceKey.of(item), item.getCount(), Integer::sum);
            }
        }
        stacks.forEach((item, count) -> rows.add(itemRow(item.stack(), count, outputs.get(item))));
        rows.add(heading("fluids"));
        boolean top = true;
        for (int layer = contents.fluids.getTanks() - 1; layer >= 0; layer--) {
            FluidStack fluid = contents.fluids.getFluidInTank(layer);
            if (fluid.isEmpty()) {
                continue;
            }
            var label = Component.empty().append(fluid.getHoverName()).append(" " + UnitUtil.fluidUnit(fluid.getAmount(), exact));
            if (top && contents.burning) {
                label.append(Component.translatable(PREFIX + "burning").withStyle(ChatFormatting.RED));
            }
            top = false;
            Use use = fluids.get(FluidKey.of(fluid));
            rows.add(new Row(label, use == null ? null : use.label("mB"), fluid, contents.fluids.getTankCapacity(layer)));
        }
        if (entity instanceof FishTankBlockEntity) {
            rows.add(Row.text(Component.translatable("tooltip.anvilcraft.fluid_tank.capacity").withStyle(ChatFormatting.BLUE)));
            rows.add(Row.text(Component.translatable("tooltip.anvilcraft.fluid_tank.capacity.value",
                UnitUtil.fluidUnit(contents.fluids.getFluidInTank(0).getAmount(), exact),
                UnitUtil.fluidUnit(contents.fluids.getTankCapacity(0), exact)).withStyle(ChatFormatting.GRAY)));
        }
        return rows;
    }

    private static Row heading(String key) {
        return Row.text(Component.translatable(PREFIX + key).withStyle(ChatFormatting.BLUE));
    }

    private static Row itemRow(ItemStack item, int count, @Nullable Use use) {
        Component label = ITooltipProvider.withIndentAndMerge(item.getHoverName(), Component.literal(" x" + count))
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
            return Component.translatable(PREFIX + "processing_step", quantity, type).withStyle(ChatFormatting.GOLD);
        }
    }

    private record Contents(
        IItemHandler input, IItemHandler output, IFluidHandler fluids,
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
