package dev.dubhe.anvilcraft.worldgen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;

/** Places one monolith near the shared spawn of the Overworld and Mun. */
public final class TheMonolith {
    public static final ResourceLocation TEMPLATE = AnvilCraft.of("the_monolith");
    public static final ResourceLocation SMALL_TEMPLATE = AnvilCraft.of("small_monolith");

    private TheMonolith() {
    }

    public static void ensureGenerated(ServerLevel level) {
        boolean giant = CelestialTravelManager.MUN_LEVEL.equals(level.dimension());
        if (!giant && (!Level.OVERWORLD.equals(level.dimension())
            || level.getChunkSource().getGenerator() instanceof FlatLevelSource)) {
            return;
        }
        State state = State.get(level);
        if (state.boundingBox != null) return;
        BoundingBox box = place(level, giant);
        if (box == null) return;
        state.setBoundingBox(box);
        AnvilCraft.LOGGER.info(
            "Monolith generated in {} at [{}, {}, {}] ~ [{}, {}, {}]",
            level.dimension().location(), box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()
        );
    }

    private static @Nullable BoundingBox place(ServerLevel level, boolean giant) {
        BlockPos spawn = level.getSharedSpawnPos();
        int x = spawn.getX() + (giant ? 32 : 16);
        int z = spawn.getZ();
        int surfaceY = CelestialTravelManager.findSurfaceY(level, x, z);
        StructureTemplate template = level.getServer().getStructureManager().getOrCreate(giant ? TEMPLATE : SMALL_TEMPLATE);
        RandomSource random = level.getRandom();
        Placement placement = placement(template, new BlockPos(x, surfaceY, z), random);
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(placement.rotation());
        if (!template.placeInWorld(level, placement.corner(), placement.corner(), settings, random, 2 | 16)) return null;
        return placement.boundingBox();
    }

    /** Centers the rotated template above the surface without burying its core. */
    public static Placement placement(StructureTemplate template, BlockPos surfacePos, RandomSource random) {
        Rotation rotation = Rotation.getRandom(random);
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        BoundingBox bounds = template.getBoundingBox(settings, BlockPos.ZERO);
        BlockPos corner = surfacePos.offset(
            -Math.floorDiv(bounds.minX() + bounds.maxX(), 2),
            1,
            -Math.floorDiv(bounds.minZ() + bounds.maxZ(), 2)
        );
        return new Placement(corner, rotation, template.getBoundingBox(settings, corner));
    }

    /** 一次碑体放置的参数。 */
    public record Placement(BlockPos corner, Rotation rotation, BoundingBox boundingBox) {
    }

    /** 石碑的持久化状态：全局唯一的碑体包围盒。 */
    public static class State extends SavedData {
        private static final String DATA_NAME = "anvilcraft_the_monolith";

        private @Nullable BoundingBox boundingBox;

        public static State get(ServerLevel level) {
            return level.getDataStorage()
                .computeIfAbsent(new Factory<>(State::new, State::load, null), DATA_NAME);
        }

        private static State load(CompoundTag tag, HolderLookup.Provider provider) {
            State state = new State();
            if (tag.contains("BoundingBox")) {
                int[] box = tag.getIntArray("BoundingBox");
                if (box.length == 6) {
                    state.boundingBox = new BoundingBox(box[0], box[1], box[2], box[3], box[4], box[5]);
                }
            }
            return state;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            if (this.boundingBox != null) {
                BoundingBox box = this.boundingBox;
                tag.putIntArray(
                    "BoundingBox",
                    new int[]{box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()}
                );
            }
            return tag;
        }

        private void setBoundingBox(BoundingBox boundingBox) {
            this.boundingBox = boundingBox;
            this.setDirty();
        }
    }
}
