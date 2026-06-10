package dev.dubhe.anvilcraft.item.property.component.amulet;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.util.ISerializer;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.function.Consumer;

/// 护符类
public interface IAmulet extends TooltipProvider {
    Codec<IAmulet> CODEC = ModRegistries.AMULET_TYPE.byNameCodec().dispatch(IAmulet::getType, IAmulet.Type::codec);
    StreamCodec<RegistryFriendlyByteBuf, IAmulet> STREAM_CODEC = ByteBufCodecs.registry(ModRegistryKeys.AMULET_TYPE)
        .dispatch(IAmulet::getType, IAmulet.Type::streamCodec);

    /// 在物品栏内时调用。<br>
    /// 用于执行护符效果。若需判断是否免疫伤害源，请参阅 {@link IAmulet#shouldImmune(ServerPlayer, DamageSource)}。
    ///
    /// @param player    玩家
    /// @param amulet    护符物品堆
    /// @param isEnabled 护符启用状态。{@code true} 为已启用，反之则为未启用
    /// @see IAmulet#shouldImmune(ServerPlayer, DamageSource)
    default void inventoryTick(ServerPlayer player, ItemStack amulet, boolean isEnabled) {
    }

    /// 根据给定数据判断是否免疫给定伤害源。<br>
    /// 用于判断是否免疫伤害源。若需执行护符效果，请参阅 {@link IAmulet#inventoryTick(ServerPlayer, ItemStack, boolean)}。
    ///
    /// @param player 玩家
    /// @param source 伤害源
    /// @return 是否免疫给定伤害源
    /// @see IAmulet#inventoryTick(ServerPlayer, ItemStack, boolean)
    default boolean shouldImmune(ServerPlayer player, DamageSource source) {
        return false;
    }

    /// 获取该护符在护符盒中所占据的重量
    ///
    /// @return 该护符在护符盒中所占据的重量
    default int getWeight() {
        return 6;
    }

    /// 判断该护符是否能作为给定的护符使用
    ///
    /// @param other 给定的护符
    /// @return 该护符是否能作为给定的护符使用
    default boolean canActAs(IAmulet other) {
        return this == other;
    }

    Type<? extends IAmulet> getType();

    @Override
    default void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
    }

    interface Type<T extends IAmulet> extends ISerializer<T> {
    }
}
