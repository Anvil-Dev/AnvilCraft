package dev.dubhe.anvilcraft.init;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
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
        event.register(List.of(BlockTintSources.redstone()), ModBlocks.REDSTONE_WIRE.get());
    }

    @SubscribeEvent
    public static void registerItemColorHandlersEvent(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(PILL, Pill.MAP_CODEC);
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
            return MAP_CODEC;
        }
    }
}
