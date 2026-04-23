package dev.dubhe.anvilcraft.network.multiple;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.ISensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.MultiphaseRef;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphase;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphases;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
            Optional.of(player.getMainHandItem()).filter(stack -> stack.has(ModComponents.MULTIPHASE))
                .or(() -> Optional.of(player.getOffhandItem()).filter(stack -> stack.has(ModComponents.MULTIPHASE)))
                .ifPresent(stack -> Objects.requireNonNull(stack.get(ModComponents.MULTIPHASE)).cyclePhases(stack));
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
            var stackOp = Optional.of(player.getItemInHand(this.hand))
                .filter(stack -> stack.has(ModComponents.MULTIPHASE));
            if (stackOp.isEmpty()) return;
            var stack = stackOp.get();
            var multiphase = stack.get(ModComponents.MULTIPHASE).toMultiphase();
            multiphase.changePhase(stack, this.index);
        }
    }

    public record AllSync(Map<UUID, Multiphase> multiphases, Set<UUID> recoverableIds) implements IClientboundPacket {
        public static final Type<AllSync> TYPE = MultiphasePackets.of("all_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, AllSync> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, Multiphase.STREAM_CODEC),
            AllSync::multiphases,
            ByteBufCodecs.collection(HashSet::new, UUIDUtil.STREAM_CODEC),
            AllSync::recoverableIds,
            AllSync::new
        );

        @Override
        public Type<AllSync> type() {
            return TYPE;
        }

        @Override
        public void handleOnClient(Player player) {
            Multiphases.get().sync(this.multiphases, this.recoverableIds);
        }
    }

    public record RecoverClear() implements IClientboundPacket {
        public static final Type<RecoverClear> TYPE = MultiphasePackets.of("recover_clear");
        public static final StreamCodec<ByteBuf, RecoverClear> STREAM_CODEC = StreamCodec.unit(new RecoverClear());

        @Override
        public Type<RecoverClear> type() {
            return TYPE;
        }

        @Override
        public void handleOnClient(Player player) {
            Multiphases.get().clearRecover();
        }
    }

    public record RefSync(int index, Optional<UUID> id) implements ISensitiveBiPacket {
        public static final Type<RefSync> TYPE = MultiphasePackets.of("ref_sync");
        public static final StreamCodec<ByteBuf, RefSync> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            RefSync::index,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            RefSync::id,
            RefSync::new
        );

        public RefSync(int index) {
            this(index, Optional.empty());
        }

        @Override
        public Type<RefSync> type() {
            return TYPE;
        }

        @Override
        public void handleOnClient(Player player) {
            player
                .getInventory()
                .getItem(this.index)
                .set(ModComponents.MULTIPHASE, new MultiphaseRef(this.id().orElseThrow()));
        }

        @Override
        public void handleOnServer(Player player) {
            var stack = player.getInventory().getItem(this.index);
            var ref = stack.get(ModComponents.MULTIPHASE);
            if (ref == null) {
                return;
            } else if (ref.isEmpty()) {
                ref = new MultiphaseRef(stack.getItem().getDescription());
                stack.set(ModComponents.MULTIPHASE, ref);
            }
            PacketDistributor.sendToPlayer(Util.cast(player), new RefSync(this.index, ref.id().optionalGet()));
        }
    }

    public record SingleSync(UUID id, Optional<Multiphase> multiphase) implements ISensitiveBiPacket {
        public static final Type<SingleSync> TYPE = MultiphasePackets.of("single_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, SingleSync> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            SingleSync::id,
            ByteBufCodecs.optional(Multiphase.STREAM_CODEC),
            SingleSync::multiphase,
            SingleSync::new
        );

        public SingleSync(UUID id) {
            this(id, Optional.empty());
        }

        private SingleSync(UUID id, Multiphase multiphase) {
            this(id, Optional.of(multiphase));
        }

        @Override
        public Type<SingleSync> type() {
            return TYPE;
        }

        @Override
        public void handleOnClient(Player player) {
            Multiphases.get().put(this.id, this.multiphase.orElseThrow());
        }

        @Override
        public void handleOnServer(Player player) {
            PacketDistributor.sendToPlayer(
                Util.cast(player),
                new SingleSync(this.id, Multiphases.get().getOrCreate(this.id))
            );
        }
    }
}
