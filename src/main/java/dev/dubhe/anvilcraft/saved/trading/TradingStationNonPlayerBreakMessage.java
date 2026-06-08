package dev.dubhe.anvilcraft.saved.trading;

import dev.anvilcraft.lib.v2.util.component.DirectInfo;
import dev.anvilcraft.lib.v2.util.component.MultilineComponentHelper;
import dev.dubhe.anvilcraft.util.ComponentUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;

public record TradingStationNonPlayerBreakMessage(
    UUID owner,
    ResourceKey<Level> dimension,
    BlockPos pos,
    long time,
    List<UUID> onliners,
    @Nullable UUID closest
) implements ITradingStationMessage {
    @Override
    public Component getRealTimeMessage(Function<UUID, Component> getter) {
        MultilineComponentHelper helper = MultilineComponentHelper.create()
            .addln("message.anvilcraft.trading_station.break.non_player.title")
            .addln("message.anvilcraft.trading_station.break.owner", getter.apply(this.owner))
            .addln(
                "message.anvilcraft.trading_station.break.pos",
                this.pos.getX(),
                this.pos.getY(),
                this.pos.getZ(),
                ComponentUtil.dimension(this.dimension)
            )
            .addln(
                "message.anvilcraft.trading_station.break.time",
                ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.ddzHH:mm:ss.SSS"))
            )
            .list(
                Component.translatable("message.anvilcraft.trading_station.break.onliners"),
                this.onliners,
                id -> new DirectInfo[] {new DirectInfo(getter.apply(id))}
            );
        if (this.closest != null) {
            helper.addln("message.anvilcraft.trading_station.break.closest", getter.apply(this.closest));
        }
        return helper.build();
    }

    @Override
    public Component getOwnerMessage(Function<UUID, Component> getter) {
        MultilineComponentHelper helper = MultilineComponentHelper.create()
            .addln("message.anvilcraft.trading_station.break.non_player.title")
            .addln(
                "message.anvilcraft.trading_station.break.pos",
                this.pos.getX(),
                this.pos.getY(),
                this.pos.getZ(),
                ComponentUtil.dimension(this.dimension)
            )
            .addln(
                "message.anvilcraft.trading_station.break.time",
                ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.ddzHH:mm:ss.SSS"))
            )
            .list(
                Component.translatable("message.anvilcraft.trading_station.break.onliners"),
                this.onliners,
                id -> new DirectInfo[] {new DirectInfo(getter.apply(id))}
            );
        if (this.closest != null) {
            helper.addln("message.anvilcraft.trading_station.break.closest", getter.apply(this.closest));
        }
        return helper.build();
    }
}
