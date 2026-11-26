package dev.dubhe.anvilcraft.block.utility.redstone;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.math.OctahedralGroup;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.ButtonCap;
import dev.dubhe.anvilcraft.block.workstation.GiantAnvilBlock;
import dev.dubhe.anvilcraft.building.BlueprintMultiblocks;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.property.component.MountedButtonBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** 以安装物最底层的占地作为缩放基准，使渲染和交互使用同一个支点。 */
public final class ButtonMount {
    public static final double FLOOR = 4.05 / 16;
    private static final Map<Direction, VoxelShape> BASE = rotateFromUp(Block.box(2, 0, 2, 14, 4, 14));
    private static final VoxelShape TORCH = Block.column(2, 0, 10);
    private static final VoxelShape LIT_REDSTONE_TORCH = Shapes.or(TORCH, Block.column(3, 7.5, 10.5));
    private static final Map<Direction, VoxelShape> END_ROD = rotateFromUp(Shapes.or(Block.column(4, 0, 1), Block.column(2, 1, 16)));
    private static final Map<Direction, VoxelShape> LIGHTNING_ROD = rotateFromUp(
        Shapes.or(Block.column(2, 0, 12), Block.column(4, 12, 16))
    );
    private static final VoxelShape FLAT = Block.column(8, 0, 4);
    static final ButtonMount COLOR = new ButtonMount(Map.of(), FLAT, 8, false);
    private static final ButtonMount HAMMER = new ButtonMount(Map.of(), Shapes.or(
        Block.column(6, 0, 2), Block.column(3, 4, 2, 5), Block.column(5, 8, 5, 8)
    ), 6, false);
    private static final Cache<BlockState, ButtonMount> BLOCKS = CacheBuilder.newBuilder().maximumSize(256).build();
    private static final Cache<HolderLookup.Provider, Cache<MountedButtonBlock, ButtonMount>> MATERIALS =
        CacheBuilder.newBuilder().weakKeys().build();

    private final Map<BlockPos, BlockState> parts;
    private final Vec3 anchor;
    private final double scale;
    private final Map<Direction, Outline> raised;
    private final Map<Direction, Outline> pressed;

    private ButtonMount(Map<BlockPos, BlockState> parts, VoxelShape source, int footWidth, boolean squash) {
        this.parts = parts;
        double floor = source.min(Direction.Axis.Y);
        AABB foot = source.toAabbs().stream().filter(box -> box.minY <= floor + 1E-7)
            .reduce(AABB::minmax).orElseThrow();
        this.anchor = new Vec3((foot.minX + foot.maxX) / 2, floor, (foot.minZ + foot.maxZ) / 2);
        this.scale = (footWidth / 16d) / Math.max(foot.getXsize(), foot.getZsize());
        VoxelShape centered = source.move(this.anchor.scale(-1));
        VoxelShape up = place(centered, this.scale, this.scale);
        VoxelShape down = squash
            ? place(centered, this.scale * ButtonCap.PRESSED_WIDTH_SCALE, this.scale * ButtonCap.PRESSED_HEIGHT_SCALE)
            : up.move(0, -2d / 16, 0);
        this.raised = outlines(up);
        this.pressed = outlines(down);
    }

    public static ButtonMount forBlock(BlockState state) {
        return BLOCKS.asMap().computeIfAbsent(state, key -> build(key, null, RegistryAccess.EMPTY));
    }

    public static ButtonMount forMounted(MountedButtonBlock mounted, HolderLookup.Provider registries) {
        if (!mounted.state().hasBlockEntity() || mounted.material().components().isEmpty()) {
            return forBlock(mounted.state());
        }
        var cache = MATERIALS.asMap().computeIfAbsent(registries, key -> CacheBuilder.newBuilder().maximumSize(128).build());
        return cache.asMap().computeIfAbsent(mounted, key -> build(key.state(), key.material().create(), registries));
    }

