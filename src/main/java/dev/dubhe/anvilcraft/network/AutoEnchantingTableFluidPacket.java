package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.fluid.FluidHandlerWrapper;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
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
            || !(containerMenu instanceof AutoEnchantingTableMenu menu)
            || !menu.getBlockEntity().getBlockPos().equals(this.pos)) {
            return;
        }
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) return;
        FluidHandlerWrapper wrapper = new FluidHandlerWrapper(be.getFluidHandler());

        if (carried.is(Items.BUCKET)) {
            // 空桶 → 抽取罐内当前流体到桶（液态魔咒不可装桶时无事发生）
            ItemStack result = wrapper.drainToItem(carried, false);
            if (result != null && !result.isEmpty()) {
                this.swapCarried(menu, player, carried, result);
                this.update(level, be);
                level.playSound(null, this.pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS);
            }
            return;
        }

        // 流体桶等容器 → 将容器内流体倒入罐内（罐子不接受该流体时无事发生）
        ItemStack result = wrapper.fillFromItem(carried, false, player.getRandom());
        if (result != null) {
            this.swapCarried(menu, player, carried, result);
            this.update(level, be);
            level.playSound(null, this.pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS);
        }
    }

    private void update(Level level, AutoEnchantingTableBlockEntity be) {
        be.setChanged();
        level.sendBlockUpdated(this.pos, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL);
    }

    private void swapCarried(AutoEnchantingTableMenu menu, Player player, ItemStack carried, ItemStack replacement) {
        if (carried.getCount() == 1) {
            menu.setCarried(replacement);
        } else {
            // 多数容器：放回背包一个成品，减少原容器数量；背包满时原地掉落，避免成品丢失
            if (!player.addItem(replacement)) {
                player.getInventory().placeItemBackInInventory(replacement);
            }
            carried.setCount(carried.getCount() - 1);
            menu.setCarried(carried);
        }
    }
}
