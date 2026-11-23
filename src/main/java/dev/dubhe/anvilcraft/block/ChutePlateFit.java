package dev.dubhe.anvilcraft.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;

/** 槽沿占用的方向和宽度共同决定盖板的可见范围与碰撞面。 */
public final class ChutePlateFit {
    private static final Map<Direction, VoxelShape> PLATES = directions(face -> panel(face, 4));
    public static final ChutePlateFit UNCUT = new ChutePlateFit(null, 0);
    private static final ChutePlateFit REGULAR = new ChutePlateFit(Direction.UP, 4);
    private static final Map<Direction, ChutePlateFit> MAGNETIC = directions(inlet -> new ChutePlateFit(inlet, 8));

    private final @Nullable Direction inlet;
    private final int cut;
    private final Map<Direction, VoxelShape> panels;

    private ChutePlateFit(@Nullable Direction inlet, int cut) {
        this.inlet = inlet;
        this.cut = cut;
        if (inlet == null) {
            this.panels = PLATES;
        } else {
            VoxelShape rim = panel(inlet, cut);
            this.panels = directions(face -> Shapes.join(PLATES.get(face), rim, BooleanOp.ONLY_FIRST));
        }
    }

    private static <T> Map<Direction, T> directions(Function<Direction, T> value) {
        Map<Direction, T> result = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            result.put(direction, value.apply(direction));
        }
        return result;
    }

    private static VoxelShape panel(Direction face, int thickness) {
        return switch (face) {
            case NORTH -> Block.box(0, 0, 0, 16, 16, thickness);
            case SOUTH -> Block.box(0, 0, 16 - thickness, 16, 16, 16);
            case WEST -> Block.box(0, 0, 0, thickness, 16, 16);
            case EAST -> Block.box(16 - thickness, 0, 0, 16, 16, 16);
            case DOWN -> Block.box(0, 0, 0, 16, thickness, 16);
            case UP -> Block.box(0, 16 - thickness, 0, 16, 16, 16);
        };
    }

    public VoxelShape panel(Direction face) {
        return this.panels.get(face);
    }

    public static ChutePlateFit of(BlockState state) {
        if (state.getBlock() instanceof ChuteBlock) {
            return REGULAR;
        }
        if (state.getBlock() instanceof MagneticChuteBlock) {
            return MAGNETIC.get(state.getValue(MagneticChuteBlock.FACING).getOpposite());
        }
        return UNCUT;
    }

    public static List<ChutePlateFit> shortenedFits() {
        List<ChutePlateFit> fits = new ArrayList<>(MAGNETIC.values());
        fits.add(REGULAR);
        return List.copyOf(fits);
    }

    public @Nullable Direction inlet() {
        return this.inlet;
    }

    public int cut() {
        return this.cut;
    }
}
