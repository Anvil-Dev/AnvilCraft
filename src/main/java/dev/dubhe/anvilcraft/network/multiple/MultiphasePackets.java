package dev.dubhe.anvilcraft.network.multiple;

import dev.anvilcraft.lib.util.CodecUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.item.property.component.MultiphaseRef;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphase;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphases;
import dev.dubhe.anvilcraft.util.Util;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class MultiphasePackets {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(
            SwitchPhase.TYPE,
            SwitchPhase.STREAM_CODEC,
            SwitchPhase.HANDLER
        );
        registrar.playToServer(
            ChangePhase.TYPE,
            ChangePhase.STREAM_CODEC,
            ChangePhase.HANDLER
        );
        registrar.playToClient(
            AllSync.TYPE,
            AllSync.STREAM_CODEC,
            AllSync.HANDLER
        );
        registrar.playToClient(
            RecoverClear.TYPE,
            RecoverClear.STREAM_CODEC,
            RecoverClear.HANDLER
        );
        registrar.playBidirectional(
            RefSync.TYPE,
            RefSync.STREAM_CODEC,
            RefSync.HANDLER
        );
    }

    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> of(String path) {
        return new CustomPacketPayload.Type<>(AnvilCraft.of("multiphase_" + path));
    }

    public record SwitchPhase() implements CustomPacketPayload {
        public static final Type<SwitchPhase> TYPE = MultiphasePackets.of("switch_phase");
        public static final StreamCodec<ByteBuf, SwitchPhase> STREAM_CODEC = StreamCodec.unit(new SwitchPhase());
        public static final IPayloadHandler<SwitchPhase> HANDLER = SwitchPhase::serverHandler;

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void serverHandler(SwitchPhase data, IPayloadContext context) {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> {
                if (!(player.level() instanceof ServerLevel)) return;
                Optional.of(player.getMainHandItem()).filter(stack -> stack.has(ModComponents.MULTIPHASE))
                    .or(() -> Optional.of(player.getOffhandItem()).filter(stack -> stack.has(ModComponents.MULTIPHASE)))
                    .ifPresent(stack -> Objects.requireNonNull(stack.get(ModComponents.MULTIPHASE)).cyclePhases(stack));
            });
        }
    }

    public record ChangePhase(InteractionHand hand, byte index, boolean merciless) implements CustomPacketPayload {
        public static final Type<ChangePhase> TYPE = MultiphasePackets.of("change_phase");
        public static final StreamCodec<FriendlyByteBuf, ChangePhase> STREAM_CODEC = StreamCodec.composite(
            CodecUtil.enumStreamCodec(InteractionHand.class),
            ChangePhase::hand,
            ByteBufCodecs.BYTE,
            ChangePhase::index,
            ByteBufCodecs.BOOL,
            ChangePhase::merciless,
            ChangePhase::new
        );
        public static final IPayloadHandler<ChangePhase> HANDLER = ChangePhase::serverHandler;

        public ChangePhase(InteractionHand hand, int index, boolean merciless) {
            this(hand, (byte) index, merciless);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public void serverHandler(IPayloadContext context) {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> {
                var stackOp = Optional.of(player.getItemInHand(this.hand))
                    .filter(stack -> stack.has(ModComponents.MULTIPHASE));
                if (stackOp.isEmpty()) return;
                var stack = stackOp.get();
                var multiphase = stack.get(ModComponents.MULTIPHASE).toMultiphase();
                multiphase.cyclePhases(
                    stack,
                    (byte) ((this.index - multiphase.peekFirst().index() + multiphase.phases().size()) % multiphase.phases().size())
                );
                if (stack.has(ModComponents.MERCILESS)) stack.set(ModComponents.MERCILESS, new Merciless(this.merciless));
            });
        }
    }

    public record AllSync(Map<UUID, Multiphase> multiphases, Set<UUID> recoverableIds) implements CustomPacketPayload {
        public static final Type<AllSync> TYPE = MultiphasePackets.of("all_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, AllSync> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, Multiphase.STREAM_CODEC),
            AllSync::multiphases,
            ByteBufCodecs.collection(HashSet::new, UUIDUtil.STREAM_CODEC),
            AllSync::recoverableIds,
            AllSync::new
        );
        public static final IPayloadHandler<AllSync> HANDLER = AllSync::clientHandler;

        @Override
        public Type<AllSync> type() {
            return TYPE;
        }

        private void clientHandler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> Multiphases.get().sync(this.multiphases, this.recoverableIds));
        }
    }

    public record RecoverClear() implements CustomPacketPayload {
        public static final Type<RecoverClear> TYPE = MultiphasePackets.of("recover_clear");
        public static final StreamCodec<RegistryFriendlyByteBuf, RecoverClear> STREAM_CODEC = StreamCodec.unit(new RecoverClear());
        public static final IPayloadHandler<RecoverClear> HANDLER = RecoverClear::clientHandler;

        @Override
        public Type<RecoverClear> type() {
            return TYPE;
        }

        private void clientHandler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> Multiphases.get().clearRecover());
        }
    }

    public record RefSync(int index, Optional<UUID> id) implements CustomPacketPayload {
        public static final Type<RefSync> TYPE = MultiphasePackets.of("ref_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, RefSync> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            RefSync::index,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            RefSync::id,
            RefSync::new
        );
        public static final IPayloadHandler<RefSync> HANDLER = new DirectionalPayloadHandler<>(
            RefSync::clientHandler,
            RefSync::serverHandler
        );

        public RefSync(int index) {
            this(index, Optional.empty());
        }

        @Override
        public Type<RefSync> type() {
            return TYPE;
        }

        private void clientHandler(IPayloadContext ctx) {
            ctx.enqueueWork(
                () -> ctx.player()
                    .getInventory()
                    .getItem(this.index)
                    .set(ModComponents.MULTIPHASE, new MultiphaseRef(this.id().orElseThrow()))
            );
        }

        private void serverHandler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                var stack = ctx.player().getInventory().getItem(this.index);
                var ref = stack.get(ModComponents.MULTIPHASE);
                if (ref == null) {
                    return;
                } else if (ref.isEmpty()) {
                    ref = new MultiphaseRef(stack.getItem().getDescription());
                    stack.set(ModComponents.MULTIPHASE, ref);
                }
                PacketDistributor.sendToPlayer(Util.cast(ctx.player()), new RefSync(this.index, ref.id().optionalGet()));
            });
        }
    }
}
