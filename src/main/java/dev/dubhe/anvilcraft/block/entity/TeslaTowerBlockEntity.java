package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.taslatower.HasCustomNameFilter;
import dev.dubhe.anvilcraft.api.taslatower.IsEntityIdFilter;
import dev.dubhe.anvilcraft.api.taslatower.IsFriendlyFilter;
import dev.dubhe.anvilcraft.api.taslatower.IsOnVehicleFilter;
import dev.dubhe.anvilcraft.api.taslatower.IsPetFilter;
import dev.dubhe.anvilcraft.api.taslatower.IsPlayerIdFilter;
import dev.dubhe.anvilcraft.api.taslatower.TeslaFilter;
import dev.dubhe.anvilcraft.block.TeslaTowerBlock;
import dev.dubhe.anvilcraft.block.state.Vertical4PartHalf;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.TeslaTowerMenu;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
public class TeslaTowerBlockEntity extends BlockEntity
        implements IPowerConsumer, MenuProvider {
    private final Comparator<Entity> ENTITY_SORTER = new Comparator<>() {
        private final Vec3 blockPosVec = getBlockPos().getCenter();

        @Override
        public int compare(Entity entity, Entity t1) {
            double d1 = entity.position().distanceTo(blockPosVec);
            double d2 = t1.position().distanceTo(blockPosVec);
            if (d1 == d2)
                return 0;
            else return d1 < d2 ? -1 : 1;
        }
    };
    private final Comparator<BlockPos> BLOCK_SORTED = new Comparator<>() {
        private final Vec3 blockPosVec = getBlockPos().getCenter();

        @Override
        public int compare(BlockPos blockPos, BlockPos t1) {
            double d1 = blockPos.getCenter().distanceTo(blockPosVec);
            double d2 = t1.getCenter().distanceTo(blockPosVec);
            if (d1 == d2)
                return 0;
            else return d1 < d2 ? -1 : 1;
        }
    };
    private final ArrayList<Pair<TeslaFilter, String>> whiteList = new ArrayList<>();
    private int tickCount = 0;
    @Setter
    @Getter
    private PowerGrid grid;

    public TeslaTowerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.TESLA_TOWER.get(), pos, blockState);
    }

    private TeslaTowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static TeslaTowerBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new TeslaTowerBlockEntity(type, pos, blockState);
    }

    @Override
    public int getInputPower() {
        if (level == null) return 0;
        return level.getBlockState(getBlockPos()).getValue(TeslaTowerBlock.HALF) == Vertical4PartHalf.BOTTOM ?
                128 : 0;
    }

    @Override
    public Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public @NotNull BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        int index = 0;
        for (Pair<TeslaFilter, String> entry : whiteList) {
            tag.putString(entry.first().getId()+"_-_"+index, entry.second());
            index++;
        }
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        whiteList.clear();
        for (String key : tag.getAllKeys()) {
            if (key.split("_-_").length != 2) continue;
            String id = key.split("_-_")[0];
            whiteList.add(Pair.of(TeslaFilter.getFilter(id), tag.getString(key)));
        }
    }

    public void tick() {
        if (level == null) return;
        BlockState state = level.getBlockState(getBlockPos());
        if (!state.is(ModBlocks.TESLA_TOWER.get())) return;
        if (state.getValue(TeslaTowerBlock.HALF) != Vertical4PartHalf.BOTTOM) return;
        if (state.getValue(TeslaTowerBlock.SWITCH) == Switch.OFF && this.getGrid() != null) {
            this.getGrid().remove(this);
        } else if (state.getValue(TeslaTowerBlock.SWITCH) == Switch.ON && this.getGrid() == null) {
            PowerGrid.addComponent(this);
        }
        this.flushState(level, getBlockPos());
        this.flushState(level, getBlockPos().above(1));
        this.flushState(level, getBlockPos().above(2));
        this.flushState(level, getBlockPos().above(3));
        if (level.isClientSide) return;
        if (state.getValue(TeslaTowerBlock.OVERLOAD) || state.getValue(TeslaTowerBlock.SWITCH) == Switch.OFF) return;
        if (tickCount > 0) {
            tickCount--;
            return;
        }
        tickCount = 80;
        tickCount--;
        BlockPos pos = getBlockPos().above(3);
        BlockPos pos1 = pos.below(8).west(8).north(8);
        BlockPos pos2 = pos.above(8).east(8).south(8);
        AABB aabb = new AABB(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX() + 1, pos2.getY() + 1, pos2.getZ() + 1);
        Optional<LivingEntity> target = level.getEntitiesOfClass(LivingEntity.class, aabb)
                .stream()
                .filter(it -> whiteList.stream().noneMatch(it2 -> it2.left().match(it, it2.right()))).min(ENTITY_SORTER);
        Vec3 targetPos;
        if (target.isPresent()) {
            targetPos = target.get().position();
        } else {
            ArrayList<BlockPos> lightingRods = new ArrayList<>();
            BlockPos.betweenClosedStream(aabb)
                    .forEach(it -> {
                        if (level.getBlockState(it).is(Blocks.LIGHTNING_ROD))
                            lightingRods.add(it.above(0));
                    });
            Optional<BlockPos> targetBlock = lightingRods.stream().min(BLOCK_SORTED);
            if (targetBlock.isPresent())
                 targetPos = targetBlock.get().getCenter();
            else
                return;
        }
        LightningBolt lightningBolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        lightningBolt.setDamage(10f);
        lightningBolt.setPos(targetPos);
        level.addFreshEntity(lightningBolt);
    }

    public void initWhiteList(Player player) {
        whiteList.add(Pair.of(new IsPlayerIdFilter(), player.getName().getString()));
        whiteList.add(Pair.of(new IsPetFilter(), ""));
        whiteList.add(Pair.of(new HasCustomNameFilter(), ""));
        whiteList.add(Pair.of(new IsEntityIdFilter(), "entity.minecraft.villager"));
        whiteList.add(Pair.of(new IsEntityIdFilter(), "entity.minecraft.wandering_trader"));
        whiteList.add(Pair.of(new IsFriendlyFilter(), ""));
        whiteList.add(Pair.of(new IsOnVehicleFilter(), ""));
    }

    public void addFilter(String id, String arg) {
        if (level == null) return;
        BlockState blockState = level.getBlockState(getBlockPos());
        int yOffset = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (level.getBlockEntity(getBlockPos().above(-yOffset)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity)
            teslaTowerBlockEntity._addFilter(id, arg);
    }

    private void _addFilter(String id, String arg) {
        whiteList.add(Pair.of(TeslaFilter.getFilter(id), arg));
    }

    public void removeFilter(String id, String arg) {
        if (level == null) return;
        BlockState blockState = level.getBlockState(getBlockPos());
        int yOffset = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (level.getBlockEntity(getBlockPos().above(-yOffset)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity)
            teslaTowerBlockEntity._removeFilter(id, arg);
    }

    private void _removeFilter(String id, String arg) {
        whiteList.removeIf(pair -> pair.first().getId().equals(id) && pair.second().equals(arg));
    }

    public void handleSync(List<Pair<TeslaFilter, String>> filters) {
        if (level == null) return;
        BlockState blockState = level.getBlockState(getBlockPos());
        int yOffset = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (level.getBlockEntity(getBlockPos().above(-yOffset)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity)
            teslaTowerBlockEntity._handleSync(filters);
    }

    private void _handleSync(List<Pair<TeslaFilter, String>> filters) {
        whiteList.clear();
        whiteList.addAll(filters);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.anvilcraft.tesla_tower");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        if (level == null) return null;
        BlockState blockState = level.getBlockState(getBlockPos());
        int yOffset = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (level.getBlockEntity(getBlockPos().above(-yOffset)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity)
            return new TeslaTowerMenu(ModMenuTypes.TESLA_TOWER.get(), i, inventory, teslaTowerBlockEntity);
        return null;
    }

    public List<Pair<TeslaFilter, String>> getWhiteList() {
        if (level == null) return List.of();
        BlockState blockState = level.getBlockState(getBlockPos());
        int yOffset = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (level.getBlockEntity(getBlockPos().above(-yOffset)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity)
            return teslaTowerBlockEntity.whiteList;
        return List.of();
    }
}
