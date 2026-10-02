package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.building.BuildingRodService;
import dev.dubhe.anvilcraft.building.BuildingRodUndo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public record BuildingRodPacket(BlockPos first, BlockPos last, Direction face, boolean blueprint,
                                Rotation rotation, Mirror mirror, boolean partial,
                                @Nullable BlockHitResult hit, Action action, long seed) implements IServerboundPacket {
    public enum Action { PLACE, START, UNDO, PLACE_BLUEPRINTS }

    public BuildingRodPacket(BlockPos first, BlockPos last, Direction face, boolean blueprint,
                             Rotation rotation, Mirror mirror, boolean partial, @Nullable BlockHitResult hit, Action action) {
        this(first, last, face, blueprint, rotation, mirror, partial, hit, action, 0);
    }

    public BuildingRodPacket(BlockPos first, BlockPos last, Direction face, boolean blueprint,
                             Rotation rotation, Mirror mirror, boolean partial, @Nullable BlockHitResult hit) {
        this(first, last, face, blueprint, rotation, mirror, partial, hit, Action.PLACE);
    }

    public BuildingRodPacket(BlockPos first, BlockPos last, Direction face, boolean blueprint,
                             Rotation rotation, Mirror mirror, boolean partial) {
        this(first, last, face, blueprint, rotation, mirror, partial, null);
    }

    public static final Type<BuildingRodPacket> TYPE = IPacket.type(AnvilCraft.of("building_rod"));
    public static final StreamCodec<FriendlyByteBuf, BuildingRodPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeBlockPos(packet.first);
            buf.writeBlockPos(packet.last);
            buf.writeEnum(packet.face);
            buf.writeBoolean(packet.blueprint);
            buf.writeEnum(packet.rotation);
            buf.writeEnum(packet.mirror);
            buf.writeBoolean(packet.partial);
            buf.writeBoolean(packet.hit != null);
            if (packet.hit != null) buf.writeBlockHitResult(packet.hit);
            buf.writeEnum(packet.action);
            buf.writeLong(packet.seed);
        },
        buf -> new BuildingRodPacket(buf.readBlockPos(), buf.readBlockPos(), buf.readEnum(Direction.class),
            buf.readBoolean(), buf.readEnum(Rotation.class), buf.readEnum(Mirror.class), buf.readBoolean(),
            buf.readBoolean() ? buf.readBlockHitResult() : null, buf.readEnum(Action.class), buf.readLong())
    );

    @Override
    public Type<BuildingRodPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (this.action == Action.UNDO) {
            BuildingRodUndo.undo(serverPlayer);
            return;
        }
        if (this.action == Action.START) {
            BuildingRodService.start(serverPlayer, this.first, this.face, this.hit, this.seed);
            return;
        }
        if (this.action == Action.PLACE_BLUEPRINTS && this.blueprint) {
            BuildingRodService.blueprints(serverPlayer, this.first, this.last, this.rotation, this.mirror, this.partial);
        } else if (this.blueprint) {
            BuildingRodService.blueprint(serverPlayer, this.first, this.rotation, this.mirror, this.partial);
        } else {
            BuildingRodService.box(serverPlayer, this.first, this.last, this.face, this.hit, this.seed);
        }
    }
}
