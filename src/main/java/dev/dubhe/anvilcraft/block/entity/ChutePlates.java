package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.InputRefusingItemHandlerWrapper;
import dev.dubhe.anvilcraft.block.ChuteBlock;
import dev.dubhe.anvilcraft.block.ChutePlateFit;
import dev.dubhe.anvilcraft.block.MagneticChuteBlock;
import dev.dubhe.anvilcraft.block.SimpleChuteBlock;
import dev.dubhe.anvilcraft.block.SimpleMagneticChuteBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/** 保存溜槽盖板及其分面物品接口，完整和简易形态共用同一套安装规则。 */
public final class ChutePlates {
    /** 让带菜单的完整溜槽和无菜单的简易溜槽共享盖板数据。 */
    public interface Holder extends IItemHandlerHolder {
        ChutePlates getChutePlates();

        default IItemHandler getPort(@Nullable Direction face) {
            return this.getChutePlates().port(face, this.getItemHandler());
        }
    }

    private final BlockEntity owner;

    private Set<Direction> plates = Set.of();
    private final Map<Direction, IItemHandler> ports = new HashMap<>();
    private VoxelShape cachedBody = Shapes.empty();
    private VoxelShape cachedShape = Shapes.empty();

    ChutePlates(BlockEntity owner) {
        this.owner = owner;
    }

    public static @Nullable ChutePlates at(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Holder holder ? holder.getChutePlates() : null;
    }

    private VoxelShape panel(Direction face) {
        return ChutePlateFit.of(this.owner.getBlockState()).panel(face);
    }

    public Set<Direction> getPlates() {
        return this.plates;
    }

    private boolean isPort(Direction face) {
        BlockState state = this.owner.getBlockState();
        Level level = this.owner.getLevel();
        return face == ChuteBlock.getFacing(state) || face == ChutePlateFit.of(state).inlet()
            || level != null && ChuteBlock.outputsToward(level.getBlockState(this.owner.getBlockPos().relative(face)), face.getOpposite());
    }

    public boolean installPlate(Direction face) {
        Level level = this.owner.getLevel();
        BlockPos pos = this.owner.getBlockPos();
        if (level == null || this.plates.size() >= 4 || this.plates.contains(face) || this.isPort(face)
            || !level.isUnobstructed(null, this.panel(face).move(pos.getX(), pos.getY(), pos.getZ()))) {
            return false;
        }
        if (!level.isClientSide()) {
            EnumSet<Direction> installed = EnumSet.of(face);
            installed.addAll(this.plates);
            this.updatePlates(installed);
        }
        return true;
    }

    public static boolean hasPlate(BlockGetter level, BlockPos pos, Direction face) {
        ChutePlates covers = at(level, pos);
        return covers != null && covers.plates.contains(face);
    }

    private IItemHandler port(@Nullable Direction face, IItemHandler inventory) {
        return this.ports.computeIfAbsent(face, side -> new InputRefusingItemHandlerWrapper(inventory) {
            // 物流元件可能缓存处理器。每次插入都检查当前盖板，无方向访问也不能绕过封闭面。
            private boolean acceptsInput() {
                return side == null ? ChutePlates.this.plates.isEmpty() : !ChutePlates.this.plates.contains(side);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return this.acceptsInput() && inventory.isItemValid(slot, stack);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return this.acceptsInput() ? inventory.insertItem(slot, stack, simulate) : stack;
            }
        });
    }

    public boolean intersectsPlate(VoxelShape shape) {
        for (Direction face : this.plates) {
            if (Shapes.joinIsNotEmpty(ChutePlateFit.UNCUT.panel(face), shape, BooleanOp.AND)) {
                return true;
            }
        }
        return false;
    }

