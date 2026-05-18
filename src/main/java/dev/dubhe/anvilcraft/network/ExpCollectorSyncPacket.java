package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.ExpCollectorMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public record ExpCollectorSyncPacket(BlockPos pos) implements IServerboundPacket {
    public static final Type<ExpCollectorSyncPacket> TYPE = IPacket.type(AnvilCraft.of("exp_collector_sync"));
    public static final StreamCodec<ByteBuf, ExpCollectorSyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        ExpCollectorSyncPacket::pos,
        ExpCollectorSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        Level level = player.level();
        AbstractContainerMenu containerMenu = player.containerMenu;
        BlockEntity blockEntity = level.getBlockEntity(this.pos);
        if (blockEntity instanceof ExpCollectorBlockEntity expCollectorBlockEntity
            && containerMenu instanceof ExpCollectorMenu expCollectorMenu) {
            ItemStack carried = expCollectorMenu.getCarried();
            if (carried.is(Items.BUCKET)) {
                IFluidHandler fluidHandler = expCollectorBlockEntity.getFluidHandler();
                FluidStack fluidStack = fluidHandler.getFluidInTank(0);
                if (fluidStack.getAmount() >= 1000) {
                    fluidHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                    expCollectorBlockEntity.setChanged();
                    level.sendBlockUpdated(this.pos, expCollectorBlockEntity.getBlockState(), expCollectorBlockEntity.getBlockState(), 3);
                    if (carried.getCount() == 1) {
                        expCollectorMenu.setCarried(ModItems.EXP_BUCKET.asStack());
                    } else if (carried.getCount() > 1) {
                        player.addItem(ModItems.EXP_BUCKET.asStack());
                        carried.setCount(carried.getCount() - 1);
                        expCollectorMenu.setCarried(carried);
                    }
                    level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS);
                }
            }
        }
    }
}
