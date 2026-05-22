package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/**
 * SmartBlockPlacer 统一网络包
 * 动作类型:
 * - "mode" - 切换拾取/移动模式 (value: 0=移动模式, 1=拾取模式)
 * - "layer" - 切换当前查看的层 (value: 0-4)
 * - "position" - 切换位置选择 (value: 0-24位置索引, name: "layer:position:selected" 格式)
 * - "missingMode" - 切换缺少方块处理模式 (value: 0=停止模式, 1=跳过模式)
 */
public record SmartBlockPlacerActionPacket(String action, int value, String name) implements IServerboundPacket {
    public static final Type<SmartBlockPlacerActionPacket> TYPE = IPacket.type(
        AnvilCraft.of("smart_block_placer_action")
    );
    
    public static final StreamCodec<ByteBuf, SmartBlockPlacerActionPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        SmartBlockPlacerActionPacket::action,
        ByteBufCodecs.INT,
        SmartBlockPlacerActionPacket::value,
        ByteBufCodecs.STRING_UTF8,
        SmartBlockPlacerActionPacket::name,
        SmartBlockPlacerActionPacket::new
    );

    // 便捷构造函数（用于只需要 value 的动作）
    public SmartBlockPlacerActionPacket(String action, int value) {
        this(action, value, "");
    }

    @Override
    public Type<SmartBlockPlacerActionPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof SmartBlockPlacerMenu menu)) {
            return;
        }
        SmartBlockPlacerBlockEntity blockEntity = menu.getBlockEntity();
        if (blockEntity == null) {
            return;
        }
        
        switch (action) {
            case "mode" -> {
                // value: 0=移动模式, 1=拾取模式
                boolean pickupMode = value == 1;
                blockEntity.setPickupMode(pickupMode);
            }
            case "layer" -> {
                // value: 0-4 层索引
                if (this.value < 0 || this.value > 4) {
                    AnvilCraft.LOGGER.warn(
                        "Player {} attempted to select invalid layer {} for SmartBlockPlacer at {}",
                        player.getName().getString(),
                        this.value,
                        blockEntity.getBlockPos()
                    );
                    return;
                }
                blockEntity.setSelectedLayer(this.value);
            }
            case "position" -> {
                // name 格式: "layer:position:selected"
                String[] parts = name.split(":");
                if (parts.length != 3) {
                    AnvilCraft.LOGGER.warn(
                        "Player {} sent malformed position data: {}",
                        player.getName().getString(),
                        name
                    );
                    return;
                }
                
                try {
                    int layer = Integer.parseInt(parts[0]);
                    int position = Integer.parseInt(parts[1]);
                    boolean selected = Boolean.parseBoolean(parts[2]);
                    
                    // 验证数据范围
                    if (layer < 0 || layer > 4) {
                        AnvilCraft.LOGGER.warn(
                            "Player {} attempted to set invalid layer {} for SmartBlockPlacer at {}",
                            player.getName().getString(),
                            layer,
                            blockEntity.getBlockPos()
                        );
                        return;
                    }
                    
                    if (position < 0 || position > 24) {
                        AnvilCraft.LOGGER.warn(
                            "Player {} attempted to set invalid position {} for SmartBlockPlacer at {}",
                            player.getName().getString(),
                            position,
                            blockEntity.getBlockPos()
                        );
                        return;
                    }
                    
                    blockEntity.togglePosition(layer, position, selected);
                } catch (NumberFormatException e) {
                    AnvilCraft.LOGGER.warn(
                        "Player {} sent non-numeric position data: {}",
                        player.getName().getString(),
                        name
                    );
                }
            }
            case "missingMode" -> {
                // value: 0=停止模式, 1=跳过模式
                boolean skipMissingMode = value == 1;
                blockEntity.setSkipMissingMode(skipMissingMode);
            }
            default -> AnvilCraft.LOGGER.warn(
                "Player {} sent unknown SmartBlockPlacer action: {}",
                player.getName().getString(),
                action
            );
        }
    }
}
