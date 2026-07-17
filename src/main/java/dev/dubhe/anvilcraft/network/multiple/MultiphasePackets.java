package dev.dubhe.anvilcraft.network.multiple;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MultiphasePackets {
    private static <T extends IPacket> Type<T> of(String path) {
        return IPacket.type(AnvilCraft.of("multiphase_" + path));
    }

    public record SwitchPhase() implements IServerboundPacket {
        public static final Type<SwitchPhase> TYPE = MultiphasePackets.of("switch_phase");
        public static final StreamCodec<ByteBuf, SwitchPhase> STREAM_CODEC = StreamCodec.unit(new SwitchPhase());

        @Override
        public Type<SwitchPhase> type() {
            return TYPE;
        }

        @Override
        public void handleOnServer(Player player) {
            ItemStack stack = findHeldMultiphase(player);
            if (stack == null) return;
            stack.get(ModComponents.MULTIPHASE).cycle(stack);
        }
    }

    public record ChangePhase(InteractionHand hand, byte index) implements IServerboundPacket {
        public static final Type<ChangePhase> TYPE = MultiphasePackets.of("change_phase");
        public static final StreamCodec<ByteBuf, ChangePhase> STREAM_CODEC = StreamCodec.composite(
            StreamCodecUtil.enumStreamCodec(InteractionHand.class),
            ChangePhase::hand,
            ByteBufCodecs.BYTE,
            ChangePhase::index,
            ChangePhase::new
        );

        public ChangePhase(InteractionHand hand, int index) {
            this(hand, (byte) index);
        }

        @Override
        public Type<ChangePhase> type() {
            return TYPE;
        }

        @Override
        public void handleOnServer(Player player) {
            ItemStack stack = player.getItemInHand(this.hand);
            if (!stack.has(ModComponents.MULTIPHASE)) return;
            stack.get(ModComponents.MULTIPHASE).select(stack, this.index);
        }
    }

    private static ItemStack findHeldMultiphase(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.has(ModComponents.MULTIPHASE)) return mainHand;
        ItemStack offHand = player.getOffhandItem();
        return offHand.has(ModComponents.MULTIPHASE) ? offHand : null;
    }
}
