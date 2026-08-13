package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.AutoEnchantingTableMenu;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public record AutoEnchantingTableFluidPacket(BlockPos pos) implements IServerboundPacket {
    public static final Type<AutoEnchantingTableFluidPacket> TYPE = IPacket.type(AnvilCraft.of("auto_enchanting_table_fluid"));
    public static final StreamCodec<ByteBuf, AutoEnchantingTableFluidPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        AutoEnchantingTableFluidPacket::pos,
        AutoEnchantingTableFluidPacket::new
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
        if (!(blockEntity instanceof AutoEnchantingTableBlockEntity be)
            || !(containerMenu instanceof AutoEnchantingTableMenu menu)) {
            return;
        }
        IFluidHandler handler = be.getFluidHandler();
        ItemStack carried = menu.getCarried();
        if (carried.is(Items.BUCKET)) {
            // 空桶 → 抽取 1000 mB 经验流体
            FluidStack drained = handler.drain(new FluidStack(ModFluids.EXP_FLUID, 1000), IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() >= 1000) {
                this.swapCarried(menu, player, carried, ModItems.EXP_BUCKET.asStack());
                be.setChanged();
                level.sendBlockUpdated(this.pos, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL);
                level.playSound(null, this.pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS);
            }
        } else if (carried.is(ModItems.EXP_BUCKET.get())) {
            // 经验流体桶 → 倒入 1000 mB
            int filled = handler.fill(new FluidStack(ModFluids.EXP_FLUID, 1000), IFluidHandler.FluidAction.EXECUTE);
            if (filled >= 1000) {
                this.swapCarried(menu, player, carried, new ItemStack(Items.BUCKET));
                be.setChanged();
                level.sendBlockUpdated(this.pos, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL);
                level.playSound(null, this.pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS);
            }
        }
    }

    private void swapCarried(AutoEnchantingTableMenu menu, Player player, ItemStack carried, ItemStack replacement) {
        if (carried.getCount() == 1) {
            menu.setCarried(replacement);
        } else {
            // 多数桶：放回背包一个成品，减少原桶数量
            player.addItem(replacement);
            carried.setCount(carried.getCount() - 1);
            menu.setCarried(carried);
        }
    }
}
