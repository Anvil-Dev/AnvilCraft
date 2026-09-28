package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.mixin.client.ClientTextTooltipAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class TooltipDedupProbe {
    private static boolean verified;

    public static void verifyNoJade(Minecraft client) {
        var graphics = new GuiGraphicsExtractor(client, new GuiRenderState(), 0, 0);
        graphics.setTooltipForNextFrame(client.font, ModBlocks.SPACE_OVERCOMPRESSOR.asStack(), 100, 100);
        if (readDeferred(graphics).isEmpty()) throw new IllegalStateException("Tooltip missing without Jade");
        AnvilCraft.LOGGER.info("PORT_NO_JADE_TOOLTIP_PASSED");
    }

    public static void verifyDeferred(GuiGraphicsExtractor graphics) {
        if (verified) return;
        List<String> lines = readDeferred(graphics);
        if (lines.stream().filter("AnvilCraft"::equals).count() != 1
            || lines.stream().noneMatch(line -> line.startsWith("Requires 4 "))
            || lines.stream().noneMatch(line -> line.contains("get more info"))) {
            throw new IllegalStateException("JEI/Jade tooltip contents: " + lines);
        }
        int hint = lines.indexOf("Requires 4 Space Over-compressor Expansions");
        if (hint < 0 || lines.indexOf("AnvilCraft") > hint) throw new IllegalStateException("Source mod-name/hint order " + lines);
        var client = Minecraft.getInstance();
        var stack = ModBlocks.SPACE_OVERCOMPRESSOR.asStack();
        var ordinary = new GuiGraphicsExtractor(client, new GuiRenderState(), 0, 0);
        ordinary.setTooltipForNextFrame(client.font, stack, 100, 100);
        if (readDeferred(ordinary).stream().filter("AnvilCraft"::equals).count() != 1) {
            throw new IllegalStateException("Ordinary tooltip lost its mod name");
        }
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("AnvilCraft"));
        var named = new GuiGraphicsExtractor(client, new GuiRenderState(), 0, 0);
        named.setTooltipForNextFrame(client.font, stack, 100, 100);
        if (readDeferred(named).stream().filter("AnvilCraft"::equals).count() != 2) {
            throw new IllegalStateException("Custom item title must remain separate from the mod name");
        }
        AnvilCraft.LOGGER.info("PORT_TOOLTIP_DEDUP_PASSED: JEI hint and guide retained; "
            + "ordinary and renamed items verified; {}", lines);
        verified = true;
    }

    private static List<String> readDeferred(GuiGraphicsExtractor graphics) {
        try {
            var deferredField = GuiGraphicsExtractor.class.getDeclaredField("deferredTooltip");
            deferredField.setAccessible(true);
            Object deferred = deferredField.get(graphics);
            if (deferred == null) throw new IllegalStateException("Missing deferred tooltip");
            List<String> lines = new ArrayList<>();
            for (var field : deferred.getClass().getDeclaredFields()) {
                if (!List.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                for (var component : (List<?>) field.get(deferred)) {
                    if (!(component instanceof ClientTextTooltipAccessor text)) continue;
                    StringBuilder line = new StringBuilder();
                    text.anvilcraft$getText().accept((index, style, codePoint) -> {
                        line.appendCodePoint(codePoint);
                        return true;
                    });
                    lines.add(line.toString());
                }
            }
            return lines;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
