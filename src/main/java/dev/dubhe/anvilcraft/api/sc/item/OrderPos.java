package dev.dubhe.anvilcraft.api.sc.item;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record OrderPos(int position, boolean folded) {
    public static final StreamCodec<ByteBuf, OrderPos> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        OrderPos::position,
        ByteBufCodecs.BOOL,
        OrderPos::folded,
        OrderPos::new
    );

    @Accessors(fluent = true)
    @Data
    public static class Mutable {
        private final int position;
        private boolean folded = false;

        public OrderPos toImmutable() {
            return new OrderPos(this.position, this.folded);
        }
    }
}
