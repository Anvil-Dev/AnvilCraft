package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class PlasmaJetClientChecks {
    private PlasmaJetClientChecks() {
    }

    public static void run(Minecraft client) {
        BlockPos base = new BlockPos(0, 68, 0);
        client.level.setBlock(base, Blocks.CAULDRON.defaultBlockState(), Block.UPDATE_CLIENTS);
        var jet = new PlasmaJetsBlockEntity(ModBlockEntities.PLASMA_JETS.get(), base.above(2), ModBlocks.PLASMA_JETS.getDefaultState());
        jet.setLevel(client.level);
        jet.getTubeWalls().add(PlasmaJetsBlockEntity.TubeWallLayer.of(base.above()));
        PlasmaJetsBlockEntity.tick(client.level, jet.getBlockPos(), jet.getBlockState(), jet);
        if (!jet.getParticleStartPos(client.level).equals(base.getCenter())) {
            throw new IllegalStateException("客户端空锅上的粒子起点丢失");
        }
        client.level.setBlock(base, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        AnvilCraft.LOGGER.info("PORT_PLASMA_CLIENT_PASSED");
    }
}
