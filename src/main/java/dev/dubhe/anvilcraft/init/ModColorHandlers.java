package dev.dubhe.anvilcraft.init;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.RedstoneWireClientPowerCache;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;

@EventBusSubscriber
public class ModColorHandlers {
    public static final Identifier PILL = AnvilCraft.of("pill");

    @SubscribeEvent
    public static void registerBlockColorHandlersEvent(RegisterColorHandlersEvent.BlockTintSources event) {
        // 无衰减导线不再把强度写进方块状态，颜色改由服务端同步的客户端缓存驱动
        event.register(List.of(new RedstoneWireTintSource()), ModBlocks.REDSTONE_WIRE.get());
    }

    @SubscribeEvent
    public static void registerItemColorHandlersEvent(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(ModColorHandlers.PILL, Pill.MAP_CODEC);
    }

    /// 复用原版红石粉的强度到颜色映射，功率取自 [RedstoneWireClientPowerCache]
    public static class RedstoneWireTintSource implements BlockTintSource {
        @Override
        public int color(BlockState state) {
            return RedStoneWireBlock.getColorForPower(0);
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return RedStoneWireBlock.getColorForPower(RedstoneWireClientPowerCache.getCurrent(pos));
        }
    }

    public static class Pill implements ItemTintSource {
        public static final MapCodec<? extends ItemTintSource> MAP_CODEC = MapCodec.unit(new Pill());

        @Override
        public int calculate(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
            PotionContents potionContents = itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (potionContents.potion().isEmpty()) {
                return -1;
            } else {
                return ARGB.opaque(potionContents.getColor());
            }
        }

        @Override
        public MapCodec<? extends ItemTintSource> type() {
            return Pill.MAP_CODEC;
        }
    }
}
