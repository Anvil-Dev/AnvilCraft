package dev.dubhe.anvilcraft.network.multiple;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 超限锻造台的模板与结果选择网络包。 */
public final class TranscendenceSmithingPackets {
    private TranscendenceSmithingPackets() {
    }

    private static <T extends IPacket> Type<T> type(String path) {
        return IPacket.type(AnvilCraft.of("transcendence_smithing_" + path));
    }

    /** 服务端同步全部虚拟模板、玩家置顶数据与当前选择。 */
    public record Sync(
        int containerId,
        List<ItemStack> templates,
        List<Identifier> favorites,
        ItemStack selectedTemplate
    ) implements IClientboundPacket {
        public static final Type<Sync> TYPE = TranscendenceSmithingPackets.type("sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            Sync::containerId,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            Sync::templates,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()),
            Sync::favorites,
            ItemStack.OPTIONAL_STREAM_CODEC,
            Sync::selectedTemplate,
            Sync::new
        );

        @Override
        public Type<Sync> type() {
            return Sync.TYPE;
        }

        @Override
        public void handleOnClient(Player player) {
            if (player.containerMenu.containerId != this.containerId) return;
            if (!(player.containerMenu instanceof TranscendenceSmithingMenu menu)) return;
            menu.handleTemplateSync(this.templates, this.favorites, this.selectedTemplate);
        }
    }

    /** 客户端选择模板或切换置顶状态。 */
    public record TemplateAction(int containerId, Identifier template, boolean toggleFavorite)
        implements IServerboundPacket {
        public static final Type<TemplateAction> TYPE = TranscendenceSmithingPackets.type("template_action");
        public static final StreamCodec<ByteBuf, TemplateAction> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            TemplateAction::containerId,
            Identifier.STREAM_CODEC,
            TemplateAction::template,
            ByteBufCodecs.BOOL,
            TemplateAction::toggleFavorite,
            TemplateAction::new
        );

        @Override
        public Type<TemplateAction> type() {
            return TemplateAction.TYPE;
        }

        @Override
        public void handleOnServer(Player player) {
            if (player.containerMenu.containerId != this.containerId) return;
            if (!(player.containerMenu instanceof TranscendenceSmithingMenu menu)) return;
            menu.handleTemplateAction(player, this.template, this.toggleFavorite);
        }
    }

    /** 客户端切换浮霜模式的候选结果。 */
    public record TurnResult(int containerId, boolean left) implements IServerboundPacket {
        public static final Type<TurnResult> TYPE = TranscendenceSmithingPackets.type("turn_result");
        public static final StreamCodec<ByteBuf, TurnResult> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            TurnResult::containerId,
            ByteBufCodecs.BOOL,
            TurnResult::left,
            TurnResult::new
        );

        @Override
        public Type<TurnResult> type() {
            return TurnResult.TYPE;
        }

        @Override
        public void handleOnServer(Player player) {
            if (player.containerMenu.containerId != this.containerId) return;
            if (!(player.containerMenu instanceof TranscendenceSmithingMenu menu)) return;
            menu.turnFrostResult(this.left);
        }
    }
}
