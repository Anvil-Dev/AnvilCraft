package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.amulet.AmuletBoxItem;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.item.property.component.PillBoxContents;
import dev.dubhe.anvilcraft.item.utility.PillBoxItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 把滚轮选中的索引同步到服务端槽位。选中索引存放在物品组件里，而滚轮只改动客户端副本：
 * 生存模式下该改动不会回传服务端（创造模式由创造模式物品栏的槽位监听器顺带同步），
 * 于是服务端取出时读到的仍是旧索引，拿出的物品与界面高亮的不一致。
 * 只同步索引，不同步物品内容——否则生存模式客户端可凭空伪造盒内物品。
 */
public record BoxSelectionSyncPacket(int menuId, int slotIndex, int selection) implements IServerboundPacket {
    public static final Type<BoxSelectionSyncPacket> TYPE = IPacket.type(AnvilCraft.of("box_selection_sync"));
    public static final StreamCodec<ByteBuf, BoxSelectionSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        BoxSelectionSyncPacket::menuId,
        ByteBufCodecs.VAR_INT,
        BoxSelectionSyncPacket::slotIndex,
        ByteBufCodecs.VAR_INT,
        BoxSelectionSyncPacket::selection,
        BoxSelectionSyncPacket::new
    );

    @Override
    public Type<BoxSelectionSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        // isValidSlotIndex 会放行 -1/-999（那是点击包的语义），这里必须自行限定非负，
        // 否则 getSlot 会越界。
        if (player.containerMenu.containerId != this.menuId()) return;
        if (this.slotIndex() < 0 || this.slotIndex() >= player.containerMenu.slots.size()) return;
        Slot slot = player.containerMenu.getSlot(this.slotIndex());
        if (!slot.isActive() || !slot.allowModification(player)) return;
        ItemStack stack = slot.getItem();
        if (stack.getItem() instanceof PillBoxItem) {
            PillBoxContents.Mutable mutable = stack
                .getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY)
                .mutable();
            // -1 表示"未选择"（取出第一个），与 setIndex 的环绕语义不同，必须走 setDefaultIndex。
            if (this.selection() < 0) {
                mutable.setDefaultIndex();
            } else {
                mutable.setIndex(this.selection());
            }
            stack.set(ModComponents.PILL_BOX_CONTENTS, mutable.immutable());
        } else if (stack.getItem() instanceof AmuletBoxItem) {
            BoxContents contents = stack.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
            BoxContents.Mutable mutable = contents.mutable();
            // select 不校验取值，负值或越界会让 pop 以负下标删元素而抛异常，按客户端的范围夹取。
            mutable.select(Math.clamp(this.selection(), 0, Math.max(0, contents.getMaxSelection() - 1)));
            stack.set(ModComponents.BOX_CONTENTS, mutable.immutable());
        } else {
            return;
        }
        // 服务端每 tick 都会 broadcastChanges，把"服务端槽内容 != remoteSlots"的槽位回传给
        // 客户端。这里改了服务端槽内容，若不更新 remoteSlots 就会每 tick 回传，客户端槽内
        // ItemStack 被换成新实例，而选择器靠"同物品同组件"早退仍持有旧实例，后续滚轮写不回
        // 槽位，界面表现为选中框乱跳/闪烁/不渲染。客户端本地已是该值，直接标记为已同步。
        player.containerMenu.setRemoteSlot(this.slotIndex(), stack);
        slot.setChanged();
    }
}
