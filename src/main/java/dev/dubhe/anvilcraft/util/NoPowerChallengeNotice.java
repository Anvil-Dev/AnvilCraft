package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.saved.NoPowerChallengeNoticeData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/// 「无电挑战注意事项」的聊天栏提示与展开内容。
public final class NoPowerChallengeNotice {
    private static final String VIEW_COMMAND = "/anvilcraft nopower notice";
    private static final String IGNORE_COMMAND = "/anvilcraft nopower ignore";
    /// 注意正文分为若干段，段号从 1 开始
    private static final int BODY_PARTS = 4;

    private NoPowerChallengeNotice() {
    }

    /// 玩家进入一个还没看过提示的存档时弹一次。
    public static void onPlayerLogin(ServerPlayer player) {
        NoPowerChallengeNoticeData data = NoPowerChallengeNotice.getData(player);
        if (data.isNotified(player.getUUID())) return;
        data.markNotified(player.getUUID());
        player.sendSystemMessage(NoPowerChallengeNotice.header());
        MutableComponent view = Component.translatable("message.anvilcraft.no_power_challenge.view")
            .withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand(NoPowerChallengeNotice.VIEW_COMMAND))
                .withHoverEvent(new HoverEvent.ShowText(
                    Component.translatable("message.anvilcraft.no_power_challenge.view.hover")
                )));
        MutableComponent ignore = Component.translatable("message.anvilcraft.no_power_challenge.ignore")
            .withStyle(style -> style
                .withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent.RunCommand(NoPowerChallengeNotice.IGNORE_COMMAND))
                .withHoverEvent(new HoverEvent.ShowText(
                    Component.translatable("message.anvilcraft.no_power_challenge.ignore.hover")
                )));
        player.sendSystemMessage(Component.empty().append(view).append(" ").append(ignore));
    }

    /// 展开完整说明。
    public static void sendDetails(ServerPlayer player) {
        player.sendSystemMessage(NoPowerChallengeNotice.header());
        for (int part = 1; part <= NoPowerChallengeNotice.BODY_PARTS; part++) {
            player.sendSystemMessage(Component.translatable("message.anvilcraft.no_power_challenge.body." + part)
                .withStyle(ChatFormatting.GRAY));
        }
    }

    /// 忽略提示，并记录下来避免重复弹出。
    public static void ignore(ServerPlayer player) {
        NoPowerChallengeNotice.getData(player).markIgnored(player.getUUID());
        player.sendSystemMessage(Component.translatable("message.anvilcraft.no_power_challenge.ignored")
            .withStyle(ChatFormatting.GRAY));
    }

    private static NoPowerChallengeNoticeData getData(ServerPlayer player) {
        MinecraftServer server = Objects.requireNonNull(player.level().getServer());
        return NoPowerChallengeNoticeData.get(server);
    }

    private static Component header() {
        return Component.translatable("message.anvilcraft.no_power_challenge.header").withStyle(ChatFormatting.GOLD);
    }
}