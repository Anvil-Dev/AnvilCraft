package dev.dubhe.anvilcraft.network.multiple;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.AdjacentSmithingMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 相邻容器锻造模板面板使用的网络包。 */
public final class SmithingTemplatePackets {
    private SmithingTemplatePackets() {
    }

    private static <T extends IPacket> Type<T> type(String path) {
        return IPacket.type(AnvilCraft.of("smithing_template_" + path));
    }

    /** 服务端向当前菜单同步可用模板和玩家置顶数据。 */
    public record Sync(
        int containerId,
        List<ItemStack> templates,
        List<ResourceLocation> favorites,
        ItemStack borrowedTemplate
    ) implements IClientboundPacket {
        public static final Type<Sync> TYPE = SmithingTemplatePackets.type("sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            Sync::containerId,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            Sync::templates,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()),
            Sync::favorites,
            ItemStack.OPTIONAL_STREAM_CODEC,
            Sync::borrowedTemplate,
            Sync::new
        );

        @Override
        public Type<Sync> type() {
            return TYPE;
        }

        @Override
        public void handleOnClient(Player player) {
            if (player.containerMenu.containerId != this.containerId) return;
            if (!(player.containerMenu instanceof AdjacentSmithingMenu menu)) return;
            menu.handleTemplateSync(this.templates, this.favorites, this.borrowedTemplate);
        }
    }

    /** 客户端请求借用模板，或切换模板的置顶状态。 */
    public record Action(int containerId, ResourceLocation template, boolean toggleFavorite)
        implements IServerboundPacket {
        public static final Type<Action> TYPE = SmithingTemplatePackets.type("action");
        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            Action::containerId,
            ResourceLocation.STREAM_CODEC,
            Action::template,
            ByteBufCodecs.BOOL,
            Action::toggleFavorite,
            Action::new
        );

        @Override
        public Type<Action> type() {
            return TYPE;
        }

        @Override
        public void handleOnServer(Player player) {
            if (player.containerMenu.containerId != this.containerId) return;
            if (!(player.containerMenu instanceof AdjacentSmithingMenu menu)) return;
            menu.handleTemplateAction(player, this.template, this.toggleFavorite);
        }
    }
}
