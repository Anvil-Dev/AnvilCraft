package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.HammerChangeBlockEvent;
import dev.dubhe.anvilcraft.block.entity.RedstoneDiceBlockEntity;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record RedstoneDiceModePacket(BlockPos pos, boolean uniform) implements IServerboundPacket {
    public static final Type<RedstoneDiceModePacket> TYPE = IPacket.type(AnvilCraft.of("redstone_dice_mode"));
    public static final StreamCodec<ByteBuf, RedstoneDiceModePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, RedstoneDiceModePacket::pos,
        ByteBufCodecs.BOOL, RedstoneDiceModePacket::uniform,
        RedstoneDiceModePacket::new
    );

    @Override
    public Type<RedstoneDiceModePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        Level level = player.level();
        if (!level.hasChunkAt(this.pos) || !player.isAlive() || player.isSpectator()
            || !player.getAbilities().mayBuild || !player.canInteractWithBlock(this.pos, 0)
            || !level.mayInteract(player, this.pos)) return;
        if (!(player.getMainHandItem().getItem() instanceof AnvilHammerItem)
            && !(player.getOffhandItem().getItem() instanceof AnvilHammerItem)) return;
        if (!(level.getBlockEntity(this.pos) instanceof RedstoneDiceBlockEntity dice)) return;
        BlockState state = dice.getBlockState();
        if (!HammerChangeBlockEvent.invoke(level, player, this.pos, state, state, true)) return;
        dice.setUniform(this.uniform);
    }
}
