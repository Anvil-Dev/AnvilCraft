package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.AdvancedComparatorBlock;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity;
import dev.dubhe.anvilcraft.inventory.AdvancedComparatorMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record AdvancedComparatorUpdatePacket(
    byte compareMode,
    boolean outputInvert,
    boolean redstoneControl,
    int highLimit,
    int lowLimit,
    int windowOutput
) implements IServerboundPacket {
    public static final Type<AdvancedComparatorUpdatePacket> TYPE = IPacket.type(AnvilCraft.of("advanced_comparator_update"));
    public static final StreamCodec<ByteBuf, AdvancedComparatorUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BYTE,
        AdvancedComparatorUpdatePacket::compareMode,
        ByteBufCodecs.BOOL,
        AdvancedComparatorUpdatePacket::outputInvert,
        ByteBufCodecs.BOOL,
        AdvancedComparatorUpdatePacket::redstoneControl,
        ByteBufCodecs.VAR_INT,
        AdvancedComparatorUpdatePacket::highLimit,
        ByteBufCodecs.VAR_INT,
        AdvancedComparatorUpdatePacket::lowLimit,
        ByteBufCodecs.VAR_INT,
        AdvancedComparatorUpdatePacket::windowOutput,
        AdvancedComparatorUpdatePacket::new
    );

    @Override
    public Type<AdvancedComparatorUpdatePacket> type() {
        return AdvancedComparatorUpdatePacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player.isSpectator() || this.compareMode < 0 || this.compareMode >= AdvancedComparatorBlockEntity.Mode.values().length) {
            return;
        }
        if (!(player.containerMenu instanceof AdvancedComparatorMenu menu) || !menu.stillValid(player)) {
            return;
        }
        AdvancedComparatorBlockEntity comparator = menu.getBlockEntity();
        // 服务端菜单保留打开时的值，只提交当前玩家改动的字段。
        if (this.compareMode != menu.getCompareMode().index()) {
            comparator.setCompareMode(AdvancedComparatorBlockEntity.Mode.fromIndex(this.compareMode));
        }
        if (this.outputInvert != menu.isOutputInvert()) {
            comparator.setOutputInvert(this.outputInvert);
        }
        if (this.redstoneControl != menu.isRedstoneControl()) {
            comparator.setRedstoneControl(this.redstoneControl);
        }
        if (this.highLimit != menu.getHighLimit()) {
            comparator.setHighLimit(this.highLimit);
        }
        if (this.lowLimit != menu.getLowLimit()) {
            comparator.setLowLimit(this.lowLimit);
        }
        if (this.windowOutput != menu.getWindowOutput()) {
            comparator.setWindowOutput(this.windowOutput);
        }
        comparator.setChanged();
        player.level().scheduleTick(comparator.getBlockPos(), comparator.getBlockState().getBlock(), AdvancedComparatorBlock.getDelay());
    }
}
