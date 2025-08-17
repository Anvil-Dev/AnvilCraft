package dev.dubhe.anvilcraft.event.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.SugarBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class AnvilHitSugarBlockEventListener {
    @SubscribeEvent
    public static void onLand(AnvilFallOnLandEvent event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockPos hitPos = pos.below();
        BlockState hitState = level.getBlockState(hitPos);
        if (hitState.getBlock() instanceof SugarBlock sugarBlock) {
            sugarBlock.onHit(level, hitPos);
        }
    }
}
