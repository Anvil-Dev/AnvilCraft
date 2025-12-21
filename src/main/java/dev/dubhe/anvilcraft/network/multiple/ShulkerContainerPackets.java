package dev.dubhe.anvilcraft.network.multiple;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sc.category.ICategory;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.inventory.ShulkerContainerMenu;
import dev.dubhe.anvilcraft.saved.sc.ContainerStorage;
import dev.dubhe.anvilcraft.saved.sc.ContainerStorages;
import dev.dubhe.anvilcraft.util.NetworkUtil;
import dev.dubhe.anvilcraft.util.Util;
import it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ShulkerContainerPackets {
    public static void register(PayloadRegistrar registrar) {
        registrar.playBidirectional(
            StorageSync.TYPE,
            StorageSync.STREAM_CODEC,
            StorageSync.HANDLER
        );
        registrar.playToServer(
            ScreenSync.TYPE,
            ScreenSync.STREAM_CODEC,
            ScreenSync.HANDLER
        );
        registrar.playToServer(
            ScreenClose.TYPE,
            ScreenClose.STREAM_CODEC,
            ScreenClose.HANDLER
        );
        registrar.playToClient(
            StoragesSync.TYPE,
            StoragesSync.STREAM_CODEC,
            StoragesSync.HANDLER
        );
        registrar.playBidirectional(
            IdSync.TYPE,
            IdSync.STREAM_CODEC,
            IdSync.HANDLER
        );
        registrar.playToClient(
            RecoverClear.TYPE,
            RecoverClear.STREAM_CODEC,
            RecoverClear.HANDLER
        );
        registrar.playToServer(
            CustomCategorySync.TYPE,
            CustomCategorySync.STREAM_CODEC,
            CustomCategorySync.HANDLER
        );
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> of(String path) {
        return new CustomPacketPayload.Type<>(AnvilCraft.of("sc_" + path));
    }

    public record StorageSync(ContainerStorage storage) implements CustomPacketPayload {
        public static final Type<StorageSync> TYPE = ShulkerContainerPackets.of("storage_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, StorageSync> STREAM_CODEC = StreamCodec.composite(
            ContainerStorage.STREAM_CODEC,
            StorageSync::storage,
            StorageSync::new
        );
        public static final IPayloadHandler<StorageSync> HANDLER = StorageSync::bidirectionalHandler;

        @Override
        public Type<StorageSync> type() {
            return TYPE;
        }

        private void bidirectionalHandler(IPayloadContext ctx) {
            ContainerStorage storage = ContainerStorages.get().getOrCreate(this.storage.getId());
            ctx.enqueueWork(() -> storage.sync(this.storage));
        }
    }

    public record ScreenSync(Int2BooleanMap order, float scrollOffs) implements CustomPacketPayload {
        public static final Type<ScreenSync> TYPE = ShulkerContainerPackets.of("screen_sync");
        public static final StreamCodec<FriendlyByteBuf, ScreenSync> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(Int2BooleanArrayMap::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.BOOL),
            ScreenSync::order,
            ByteBufCodecs.FLOAT,
            ScreenSync::scrollOffs,
            ScreenSync::new
        );
        public static final IPayloadHandler<ScreenSync> HANDLER = ScreenSync::serverHandler;

        @Override
        public Type<ScreenSync> type() {
            return TYPE;
        }

        private void serverHandler(IPayloadContext ctx) {
            if (!(ctx.player().containerMenu instanceof ShulkerContainerMenu menu)) return;
            ctx.enqueueWork(() -> menu.applyOrder(this.order, this.scrollOffs));
        }
    }

    public record ScreenClose(BlockPos pos) implements CustomPacketPayload {
        public static final Type<ScreenClose> TYPE = ShulkerContainerPackets.of("screen_close");
        public static final StreamCodec<FriendlyByteBuf, ScreenClose> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ScreenClose::pos,
            ScreenClose::new
        );
        public static final IPayloadHandler<ScreenClose> HANDLER = ScreenClose::serverHandler;

        @Override
        public Type<ScreenClose> type() {
            return TYPE;
        }

        private void serverHandler(IPayloadContext ctx) {
            Player player = ctx.player();
            ctx.enqueueWork(
                () -> player.level()
                    .getBlockEntity(this.pos, ModBlockEntities.SHULKER_CONTAINER.get())
                    .ifPresent(entity -> entity.someoneClosedMenu(player))
            );
        }
    }

    public record StoragesSync(Set<UUID> ids, Set<UUID> recoverableIds) implements CustomPacketPayload {
        public static final Type<StoragesSync> TYPE = ShulkerContainerPackets.of("storages_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, StoragesSync> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(HashSet::new, UUIDUtil.STREAM_CODEC),
            StoragesSync::ids,
            ByteBufCodecs.collection(HashSet::new, UUIDUtil.STREAM_CODEC),
            StoragesSync::recoverableIds,
            StoragesSync::new
        );
        public static final IPayloadHandler<StoragesSync> HANDLER = StoragesSync::clientHandler;

        @Override
        public Type<StoragesSync> type() {
            return TYPE;
        }

        private void clientHandler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> ContainerStorages.get().syncFromServer(this.ids, this.recoverableIds));
        }
    }

    public record IdSync(BlockPos pos, Optional<UUID> id) implements CustomPacketPayload {
        public static final Type<IdSync> TYPE = ShulkerContainerPackets.of("id_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, IdSync> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            IdSync::pos,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            IdSync::id,
            IdSync::new
        );
        public static final IPayloadHandler<IdSync> HANDLER = new DirectionalPayloadHandler<>(IdSync::clientHandler, IdSync::serverHandler);

        /**
         * 客户端构造函数
         *
         * @param pos 方块实体位置
         */
        public IdSync(BlockPos pos) {
            this(pos, Optional.empty());
        }

        /**
         * 服务端构造函数
         *
         * @param pos 方块实体位置
         * @param id 存储ID
         */
        public IdSync(BlockPos pos, UUID id) {
            this(pos, Optional.of(id));
        }

        @Override
        public Type<IdSync> type() {
            return TYPE;
        }

        private void clientHandler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> Util.ifAllPresent(
                this.id,
                () -> ctx.player().level().getBlockEntity(this.pos, ModBlockEntities.SHULKER_CONTAINER.get()),
                (id, be) -> be.syncStorageId(id)
            ));
        }

        private void serverHandler(IPayloadContext ctx) {
            ServerLevel level = Util.cast(ctx.player().level());
            ctx.enqueueWork(
                () -> level.getBlockEntity(this.pos, ModBlockEntities.SHULKER_CONTAINER.get())
                    .ifPresent(be -> PacketDistributor.sendToPlayersTrackingChunk(
                        level,
                        new ChunkPos(this.pos),
                        new IdSync(this.pos, be.getStorageId())
                    ))
            );
        }
    }

    public record RecoverClear() implements CustomPacketPayload {
        public static final Type<RecoverClear> TYPE = ShulkerContainerPackets.of("recover_clear");
        public static final StreamCodec<RegistryFriendlyByteBuf, RecoverClear> STREAM_CODEC = StreamCodec.unit(new RecoverClear());
        public static final IPayloadHandler<RecoverClear> HANDLER = RecoverClear::clientHandler;

        @Override
        public Type<RecoverClear> type() {
            return TYPE;
        }

        private void clientHandler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> ContainerStorages.get().clearRecover());
        }
    }

    public record CustomCategorySync(UUID id, ICategory custom, boolean add) implements CustomPacketPayload {
        public static final Type<CustomCategorySync> TYPE = ShulkerContainerPackets.of("custom_category_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, CustomCategorySync> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            CustomCategorySync::id,
            ICategory.STREAM_CODEC,
            CustomCategorySync::custom,
            ByteBufCodecs.BOOL,
            CustomCategorySync::add,
            CustomCategorySync::new
        );
        public static final IPayloadHandler<CustomCategorySync> HANDLER = CustomCategorySync::serverHandler;

        @Override
        public Type<CustomCategorySync> type() {
            return TYPE;
        }

        private void serverHandler(IPayloadContext ctx) {
            ServerPlayer player = Util.cast(ctx.player());
            ctx.enqueueWork(
                () -> {
                    var storageOp = ContainerStorages.get().get(this.id);
                    if (storageOp.isEmpty()) return;
                    var storage = storageOp.get();
                    var categories = storage.getCategories();
                    if (this.add) {
                        categories.addCustom(this.custom);
                    } else {
                        categories.removeCustom(this.custom);
                    }
                    NetworkUtil.sendToAllPlayersExcluded(player.serverLevel(), player, new StorageSync(storage));
                }
            );
        }
    }
}
