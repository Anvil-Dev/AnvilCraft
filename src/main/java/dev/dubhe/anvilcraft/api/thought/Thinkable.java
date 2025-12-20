package dev.dubhe.anvilcraft.api.thought;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforgespi.Environment;

import java.util.List;

public interface Thinkable {
    @OnlyIn(Dist.CLIENT)
    default void appendHoverText(List<Component> tooltipComponents) {
        if (!Environment.get().getDist().isClient()) {
            return;
        }
        long lastThoughtTime = ThoughtManager.getLastThoughtTime();
        if (lastThoughtTime <= 0) {
            tooltipComponents.add(
                Component.translatable(
                    "tooltip.anvilcraft.thought",
                    Component.keybind("key.anvilcraft.thought")
                ).withStyle(ChatFormatting.GRAY)
            );
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        long curTime = minecraft.gui.getGuiTicks();
        long deltaTime = curTime - lastThoughtTime;
        final int maxPlaceholderCount = 20;
        final double maxSeconds = ThoughtManager.getMAX_SECONDS();
        int placeholderCount = (int) Math.floor(Math.min(deltaTime, 20 * maxSeconds) / (20 * maxSeconds) * maxPlaceholderCount);
        int blankCount = maxPlaceholderCount - placeholderCount;
        StringBuilder builder = new StringBuilder("[");
        builder.append("||".repeat(Math.max(0, placeholderCount)));
        builder.append(" ".repeat(Math.max(0, blankCount)));
        tooltipComponents.add(Component.literal(builder.append("]").toString()).withStyle(ChatFormatting.GRAY));
    }

    @OnlyIn(Dist.CLIENT)
    default void onThought() {
    }
}
