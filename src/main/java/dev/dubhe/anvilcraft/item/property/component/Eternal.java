package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.InventoryUtil;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.List;
import java.util.function.Consumer;

public record Eternal() implements TooltipProvider {
    public static final Eternal DEFAULT = new Eternal();
    public static final MapCodec<Eternal> CODEC = MapCodec.unit(Eternal.DEFAULT);
    public static final StreamCodec<ByteBuf, Eternal> STREAM_CODEC = StreamCodec.unit(Eternal.DEFAULT);

    public static void tick(ServerPlayer player) {
        List<ItemStack> eternalItems = InventoryUtil.getItems(
            player.getInventory(),
            stack -> stack.has(ModComponents.ETERNAL)
        );

        for (ItemStack stack : eternalItems) {
            if (stack.has(DataComponents.DAMAGE)) {
                stack.set(DataComponents.DAMAGE, 0);
            }
            if (!stack.has(DataComponents.UNBREAKABLE)) {
                stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
            }
        }
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        consumer.accept(Component.translatable("tooltip.anvilcraft.property.eternal").withColor(0xD3C5F6));
    }
}
