package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.anvil.BlockDevourerBehavior;
import dev.dubhe.anvilcraft.anvil.BlockPlacerBehavior;
import dev.dubhe.anvilcraft.anvil.CementStainingBehavior;
import dev.dubhe.anvilcraft.anvil.GunpowderBlockBehavior;
import dev.dubhe.anvilcraft.anvil.HitBeeNestBehavior;
import dev.dubhe.anvilcraft.anvil.HitCrabTrapBehavior;
import dev.dubhe.anvilcraft.anvil.HitSpawnerBehavior;
import dev.dubhe.anvilcraft.anvil.ImpactPileBehavior;
import dev.dubhe.anvilcraft.anvil.ItemStampingBehavior;
import dev.dubhe.anvilcraft.anvil.MassInjectBehavior;
import dev.dubhe.anvilcraft.anvil.RedstoneEMPBehavior;
import dev.dubhe.anvilcraft.anvil.ResetVaultBehavior;
import dev.dubhe.anvilcraft.anvil.SugarBlockBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilBehaviorRegisterEvent;
import dev.dubhe.anvilcraft.block.BlockDevourerBlock;
import dev.dubhe.anvilcraft.block.BlockPlacerBlock;
import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.GunpowderBlock;
import dev.dubhe.anvilcraft.block.SugarBlock;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ModAnvilBehaviors {
    @SubscribeEvent
    public static void register(@NotNull AnvilBehaviorRegisterEvent event) {
        event.registerBehavior(Blocks.REDSTONE_BLOCK, new RedstoneEMPBehavior());
        event.registerBehavior(
            state -> state.is(Blocks.BEEHIVE) || state.is(Blocks.BEE_NEST),
            new HitBeeNestBehavior()
        );
        event.registerBehavior(Blocks.SPAWNER, new HitSpawnerBehavior());
        event.registerBehavior(ModBlocks.CRAB_TRAP.get(), new HitCrabTrapBehavior());
        event.registerBehavior(state -> state.getBlock() instanceof CementCauldronBlock, new CementStainingBehavior());
        event.registerBehavior(ModBlocks.STAMPING_PLATFORM.get(), new ItemStampingBehavior());
        event.registerBehavior(ModBlocks.SPACE_OVERCOMPRESSOR.get(), new MassInjectBehavior());
        event.registerBehavior(state -> state.is(ModBlockTags.STORAGE_BLOCKS_LEAD), new ResetVaultBehavior());
        event.registerBehavior(
            state -> state.getBlock() instanceof BlockDevourerBlock && !state.getValue(BlockDevourerBlock.TRIGGERED),
            new BlockDevourerBehavior()
        );
        event.registerBehavior(state -> state.getBlock() instanceof BlockPlacerBlock, new BlockPlacerBehavior());
        event.registerBehavior(state -> state.getBlock() instanceof GunpowderBlock, new GunpowderBlockBehavior());
        event.registerBehavior(state -> state.is(ModBlocks.IMPACT_PILE), new ImpactPileBehavior());
        event.registerBehavior(state -> state.getBlock() instanceof SugarBlock, new SugarBlockBehavior());
    }
}