    public InteractionResult takePlate(Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !player.getMainHandItem().isEmpty() || !player.mayBuild() || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        Direction closest = this.hitPlate(hit);
        if (closest == null) {
            return InteractionResult.PASS;
        }
        if (this.owner.getLevel() instanceof ServerLevel server) {
            this.removePlate(closest);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.HEAVY_IRON_PLATE));
            this.refreshConnections(server);
        }
        return InteractionResult.SUCCESS;
    }

    private void refreshConnections(ServerLevel server) {
        BlockPos pos = this.owner.getBlockPos();
        if (server.getBlockEntity(pos) != this.owner) {
            return;
        }
        BlockState state = server.getBlockState(pos);
        BlockState connected = Block.updateFromNeighbourShapes(state, server, pos);
        if (connected != state) {
            server.setBlockAndUpdate(pos, connected);
        }
        server.neighborChanged(pos, state.getBlock(), pos);
    }

    private @Nullable Direction hitPlate(BlockHitResult hit) {
        Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(this.owner.getBlockPos()));
        Vec3 centered = local.subtract(0.5, 0.5, 0.5);
        Direction normal = hit.getDirection();
        Direction closest = null;
        int closestRow = Integer.MAX_VALUE;
        for (Direction face : this.plates) {
            VoxelShape panel = this.panel(face);
            if (panel.isEmpty() || !panel.bounds().inflate(1e-5).contains(local)) {
                continue;
            }
            double depth = 0.5 - centered.dot(Vec3.atLowerCornerOf(face.getNormal()));
            if (face == normal && depth < 1e-5) {
                return face;
            }
            // 侧面的斜接按整像素分界。相同深度时，取局部西端保留对角像素的那块板。
            int row = Math.clamp((int) Math.floor(depth * 16), 0, 3);
            if (closest == null || row < closestRow || row == closestRow
                && face.getNormal().cross(normal.getNormal()).equals(closest.getNormal())) {
                closest = face;
                closestRow = row;
            }
        }
        return closest;
    }

    void clearPortPlates() {
        if (this.plates.isEmpty() || !(this.owner.getLevel() instanceof ServerLevel server)) {
            return;
        }
        BlockState state = this.owner.getBlockState();
        Direction inlet = ChutePlateFit.of(state).inlet();
        Direction outlet = ChuteBlock.getFacing(state);
        if (outlet != null && this.plates.contains(outlet) || inlet != null && this.plates.contains(inlet)) {
            EnumSet<Direction> remaining = EnumSet.copyOf(this.plates);
            remaining.removeIf(face -> face == inlet || face == outlet);
            int removed = this.plates.size() - remaining.size();
            this.updatePlates(remaining);
            Block.popResource(server, this.owner.getBlockPos(), new ItemStack(ModBlocks.HEAVY_IRON_PLATE, removed));
            this.refreshConnections(server);
        }
    }

    public static boolean isConversion(Block previous, Block next) {
        return previous instanceof ChuteBlock && next instanceof SimpleChuteBlock
            || previous instanceof SimpleChuteBlock && next instanceof ChuteBlock
            || previous instanceof MagneticChuteBlock && next instanceof SimpleMagneticChuteBlock
            || previous instanceof SimpleMagneticChuteBlock && next instanceof MagneticChuteBlock;
    }

    public void transferTo(Holder target) {
        if (this.plates.isEmpty()) {
            return;
        }
        ChutePlates next = target.getChutePlates();
        BlockState state = next.owner.getBlockState();
        Direction inlet = ChutePlateFit.of(state).inlet();
        Direction outlet = ChuteBlock.getFacing(state);
        EnumSet<Direction> retained = EnumSet.copyOf(this.plates);
        retained.removeIf(face -> face == inlet || face == outlet);
        int removed = this.plates.size() - retained.size();
        next.plates = Set.copyOf(retained);
        this.plates = Set.of();
        if (removed > 0 && this.owner.getLevel() instanceof ServerLevel server) {
            Block.popResource(server, this.owner.getBlockPos(), new ItemStack(ModBlocks.HEAVY_IRON_PLATE, removed));
        }
    }

    public void dropAfterFailedConversion() {
        if (!this.plates.isEmpty() && this.owner.getLevel() instanceof ServerLevel server) {
            int count = this.plates.size();
            this.plates = Set.of();
            Block.popResource(server, this.owner.getBlockPos(), new ItemStack(ModBlocks.HEAVY_IRON_PLATE, count));
        }
    }

    private void removePlate(Direction face) {
        EnumSet<Direction> remaining = EnumSet.copyOf(this.plates);
        remaining.remove(face);
        this.updatePlates(remaining);
    }

    private void updatePlates(Set<Direction> plates) {
        this.plates = Set.copyOf(plates);
        this.cachedBody = Shapes.empty();
        this.owner.setChanged();
        if (this.owner.getLevel() instanceof ServerLevel server) {
            BlockState state = this.owner.getBlockState();
            this.owner.invalidateCapabilities();
            server.sendBlockUpdated(this.owner.getBlockPos(), state, state, Block.UPDATE_ALL);
            state.updateNeighbourShapes(server, this.owner.getBlockPos(), Block.UPDATE_ALL);
            server.updateNeighborsAt(this.owner.getBlockPos(), state.getBlock());
        }
    }

    public static VoxelShape withPlates(BlockGetter level, BlockPos pos, VoxelShape body) {
        ChutePlates chute = at(level, pos);
        if (chute == null || chute.plates.isEmpty()) {
            return body;
        }
        if (body != chute.cachedBody) {
            VoxelShape shape = body;
            for (Direction face : chute.plates) {
                shape = Shapes.or(shape, chute.panel(face));
            }
            chute.cachedShape = shape;
            chute.cachedBody = body;
        }
        return chute.cachedShape;
    }

    public boolean blocksExplosionRay(Vec3 from, Vec3 to) {
        for (Direction face : this.plates) {
            // 槽沿和短板共同封闭这一面，整面阻挡爆炸射线。
            AABB plate = ChutePlateFit.UNCUT.panel(face).bounds().move(this.owner.getBlockPos());
            if (plate.contains(from) || plate.contains(to) || plate.clip(from, to).isPresent()) {
                return true;
            }
        }
        return false;
    }

    void save(CompoundTag output) {
        if (!this.plates.isEmpty()) {
            ListTag faces = new ListTag();
            this.plates.forEach(face -> faces.add(StringTag.valueOf(face.getSerializedName())));
            output.put("Plates", faces);
        }
    }

    void load(CompoundTag input) {
        ListTag faces = input.getList("Plates", Tag.TAG_STRING);
        EnumSet<Direction> installed = EnumSet.noneOf(Direction.class);
        for (int i = 0; i < faces.size(); i++) {
            Direction direction = Direction.byName(faces.getString(i));
            if (direction != null) {
                installed.add(direction);
            }
        }
        this.plates = Set.copyOf(installed);
        this.cachedBody = Shapes.empty();
    }

    public static List<ItemStack> withPlateDrops(List<ItemStack> drops, LootParams.Builder params) {
        BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof Holder chute && !chute.getChutePlates().plates.isEmpty()) {
            List<ItemStack> result = new ArrayList<>(drops);
            result.add(new ItemStack(ModBlocks.HEAVY_IRON_PLATE, chute.getChutePlates().plates.size()));
            return result;
        }
        return drops;
    }
}
