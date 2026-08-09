package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.util.StructureSaveUtil;
import dev.dubhe.anvilcraft.util.WatchableCyclingValue;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Structure Scanner 统一网络包
 * Structure Scanner unified network packet.
 */
public record StructureScannerActionPacket(
    Action action,
    int value,
    Optional<String> name,
    RangeAxis rangeAxis
) implements IServerboundPacket {
    public static final Type<StructureScannerActionPacket> TYPE = IPacket.type(
        AnvilCraft.of("structure_scanner_action")
    );

    public static final StreamCodec<ByteBuf, StructureScannerActionPacket> STREAM_CODEC = StreamCodec.composite(
        StreamCodecUtil.enumStreamCodec(Action.class),
        StructureScannerActionPacket::action,
        ByteBufCodecs.INT,
        StructureScannerActionPacket::value,
        ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
        StructureScannerActionPacket::name,
        StreamCodecUtil.enumStreamCodec(RangeAxis.class),
        StructureScannerActionPacket::rangeAxis,
        StructureScannerActionPacket::new
    );

    public StructureScannerActionPacket(Action action) {
        this(action, 0, Optional.empty(), RangeAxis.NONE);
    }

    public StructureScannerActionPacket(Action action, @Nullable String name) {
        this(action, 0, Optional.ofNullable(name), RangeAxis.NONE);
    }

    public StructureScannerActionPacket(Action action, int value, RangeAxis rangeAxis) {
        this(action, value, Optional.empty(), rangeAxis);
    }

    public enum Action {
        START,
        STOP,
        RANGE_CHANGE,
        CONFIRM
    }

    public enum RangeAxis {
        NONE,
        X,
        Y,
        Z
    }

    @Override
    public Type<StructureScannerActionPacket> type() {
        return StructureScannerActionPacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof StructureScannerMenu menu)) {
            return;
        }
        StructureScannerBlockEntity blockEntity = menu.getBlockEntity();

        switch (this.action) {
            case START -> {
                blockEntity.startScanning();
                // 同步范围到客户端
                this.syncRangeToClient(player, blockEntity);
            }
            case STOP -> {
                blockEntity.stopScanning();
                // 同步范围到客户端
                this.syncRangeToClient(player, blockEntity);
            }
            case RANGE_CHANGE -> {
                boolean validRange = switch (this.rangeAxis) {
                    case X -> StructureScannerActionPacket.validateAndApplyRange(blockEntity.getRangeX(), this.value);
                    case Y -> StructureScannerActionPacket.validateAndApplyRange(blockEntity.getRangeY(), this.value);
                    case Z -> StructureScannerActionPacket.validateAndApplyRange(blockEntity.getRangeZ(), this.value);
                    case NONE -> false;
                };

                if (!validRange) {
                    AnvilCraft.LOGGER.warn(
                        "Player {} sent invalid range value: {} for {} (valid range: 0-{})",
                        player.getName().getString(),
                        this.value,
                        this.rangeAxis,
                        this.rangeAxis != RangeAxis.NONE ? StructureScannerActionPacket.getRangeCount(blockEntity, this.rangeAxis) - 1 : 0
                    );
                    return;
                }

                // 同步范围到客户端
                this.syncRangeToClient(player, blockEntity);
            }
            case CONFIRM -> {
                // 检查是否放入了结构磁盘
                if (blockEntity.isDiskEmpty()) {
                    player.sendSystemMessage(
                        Component.translatable(
                            "message.anvilcraft.structure_scanner.no_disk"
                        ).withStyle(ChatFormatting.RED)
                    );
                    return;
                }

                // 检查输出槽位是否为空
                if (blockEntity.hasOutput()) {
                    player.sendSystemMessage(
                        Component.translatable(
                            "message.anvilcraft.structure_scanner.output_not_empty"
                        ).withStyle(ChatFormatting.RED)
                    );
                    return;
                }

                // 保存结构文件(成功或失败都不会发送聊天消息,仅记录到服务器日志)
                String structureName = this.name.orElse("");
                if (structureName.isEmpty()) {
                    structureName = "structure_" + System.currentTimeMillis();
                }
                StructureSaveUtil.saveStructureToDisk(
                    player.level(), blockEntity, structureName
                );
            }
            default -> {}
        }
    }

    private void syncRangeToClient(Player player, StructureScannerBlockEntity blockEntity) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(
                serverPlayer,
                new StructureScannerRangeSyncPacket(
                    blockEntity.getRangeX().index(),
                    blockEntity.getRangeY().index(),
                    blockEntity.getRangeZ().index()
                )
            );
        }
    }

    /**
     * Validate and apply range value with bounds checking
     *
     * @param range The WatchableCyclingValue to update
     * @param value The index value from client
     * @return true if value was valid and applied, false otherwise
     */
    private static boolean validateAndApplyRange(
        WatchableCyclingValue<?> range,
        int value
    ) {
        // Bounds check: 0 <= value < count
        if (value < 0 || value >= range.count()) {
            return false;
        }

        range.fromIndex(value);
        return true;
    }

    /**
     * Get the count of valid values for a range axis
     */
    private static int getRangeCount(StructureScannerBlockEntity blockEntity, RangeAxis rangeAxis) {
        return switch (rangeAxis) {
            case X -> blockEntity.getRangeX().count();
            case Y -> blockEntity.getRangeY().count();
            case Z -> blockEntity.getRangeZ().count();
            default -> 0;
        };
    }
}
