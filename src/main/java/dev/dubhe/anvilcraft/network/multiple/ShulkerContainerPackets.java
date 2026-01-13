package dev.dubhe.anvilcraft.network.multiple;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sc.category.ICategory;
import dev.dubhe.anvilcraft.api.sc.category.store.Categories;
import dev.dubhe.anvilcraft.api.sc.item.ItemEntries;
import dev.dubhe.anvilcraft.api.sc.item.OrderPos;
import dev.dubhe.anvilcraft.api.sc.upgrade.Upgrades;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.inventory.ShulkerContainerMenu;
import dev.dubhe.anvilcraft.saved.sc.client.ClientSCStorages;
import dev.dubhe.anvilcraft.saved.sc.server.ServerSCStorages;
import dev.dubhe.anvilcraft.util.NetworkUtil;
import dev.dubhe.anvilcraft.util.Util;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ShulkerContainerPackets {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(
            EntriesSync.TYPE,
            EntriesSync.STREAM_CODEC,
            EntriesSync.HANDLER
        );
        registrar.playBidirectional(
            CategoriesSync.TYPE,
            CategoriesSync.STREAM_CODEC,
            CategoriesSync.HANDLER
        );
        registrar.playToClient(
            UpgradesSync.TYPE,
            UpgradesSync.STREAM_CODEC,
            UpgradesSync.HANDLER
        );
        registrar.playToServer(
            OrderSync.TYPE,
            OrderSync.STREAM_CODEC,
            OrderSync.HANDLER
        );
        registrar.playToServer(
            ScreenClose.TYPE,
            ScreenClose.STREAM_CODEC,
            ScreenClose.HANDLER
        );
        registrar.playToClient(
            StoragesIdSync.TYPE,
            StoragesIdSync.STREAM_CODEC,
            StoragesIdSync.HANDLER
        );
        registrar.playToClient(
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
        registrar.playBidirectional(
            TransferSync.TYPE,
            TransferSync.STREAM_CODEC,
            TransferSync.HANDLER
        );
        registrar.playToServer(
            UpgradeRequest.TYPE,
            UpgradeRequest.STREAM_CODEC,
            UpgradeRequest.HANDLER
        );
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> of(String path) {
        return new CustomPacketPayload.Type<>(AnvilCraft.of("sc_" + path));
    }

    public record EntriesSync(UUID storageId, ItemEntries entries) implements CustomPacketPayload {
        public static final Type<EntriesSync> TYPE = ShulkerContainerPackets.of("entries_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, EntriesSync> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            EntriesSync::storageId,
            ItemEntries.STREAM_CODEC,
            EntriesSync::entries,
            EntriesSync::new
        );
        public static final IPayloadHandler<EntriesSync> HANDLER = EntriesSync::clientHandler;

        @Override
        public Type<EntriesSync> type() {
            return TYPE;
        }

        private void clientHandler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> ClientSCStorages.getOrCreate(this.storageId).sync(this.entries));
        }
    }

    public record CategoriesSync(UUID storageId, Categories categories) implements CustomPacketPayload {
        public static final Type<CategoriesSync> TYPE = ShulkerContainerPackets.of("categories_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, CategoriesSync> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            CategoriesSync::storageId,
            Categories.STREAM_CODEC,
            CategoriesSync::categories,
            CategoriesSync::new
        );
        public static final IPayloadHandler<CategoriesSync> HANDLER = new DirectionalPayloadHandler<>(
            CategoriesSync::clientHandler,
            CategoriesSync::serverHandler
        );

        @Override
        public Type<CategoriesSync> type() {
            return TYPE;
        }

        private void clientHandler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> ClientSCStorages.getOrCreate(this.storageId).sync(this.categories));
        }

        private void serverHandler(IPayloadContext ctx) {
            ServerSCStorages.get().setDirty();
            ctx.enqueueWork(() -> ServerSCStorages.get().getOrCreate(this.storageId).getCategories().sync(this.categories));
        }
    }

    public record UpgradesSync(UUID storageId, Upgrades upgrades) implements CustomPacketPayload {
        public static final Type<UpgradesSync> TYPE = ShulkerContainerPackets.of("upgrades_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, UpgradesSync> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            UpgradesSync::storageId,
            Upgrades.STREAM_CODEC,
            UpgradesSync::upgrades,
            UpgradesSync::new
        );
        public static final IPayloadHandler<UpgradesSync> HANDLER = UpgradesSync::clientHandler;

        @Override
        public Type<UpgradesSync> type() {
            return TYPE;
        }

        private void clientHandler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> ClientSCStorages.getOrCreate(this.storageId).sync(this.upgrades));
        }
    }

    public record OrderSync(List<OrderPos> order) implements CustomPacketPayload {
        public static final Type<OrderSync> TYPE = ShulkerContainerPackets.of("order_sync");
        public static final StreamCodec<FriendlyByteBuf, OrderSync> STREAM_CODEC = StreamCodec.composite(
            OrderPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
            OrderSync::order,
            OrderSync::new
        );
        public static final IPayloadHandler<OrderSync> HANDLER = OrderSync::serverHandler;

        @Override
        public Type<OrderSync> type() {
            return TYPE;
        }

        private void serverHandler(IPayloadContext ctx) {
            if (!(ctx.player().containerMenu instanceof ShulkerContainerMenu menu)) return;
            ctx.enqueueWork(() -> menu.applyOrder(this.order));
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
                    .ifPresent(entity -> entity.someoneClosed(player))
            );
        }
    }

    public record StoragesIdSync(Set<UUID> storageIds, Set<UUID> recoverableIds) implements CustomPacketPayload {
        public static final Type<StoragesIdSync> TYPE = ShulkerContainerPackets.of("storages_id_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, StoragesIdSync> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(HashSet::new, UUIDUtil.STREAM_CODEC),
            StoragesIdSync::storageIds,
            ByteBufCodecs.collection(HashSet::new, UUIDUtil.STREAM_CODEC),
            StoragesIdSync::recoverableIds,
            StoragesIdSync::new
        );
        public static final IPayloadHandler<StoragesIdSync> HANDLER = StoragesIdSync::clientHandler;

        @Override
        public Type<StoragesIdSync> type() {
            return TYPE;
        }

        private void clientHandler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> ClientSCStorages.sync(this.storageIds, this.recoverableIds));
        }
    }

    public record IdSync(BlockPos pos, Optional<UUID> storageId) implements CustomPacketPayload {
        public static final Type<IdSync> TYPE = ShulkerContainerPackets.of("id_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, IdSync> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            IdSync::pos,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            IdSync::storageId,
            IdSync::new
        );
        public static final IPayloadHandler<IdSync> HANDLER = IdSync::clientHandler;

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
                this.storageId,
                () -> ctx.player().level().getBlockEntity(this.pos, ModBlockEntities.SHULKER_CONTAINER.get()),
                (id, be) -> be.setStorageId(id)
            ));
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
            ctx.enqueueWork(ClientSCStorages::clearRecover);
        }
    }

    public record CustomCategorySync(UUID storageId, ICategory custom, boolean add) implements CustomPacketPayload {
        public static final Type<CustomCategorySync> TYPE = ShulkerContainerPackets.of("custom_category_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, CustomCategorySync> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            CustomCategorySync::storageId,
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
                    var storageOp = ServerSCStorages.get().get(this.storageId);
                    if (storageOp.isEmpty()) return;
                    var storage = storageOp.get();
                    var categories = storage.getCategories();
                    if (this.add) {
                        categories.addCustom(this.custom);
                    } else {
                        categories.removeCustom(this.custom);
                    }
                    NetworkUtil.sendToAllPlayersExcluded(player.serverLevel(), player, new CategoriesSync(this.storageId, categories));
                    ServerSCStorages.get().setDirty();
                }
            );
        }
    }

    public record TransferSync(UUID storageId, Optional<UUID> owner, boolean share) implements CustomPacketPayload {
        public static final Type<TransferSync> TYPE = ShulkerContainerPackets.of("transfer_sync");
        public static final StreamCodec<ByteBuf, TransferSync> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            TransferSync::storageId,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            TransferSync::owner,
            ByteBufCodecs.BOOL,
            TransferSync::share,
            TransferSync::new
        );
        public static final IPayloadHandler<TransferSync> HANDLER = new DirectionalPayloadHandler<>(
            TransferSync::clientHandler,
            TransferSync::serverHandler
        );

        public TransferSync(UUID storageId, boolean share) {
            this(storageId, Optional.empty(), share);
        }

        public TransferSync(UUID storageId, UUID owner, boolean share) {
            this(storageId, Optional.of(owner), share);
        }

        @Override
        public Type<TransferSync> type() {
            return TYPE;
        }

        private void clientHandler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                var upgrades = ClientSCStorages.getOrCreate(this.storageId).getUpgrades();
                upgrades.setOwner(this.owner.orElseThrow());
                upgrades.setShare(this.share);
            });
        }

        private void serverHandler(IPayloadContext ctx) {
            UUID id = ctx.player().getGameProfile().getId();
            ctx.enqueueWork(() -> {
                var storageOp = ServerSCStorages.get().get(this.storageId);
                if (storageOp.isEmpty()) return;
                var upgrade = storageOp.get().getUpgrades();

                if (upgrade.getOwner() == null) {
                    upgrade.setOwner(id);
                    PacketDistributor.sendToAllPlayers(new TransferSync(this.storageId, id, false));
                    ServerSCStorages.get().setDirty();
                } else if (upgrade.getOwner().equals(id)) {
                    upgrade.setShare(this.share);
                    PacketDistributor.sendToAllPlayers(new TransferSync(this.storageId, id, this.share));
                    ServerSCStorages.get().setDirty();
                }
            });
        }
    }

    public record UpgradeRequest(int index) implements CustomPacketPayload {
        public static final Type<UpgradeRequest> TYPE = ShulkerContainerPackets.of("upgrade_request");
        public static final StreamCodec<ByteBuf, UpgradeRequest> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            UpgradeRequest::index,
            UpgradeRequest::new
        );
        public static final IPayloadHandler<UpgradeRequest> HANDLER = UpgradeRequest::serverHandler;

        @Override
        public Type<UpgradeRequest> type() {
            return TYPE;
        }

        private void serverHandler(IPayloadContext ctx) {
            ctx.enqueueWork(
                () -> Util.castSafely(ctx.player().containerMenu, ShulkerContainerMenu.class)
                    .ifPresent(menu -> menu.upgrade(this.index))
            );
        }
    }
}
