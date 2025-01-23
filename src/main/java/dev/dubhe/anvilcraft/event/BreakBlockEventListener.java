package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.item.HasMobBlockItem;
import dev.dubhe.anvilcraft.util.ModEnchantmentHelper;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber
public class BreakBlockEventListener {
    @SubscribeEvent
    public static void onBlockRemoved(BlockEvent.BreakEvent event) {
        LevelAccessor level = event.getLevel();
        Player player = event.getPlayer();
        if(!(player.level() instanceof ServerLevel serverLevel)) return;
        ItemStack stack = player.getMainHandItem();
        ModEnchantmentHelper.onPostBreakBlock(
            serverLevel,
            stack,
            player,
            EquipmentSlot.MAINHAND,
            event.getPos().getCenter(),
            event.getState()
        );
        if (level.getBlockState(event.getPos()).is(ModBlocks.MOB_AMBER_BLOCK) && !player.isCreative()) {
            event.setCanceled(true);
            BlockEntity entity = level.getBlockEntity(event.getPos());
            HasMobBlockItem.SavedEntity savedEntity = entity.components().filter((type) -> type == ModComponents.SAVED_ENTITY).get(ModComponents.SAVED_ENTITY);
            level.setBlock(event.getPos(), Blocks.AIR.defaultBlockState(), 3);
            level.playSound(player, event.getPos(), SoundType.METAL.getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
            ItemStack itemStack = new ItemStack(ModBlocks.MOB_AMBER_BLOCK.get());
            itemStack.set(ModComponents.SAVED_ENTITY, savedEntity);
            ItemEntity itemEntity = new ItemEntity((Level) level, event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), itemStack);
            level.addFreshEntity(itemEntity);
        }
    }
}
