package dev.dubhe.anvilcraft.worldgen;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.init.ModStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * 石碑结构：不参与自然生成（无 structure_set）；
 * 碑体在玩家首次登月时由代码放置，亦可通过 /place structure 手动放置。
 */
public class TheMonolithStructure extends Structure {
    public static final MapCodec<TheMonolithStructure> CODEC = simpleCodec(TheMonolithStructure::new);

    public TheMonolithStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        // 目标区块地形可能尚未生成，优先直接扫描方块获取地表
        int y = context.heightAccessor() instanceof ServerLevel level
            ? CelestialTravelManager.findSurfaceY(level, x, z)
            : context.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()
            );
        BlockPos surfacePos = new BlockPos(x, y, z);
        return Optional.of(
            new GenerationStub(
                surfacePos,
                builder -> builder.addPiece(
                    TheMonolithPiece.at(context.structureTemplateManager(), surfacePos, context.random())
                )
            )
        );
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.THE_MONOLITH.get();
    }
}
