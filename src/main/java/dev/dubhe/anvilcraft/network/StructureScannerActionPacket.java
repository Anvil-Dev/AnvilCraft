package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Structure Scanner 统一网络包
 * 动作类型: "start" - 开始扫描
 *          "stop" - 停止扫描
 *          "rangeChange" - 范围变更（格式: "rangeX:5"）
 */
public record StructureScannerActionPacket(String action, int value, String name) implements IServerboundPacket {
    public static final Type<StructureScannerActionPacket> TYPE = IPacket.type(
        AnvilCraft.of("structure_scanner_action")
    );
    
    public static final StreamCodec<ByteBuf, StructureScannerActionPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        StructureScannerActionPacket::action,
        ByteBufCodecs.INT,
        StructureScannerActionPacket::value,
        ByteBufCodecs.STRING_UTF8,
        StructureScannerActionPacket::name,
        StructureScannerActionPacket::new
    );

    // 便捷构造函数
    public StructureScannerActionPacket(String action) {
        this(action, 0, "");
    }
    
    // 便捷构造函数（用于confirm动作）
    public StructureScannerActionPacket(String action, String name) {
        this(action, 0, name);
    }

    @Override
    public Type<StructureScannerActionPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof StructureScannerMenu menu)) {
            return;
        }
        StructureScannerBlockEntity blockEntity = menu.getBlockEntity();
        if (blockEntity == null) {
            return;
        }
        
        switch (action) {
            case "start" -> {
                blockEntity.startScanning();
                // 同步范围到客户端
                syncRangeToClient(player, blockEntity);
            }
            case "stop" -> {
                blockEntity.stopScanning();
                // 同步范围到客户端
                syncRangeToClient(player, blockEntity);
            }
            case "rangeChange" -> {
                // name 格式: "rangeX", "rangeY", "rangeZ"
                boolean validRange = switch (name) {
                    case "rangeX" -> validateAndApplyRange(blockEntity.getRangeX(), value);
                    case "rangeY" -> validateAndApplyRange(blockEntity.getRangeY(), value);
                    case "rangeZ" -> validateAndApplyRange(blockEntity.getRangeZ(), value);
                    default -> false;
                };
                
                if (!validRange) {
                    AnvilCraft.LOGGER.warn(
                        "Player {} sent invalid range value: {} for {} (valid range: 0-{})",
                        player.getName().getString(),
                        value,
                        name,
                        name.startsWith("range") ? getRangeCount(blockEntity, name) - 1 : 0
                    );
                    return;
                }
                
                // 同步范围到客户端
                syncRangeToClient(player, blockEntity);
            }
            case "confirm" -> {
                // 检查是否放入了结构磁盘
                if (blockEntity.getDiskInventory().getItem(0).isEmpty()) {
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable(
                            "message.anvilcraft.structure_scanner.no_disk"
                        ).withStyle(net.minecraft.ChatFormatting.RED)
                    );
                    return;
                }
                
                // 检查输出槽位是否为空
                if (!blockEntity.getOutputInventory().getItem(0).isEmpty()) {
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable(
                            "message.anvilcraft.structure_scanner.output_not_empty"
                        ).withStyle(net.minecraft.ChatFormatting.RED)
                    );
                    return;
                }
                
                // 保存结构文件(成功或失败都不会发送聊天消息,仅记录到服务器日志)
                String structureName = name.isEmpty() ? "structure_" + System.currentTimeMillis() : name;
                dev.dubhe.anvilcraft.util.StructureSaveUtil.saveStructureToDisk(
                    player.level(), blockEntity, structureName
                );
            }
            default -> {}
        }
    }
    
    private void syncRangeToClient(Player player, StructureScannerBlockEntity blockEntity) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new StructureScannerRangeSyncPacket(
                blockEntity.getRangeX().index(),
                blockEntity.getRangeY().index(),
                blockEntity.getRangeZ().index()
            ));
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
        dev.dubhe.anvilcraft.util.WatchableCyclingValue<?> range, 
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
     * Get the count of valid values for a range name
     */
    private static int getRangeCount(StructureScannerBlockEntity blockEntity, String name) {
        return switch (name) {
            case "rangeX" -> blockEntity.getRangeX().count();
            case "rangeY" -> blockEntity.getRangeY().count();
            case "rangeZ" -> blockEntity.getRangeZ().count();
            default -> 0;
        };
    }
}
