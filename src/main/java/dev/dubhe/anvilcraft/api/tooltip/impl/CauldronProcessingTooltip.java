package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity.RecipePreview;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** 将鱼缸和大锅的加工预览关联到各自的原料、产物与液层，供铁砧锤与 Jade 显示。 */
public final class CauldronProcessingTooltip {
    private static final String PREFIX = "tooltip.anvilcraft.large_cauldron.";

    private CauldronProcessingTooltip() {
    }

    static List<Component> contents(BlockEntity entity) {
        Contents contents = Contents.of(entity);
        if (contents == null) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        lines.add(heading("inputs"));
        for (int slot = 0; slot < contents.input.size(); slot++) {
            ItemResource item = contents.input.getResource(slot);
            if (!item.isEmpty()) {
                int inputSlot = slot;
                lines.add(itemLine(item, contents.input.getAmountAsInt(slot), contents, preview -> preview.inputSlot() == inputSlot));
            }
        }
        lines.add(heading("outputs"));
        Map<ItemResource, Integer> outputs = new LinkedHashMap<>();
        for (int slot = 0; slot < contents.output.size(); slot++) {
            ItemResource item = contents.output.getResource(slot);
            if (!item.isEmpty()) {
                outputs.merge(item, contents.output.getAmountAsInt(slot), Integer::sum);
            }
        }
        outputs.forEach((item, count) -> lines.add(itemLine(item, count, contents, preview ->
            preview.outputSlot() >= 0 && contents.output.getResource(preview.outputSlot()).equals(item))));
        lines.add(heading("fluids"));
        for (int layer = 0; layer < contents.fluids.size(); layer++) {
            FluidStack fluid = contents.fluids.get(layer);
            MutableComponent line = Component.empty().append(fluid.getHoverName())
                .append(" " + UnitUtil.fluidUnit(fluid.getAmount(), false))
                .withStyle(ChatFormatting.GRAY);
            if (layer == 0 && contents.burning) {
                line.append(Component.translatable(PREFIX + "burning").withStyle(ChatFormatting.RED));
            }
            lines.add(withCategories(line, contents, preview -> usesFluid(preview, fluid)));
        }
        Set<String> unattached = new LinkedHashSet<>();
        for (RecipePreview preview : contents.previews) {
            if (preview.inputSlot() < 0 && preview.outputSlot() < 0
                && contents.fluids.stream().noneMatch(fluid -> usesFluid(preview, fluid))) {
                unattached.add(preview.categoryPath());
            }
        }
        unattached.forEach(category -> lines.add(process(category)));
        return lines;
    }

    /** 每个可执行的配方显示一次，可在服务端构造后交给客户端翻译。 */
    public static List<Component> previewLines(BlockEntity entity) {
        Contents contents = Contents.of(entity);
        if (contents == null || contents.previews.isEmpty()) {
            return List.of();
        }
        Map<Identifier, Summary> recipes = new LinkedHashMap<>();
        for (RecipePreview preview : contents.previews) {
            var summary = recipes.computeIfAbsent(preview.recipeId(), id -> new Summary(preview.categoryPath(), new LinkedHashSet<>()));
            if (preview.inputSlot() >= 0) {
                summary.sources.add(contents.input.getResource(preview.inputSlot()).toStack().getHoverName());
            }
            if (preview.outputSlot() >= 0) {
                summary.sources.add(contents.output.getResource(preview.outputSlot()).toStack().getHoverName());
            }
            for (FluidStack fluid : contents.fluids) {
                if (usesFluid(preview, fluid)) {
                    summary.sources.add(fluid.getHoverName());
                }
            }
        }
        List<Component> lines = new ArrayList<>(recipes.size());
        for (Summary summary : recipes.values()) {
            MutableComponent line = process(summary.category);
            if (!summary.sources.isEmpty()) {
                var sources = ComponentUtils.formatList(summary.sources, ComponentUtils.DEFAULT_SEPARATOR);
                line.append(Component.literal(" · ").withStyle(ChatFormatting.GRAY)).append(sources.copy().withStyle(ChatFormatting.GRAY));
            }
            lines.add(line);
        }
        return lines;
    }

    private static Component heading(String key) {
        return Component.translatable(PREFIX + key).withStyle(ChatFormatting.BLUE);
    }

    private static MutableComponent process(String category) {
        return Component.translatable(PREFIX + "preview", categoryName(category)).withStyle(ChatFormatting.GOLD);
    }

    private static Component itemLine(ItemResource item, int count, Contents contents, Predicate<RecipePreview> matches) {
        var line = Component.empty().append(item.toStack().getHoverName()).append(" x" + count).withStyle(ChatFormatting.GRAY);
        return withCategories(line, contents, matches);
    }

    private static Component withCategories(MutableComponent line, Contents contents, Predicate<RecipePreview> matches) {
        List<Component> categories = contents.previews.stream().filter(matches).map(RecipePreview::categoryPath)
            .distinct().map(CauldronProcessingTooltip::categoryName).toList();
        if (!categories.isEmpty()) {
            line.append(Component.translatable(PREFIX + "will_process", ComponentUtils.formatList(categories, Component.literal(" / ")))
                .withStyle(ChatFormatting.GOLD));
        }
        return ITooltipProvider.withIndentAndMerge(line);
    }

    private static Component categoryName(String category) {
        String fallback = FormattingUtil.toEnglishName(category.replace('.', '_').replace('/', '_').replace('-', '_'));
        return Component.translatableWithFallback("gui.anvilcraft.category." + category, fallback);
    }

    private static boolean usesFluid(RecipePreview preview, FluidStack fluid) {
        return preview.fluidPredicates().stream().anyMatch(predicate -> predicate.hasCheck() && predicate.matchesFluid(fluid));
    }

    private record Summary(String category, Set<Component> sources) {
    }

    private record Contents(ResourceHandler<ItemResource> input, ResourceHandler<ItemResource> output, List<FluidStack> fluids,
                            boolean burning, List<RecipePreview> previews) {
        private static @Nullable Contents of(BlockEntity entity) {
            if (entity instanceof LargeCauldronBlockEntity part) {
                LargeCauldronBlockEntity pot = part.getMainPart();
                var layers = new ArrayList<FluidStack>();
                for (int layer = pot.getFluids().getLayerCount() - 1; layer >= 0; layer--) {
                    layers.add(pot.getFluids().getFluidInTank(layer));
                }
                return new Contents(pot.getInputHandler(), pot.getOutputHandler(), layers, pot.isIgnited(), pot.getRecipePreviews());
            }
            if (entity instanceof FishTankBlockEntity tank) {
                FluidStack fluid = tank.getFluidHandler().getStack();
                return new Contents(tank.getInputHandler(), tank.getOutputHandler(), fluid.isEmpty() ? List.of() : List.of(fluid),
                    tank.isIgnited(), tank.getRecipePreviews());
            }
            return null;
        }
    }
}
