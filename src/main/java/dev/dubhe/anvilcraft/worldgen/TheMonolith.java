package dev.dubhe.anvilcraft.worldgen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Arrays;
import javax.annotation.Nullable;

/** Places one monolith near the shared spawn of the Overworld and Mun. */
public final class TheMonolith {
    public static final ResourceLocation TEMPLATE = AnvilCraft.of("the_monolith");
    public static final ResourceLocation SMALL_TEMPLATE = AnvilCraft.of("small_monolith");
    private static final int SMALL_SEARCH_RADIUS = 64;
    private static final int MAX_GROUND_HEIGHT_DIFFERENCE = 2;

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
        StructureTemplate template = level.getServer().getStructureManager().getOrCreate(giant ? TEMPLATE : SMALL_TEMPLATE);
        BlockPos surface = giant ? new BlockPos(x, CelestialTravelManager.findSurfaceY(level, x, z), z)
            : findSmallMonolithGround(level, template, new BlockPos(x, 0, z));
        if (surface == null) {
            AnvilCraft.LOGGER.warn("No open, dry monolith site found near the Overworld spawn {}", spawn);
            return null;
        }
        RandomSource random = level.getRandom();
        Placement placement = placement(template, surface);
        if (!giant && !prepareSmallMonolithGround(level, placement.boundingBox(), surface.getY())) return null;
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(placement.rotation());
        if (!template.placeInWorld(level, placement.corner(), placement.corner(), settings, random, 2 | 16)) return null;
        if (giant) {
            placeDevelopmentSign(level, placement.boundingBox());
        }
        return placement.boundingBox();
    }

    static @Nullable BlockPos findSmallMonolithGround(ServerLevel level, StructureTemplate template, BlockPos origin) {
        BlockPos fallback = null;
        int bestUnevenness = Integer.MAX_VALUE;
        BoundingBox footprint = placement(template, BlockPos.ZERO).boundingBox();
        for (int radius = 0; radius <= SMALL_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BoundingBox bounds = footprint.moved(origin.getX() + dx, 0, origin.getZ() + dz);
                    int[] heights = sampleSmallMonolithGround(level, bounds);
                    if (heights == null) continue;
                    // 邻区块完成树木装饰后再复查，避免稍后伸入场地的树冠。
                    for (int cx = (bounds.minX() - 17) >> 4; cx <= (bounds.maxX() + 17) >> 4; cx++) {
                        for (int cz = (bounds.minZ() - 17) >> 4; cz <= (bounds.maxZ() + 17) >> 4; cz++) {
                            level.getChunk(cx, cz);
                        }
                    }
                    heights = sampleSmallMonolithGround(level, bounds);
                    if (heights == null) continue;
                    int max = Arrays.stream(heights).max().orElseThrow();
                    int unevenness = Arrays.stream(heights).map(height -> max - height).sum();
                    BlockPos surface = origin.offset(dx, max, dz);
                    if (unevenness == 0) return surface;
                    if (unevenness >= bestUnevenness) continue;
                    bestUnevenness = unevenness;
                    fallback = surface;
                }
            }
        }
        return fallback;
    }

    static @Nullable int[] sampleSmallMonolithGround(ServerLevel level, BoundingBox bounds) {
        int[] heights = new int[(bounds.getXSpan() + 2) * (bounds.getZSpan() + 2)];
        int index = 0;
        int min = level.getMaxBuildHeight();
        int max = level.getMinBuildHeight();
        for (int x = bounds.minX() - 1; x <= bounds.maxX() + 1; x++) {
            for (int z = bounds.minZ() - 1; z <= bounds.maxZ() + 1; z++) {
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
                    x, level.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15), z
                );
                BlockState state = level.getBlockState(pos);
                while (state.isAir() || state.canBeReplaced()) {
                    if (!state.getFluidState().isEmpty() || state.is(BlockTags.LEAVES)
                        || pos.getY() <= level.getMinBuildHeight()) return null;
                    pos.move(0, -1, 0);
                    state = level.getBlockState(pos);
                }
                if (!isNaturalGround(state) || !state.getFluidState().isEmpty()) return null;
                heights[index++] = pos.getY();
                min = Math.min(min, pos.getY());
                max = Math.max(max, pos.getY());
                if (max - min > MAX_GROUND_HEIGHT_DIFFERENCE
                    || max + bounds.getYSpan() + 1 >= level.getMaxBuildHeight()) return null;
            }
        }
        return heights;
    }

    private static boolean isNaturalGround(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD)
            || state.is(BlockTags.TERRACOTTA) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)
            || state.is(Blocks.SNOW_BLOCK);
    }

    static boolean prepareSmallMonolithGround(ServerLevel level, BoundingBox bounds, int surfaceY) {
        int[] heights = sampleSmallMonolithGround(level, bounds);
        if (heights == null) return false;
        int index = 0;
        for (int x = bounds.minX() - 1; x <= bounds.maxX() + 1; x++) {
            for (int z = bounds.minZ() - 1; z <= bounds.maxZ() + 1; z++) {
                int groundY = heights[index++];
                BlockState ground = level.getBlockState(new BlockPos(x, groundY, z));
                if (ground.hasProperty(SnowyDirtBlock.SNOWY)) ground = ground.setValue(SnowyDirtBlock.SNOWY, false);
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = groundY; y <= Math.max(top, surfaceY + 1); y++) {
                    level.setBlock(new BlockPos(x, y, z), y <= surfaceY ? ground : Blocks.AIR.defaultBlockState(), 2 | 16);
                }
            }
        }
        return true;
    }

    private static void placeDevelopmentSign(ServerLevel level, BoundingBox monolith) {
        int x = monolith.minX() - 2;
        int z = monolith.getCenter().getZ();
        BlockPos pos = new BlockPos(x, CelestialTravelManager.findSurfaceY(level, x, z) + 1, z);
        level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 4), 3);
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return;
        SignText text = new SignText()
            .setMessage(1, Component.literal("W.I.P."))
            .setColor(DyeColor.WHITE)
            .setHasGlowingText(true);
        sign.setText(text, true);
        sign.setText(text, false);
    }

    /** Centers the rotated template above the surface without burying its core. */
    public static Placement placement(StructureTemplate template, BlockPos surfacePos) {
        Rotation rotation = Rotation.CLOCKWISE_90;
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