    public static ButtonMount forHammer() {
        return HAMMER;
    }

    public Map<BlockPos, BlockState> parts() {
        return this.parts;
    }

    public Vec3 anchor() {
        return this.anchor;
    }

    public double scale() {
        return this.scale;
    }

    public AABB bounds() {
        return this.outline(Direction.UP, false).bounds();
    }

    public VoxelShape outline(Direction facing, boolean pressed) {
        return (pressed ? this.pressed : this.raised).get(facing).cap();
    }

    VoxelShape withBase(Direction facing, boolean pressed) {
        return (pressed ? this.pressed : this.raised).get(facing).button();
    }

    static VoxelShape base(Direction facing) {
        return BASE.get(facing);
    }

    private static ButtonMount build(BlockState state, @Nullable ItemStack material, HolderLookup.Provider registries) {
        Map<BlockPos, BlockState> cells = new LinkedHashMap<>();
        BlueprintMultiblocks.expectedParts(BlockPos.ZERO, state).forEach(part -> {
            BlockState piece = part.state();
            // 放置多部件电器会打开开关，底座应展示玩家为安装物选定的开关位置。
            if (piece.is(state.getBlock()) && state.hasProperty(IPowerComponent.SWITCH)) {
                piece = piece.setValue(IPowerComponent.SWITCH, state.getValue(IPowerComponent.SWITCH));
            }
            cells.put(part.pos(), piece);
        });
        Map<BlockPos, BlockState> parts = Map.copyOf(cells);
        if (state.is(ModBlocks.CHROMATIC_STONE)) {
            return new ButtonMount(parts, FLAT, 8, false);
        }
        View view = new View(parts, new HashMap<>(), material, registries);
        VoxelShape shape = Shapes.empty();
        for (var part : parts.entrySet()) {
            VoxelShape partShape = partShape(part.getValue(), view, part.getKey());
            shape = Shapes.joinUnoptimized(shape, partShape.move(part.getKey()), BooleanOp.OR);
        }
        if (shape.isEmpty()) {
            // 隐形光源等方块没有可选轮廓，仍需给安装物保留可卸下的点击区域。
            for (BlockPos pos : parts.keySet()) {
                shape = Shapes.joinUnoptimized(shape, Shapes.block().move(pos), BooleanOp.OR);
            }
        }
        return new ButtonMount(parts, shape.optimize(), 8, true);
    }

