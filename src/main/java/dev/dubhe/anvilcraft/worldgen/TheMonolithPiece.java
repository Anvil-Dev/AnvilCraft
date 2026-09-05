package dev.dubhe.anvilcraft.worldgen;

import dev.dubhe.anvilcraft.init.ModStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** 石碑结构片：以结构模板放置磨制深板岩碑体，放置范围按当前生成区块裁剪。 */
public class TheMonolithPiece extends StructurePiece {
    private final BlockPos corner;
    private final Rotation rotation;

    private TheMonolithPiece(BlockPos corner, Rotation rotation, BoundingBox boundingBox) {
        super(ModStructureTypes.THE_MONOLITH_PIECE.get(), 0, boundingBox);
        this.corner = corner;
        this.rotation = rotation;
    }

    public TheMonolithPiece(CompoundTag tag) {
        super(ModStructureTypes.THE_MONOLITH_PIECE.get(), tag);
        int[] corner = tag.getIntArray("Corner");
        this.corner = new BlockPos(corner[0], corner[1], corner[2]);
        this.rotation = Rotation.valueOf(tag.getString("Rotation"));
    }

    /** 以地表位置为碑体中心创建结构片。 */
    public static TheMonolithPiece at(StructureTemplateManager templates, BlockPos surfacePos, RandomSource random) {
        StructureTemplate template = templates.getOrCreate(TheMonolith.TEMPLATE);
        TheMonolith.Placement placement = TheMonolith.placement(template, surfacePos, random);
        return new TheMonolithPiece(placement.corner(), placement.rotation(), placement.boundingBox());
    }

    @Override
    public void postProcess(
        WorldGenLevel level,
        StructureManager structureManager,
        ChunkGenerator generator,
        RandomSource random,
        BoundingBox box,
        ChunkPos chunkPos,
        BlockPos pos
    ) {
        MinecraftServer server = level.getServer();
        if (server == null) return;
        StructureTemplate template = server.getStructureManager().getOrCreate(TheMonolith.TEMPLATE);
        StructurePlaceSettings settings = new StructurePlaceSettings()
            .setRotation(this.rotation)
            .setBoundingBox(box);
        template.placeInWorld(level, this.corner, this.corner, settings, random, 2 | 16);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putIntArray("Corner", new int[]{this.corner.getX(), this.corner.getY(), this.corner.getZ()});
        tag.putString("Rotation", this.rotation.name());
    }
}
