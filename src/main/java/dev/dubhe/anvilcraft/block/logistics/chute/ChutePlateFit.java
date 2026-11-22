package dev.dubhe.anvilcraft.block.logistics.chute;

import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 槽沿占用的方向和宽度共同决定盖板的可见范围与碰撞面。 */
public final class ChutePlateFit {
    private static final Map<Direction, VoxelShape> PLATES = Shapes.rotateAll(Block.box(0, 0, 0, 16, 16, 4));
    public static final ChutePlateFit UNCUT = new ChutePlateFit(null, 0);
    private static final ChutePlateFit REGULAR = new ChutePlateFit(Direction.UP, 4);
    private static final Map<Direction, ChutePlateFit> MAGNETIC = Util.makeEnumMap(Direction.class, inlet -> new ChutePlateFit(inlet, 8));

    private final @Nullable Direction inlet;
    private final int cut;
    private final Map<Direction, VoxelShape> panels;

    private ChutePlateFit(@Nullable Direction inlet, int cut) {
        this.inlet = inlet;
        this.cut = cut;
        if (inlet == null) {
            this.panels = PLATES;
        } else {
            VoxelShape rim = Shapes.rotateAll(Block.box(0, 0, 0, 16, 16, cut)).get(inlet);
            this.panels = Util.makeEnumMap(Direction.class, face -> Shapes.join(PLATES.get(face), rim, BooleanOp.ONLY_FIRST));
        }
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

    public VoxelShape panel(Direction face) {
        return this.panels.get(face);
    }
}
