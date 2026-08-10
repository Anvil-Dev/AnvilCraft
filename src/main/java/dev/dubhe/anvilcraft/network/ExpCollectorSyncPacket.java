package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.ExpCollectorMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public record ExpCollectorSyncPacket(BlockPos pos) implements IServerboundPacket {
    public static final Type<ExpCollectorSyncPacket> TYPE = IPacket.type(AnvilCraft.of("exp_collector_sync"));
    public static final StreamCodec<ByteBuf, ExpCollectorSyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        ExpCollectorSyncPacket::pos,
        ExpCollectorSyncPacket::new
    );

    @Override
    public Type<ExpCollectorSyncPacket> type() {
        return ExpCollectorSyncPacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof ExpCollectorMenu menu)) return;
        ExpCollectorBlockEntity collector = menu.getBlockEntity();
        if (!collector.getBlockPos().equals(this.pos)) return;
        ItemStack carried = menu.getCarried();
        if (!carried.is(Items.BUCKET)) return;

        FluidResource experience = FluidResource.of(ModFluids.EXP_FLUID);
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = collector.getInternalFluidHandler().extract(experience, 1000, transaction);
            if (extracted != 1000) return;
            transaction.commit();
        }

        if (carried.getCount() == 1) {
            menu.setCarried(ModItems.EXP_BUCKET.asStack());
        } else {
            carried.shrink(1);
            menu.setCarried(carried);
            player.addItem(ModItems.EXP_BUCKET.asStack());
        }
        player.level().playSound(null, this.pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS);
    }
}
