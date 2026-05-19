package dev.dubhe.anvilcraft.api.thought;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLLoader;

import java.util.function.Consumer;

public interface Thinkable {
    default void appendHoverText(Consumer<Component> consumer) {
        if (!FMLLoader.getCurrent().getDist().isClient()) return;
        long lastThoughtTime = ThoughtManager.getLastThoughtTime();
        if (lastThoughtTime <= 0) {
            consumer.accept(
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
        final double maxSeconds = ThoughtManager.getMaxSeconds();
        int placeholderCount = (int) Math.floor(Math.min(deltaTime, 20 * maxSeconds) / (20 * maxSeconds) * maxPlaceholderCount);
        int blankCount = maxPlaceholderCount - placeholderCount;
        StringBuilder builder = new StringBuilder("[");
        builder.repeat("||", Math.max(0, placeholderCount));
        builder.repeat(" ", Math.max(0, blankCount));
        consumer.accept(Component.literal(builder.append("]").toString()).withStyle(ChatFormatting.GRAY));
    }

    default void onThought() {
    }
}
