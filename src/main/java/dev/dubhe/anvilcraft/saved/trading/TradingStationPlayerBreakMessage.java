package dev.dubhe.anvilcraft.saved.trading;

import dev.anvilcraft.lib.v2.util.component.MultilineComponentHelper;
import dev.dubhe.anvilcraft.util.ComponentUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Function;

public record TradingStationPlayerBreakMessage(
    UUID owner,
    UUID breaker,
    ResourceKey<Level> dimension,
    BlockPos pos,
    long time
) implements ITradingStationMessage {
    @Override
    public Component getRealTimeMessage(Function<UUID, Component> getter) {
        MultilineComponentHelper helper = MultilineComponentHelper.create()
            .addln("message.anvilcraft.trading_station.break.player.title")
            .addln("message.anvilcraft.trading_station.break.owner", getter.apply(this.owner))
            .addln("message.anvilcraft.trading_station.break.breaker", getter.apply(this.breaker))
            .addln(
                "message.anvilcraft.trading_station.break.pos",
                this.pos.getX(),
                this.pos.getY(),
                this.pos.getZ(),
                ComponentUtil.dimension(this.dimension)
            )
            .addln("message.anvilcraft.trading_station.break.time", DateTimeFormatter.ofPattern("yyyy.MM.ddzHH:mm:ss.SSS"));
        return helper.build();
    }

    @Override
    public Component getOwnerMessage(Function<UUID, Component> getter) {
        MultilineComponentHelper helper = MultilineComponentHelper.create()
            .addln("message.anvilcraft.trading_station.break.player.title")
            .addln("message.anvilcraft.trading_station.break.breaker", getter.apply(this.breaker))
            .addln(
                "message.anvilcraft.trading_station.break.pos",
                this.pos.getX(),
                this.pos.getY(),
                this.pos.getZ(),
                ComponentUtil.dimension(this.dimension)
            )
            .addln("message.anvilcraft.trading_station.break.time", DateTimeFormatter.ofPattern("yyyy.MM.ddzHH:mm:ss.SSS"));
        return helper.build();
    }
}