    private static VoxelShape partShape(BlockState state, BlockGetter view, BlockPos pos) {
        // 火把与杆件原本留有更宽的点击区域，安装时按模型里的杆脚和端头缩放。
        if (state.is(Blocks.TORCH) || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.COPPER_TORCH)) {
            return TORCH;
        }
        if (state.is(Blocks.REDSTONE_TORCH)) {
            return state.getValue(BlockStateProperties.LIT) ? LIT_REDSTONE_TORCH : TORCH;
        }
        if (state.is(Blocks.END_ROD)) {
            return END_ROD.get(state.getValue(BlockStateProperties.FACING));
        }
        if (state.getBlock() instanceof LightningRodBlock) {
            return LIGHTNING_ROD.get(state.getValue(BlockStateProperties.FACING));
        }
        if (state.getBlock() instanceof GiantAnvilBlock giant) {
            return giant.getModelHolderState(state) == state ? giantAnvil(giant, state) : Shapes.empty();
        }
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> multipart) {
            return multipart.getPartShape(state);
        }
        VoxelShape anvil;
        if (state.is(ModBlocks.ROYAL_ANVIL)) {
            anvil = Shapes.or(Block.column(12, 0, 4), Block.column(4, 4, 10), Block.box(5, 4, 4, 11, 10, 6),
                Block.box(5, 4, 10, 11, 10, 12), Block.column(10, 16, 10, 16));
        } else if (state.is(ModBlocks.EMBER_ANVIL) || state.is(ModBlocks.FROST_ANVIL) || state.is(ModBlocks.TRANSCENDENCE_ANVIL)) {
            // 三种发光铁砧的反向描边延伸到了实体砧身外侧。
            VoxelShape waist = state.is(ModBlocks.TRANSCENDENCE_ANVIL)
                ? Block.column(6, 4, 10) : Block.box(5.025, 3.025, 4.025, 10.975, 10.975, 11.975);
            anvil = Shapes.or(Block.box(2.025, 0.025, 2.025, 13.975, 3.975, 13.975), waist,
                Block.box(3.025, 10.025, 0.025, 12.975, 15.975, 15.975));
        } else {
            return state.getShape(view, pos);
        }
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis() == Direction.Axis.X
            ? Shapes.rotate(anvil, OctahedralGroup.BLOCK_ROT_Y_90) : anvil;
    }

    private static VoxelShape giantAnvil(GiantAnvilBlock giant, BlockState state) {
        // 斜撑沿用原方块的阶梯近似，砧面的内凹和十字支撑按模型单独保留。
        VoxelShape braces = Shapes.joinUnoptimized(giant.getMultiPartShape(state), Block.column(48, -3, 16), BooleanOp.AND);
        return Shapes.or(braces, Block.column(40, -16, -8), Block.column(30, -8, -3), Block.column(16, -3, 16),
            Block.column(16, 48, 12, 26), Block.column(48, 16, 12, 26),
            Block.column(48, 16, 22), Block.column(44, 22, 26), Block.column(48, 26, 32));
    }

    private static VoxelShape place(VoxelShape centered, double width, double height) {
        VoxelShape shape = Shapes.empty();
        for (AABB box : centered.toAabbs()) {
            shape = Shapes.joinUnoptimized(shape, Shapes.box(
                0.5 + box.minX * width, FLOOR + box.minY * height, 0.5 + box.minZ * width,
                0.5 + box.maxX * width, FLOOR + box.maxY * height, 0.5 + box.maxZ * width
            ), BooleanOp.OR);
        }
        return shape.optimize();
    }

    private static Map<Direction, VoxelShape> rotateFromUp(VoxelShape shape) {
        return Shapes.rotateAll(Shapes.rotate(shape, OctahedralGroup.BLOCK_ROT_X_90));
    }

    private static Map<Direction, Outline> outlines(VoxelShape up) {
        Map<Direction, Outline> result = new EnumMap<>(Direction.class);
        rotateFromUp(up).forEach((facing, cap) -> result.put(facing, new Outline(cap, Shapes.or(base(facing), cap))));
        return result;
    }

    private record Outline(VoxelShape cap, VoxelShape button) {
    }

    private record View(
        Map<BlockPos, BlockState> parts, Map<BlockPos, BlockEntity> entities, @Nullable ItemStack material, HolderLookup.Provider registries
    ) implements BlockGetter {
        @Override
        public BlockState getBlockState(BlockPos pos) {
            return this.parts.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return this.getBlockState(pos).getFluidState();
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return this.entities.computeIfAbsent(pos.immutable(), this::createEntity);
        }

        private @Nullable BlockEntity createEntity(BlockPos pos) {
            BlockState state = this.getBlockState(pos);
            if (!(state.getBlock() instanceof EntityBlock block)) {
                return null;
            }
            BlockEntity entity = block.newBlockEntity(pos, state);
            if (entity != null && this.material != null) {
                var data = this.material.get(DataComponents.BLOCK_ENTITY_DATA);
                if (data != null && data.type() == entity.getType()) {
                    data.loadInto(entity, this.registries);
                }
                entity.applyComponentsFromItemStack(this.material);
            }
            return entity;
        }

        @Override
        public int getMinY() {
            return DimensionType.MIN_Y;
        }

        @Override
        public int getHeight() {
            return DimensionType.Y_SIZE;
        }
    }
}
