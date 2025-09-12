package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
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
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.TeslaTowerMenu;
import dev.dubhe.anvilcraft.util.DistanceComparator;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
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
import java.util.List;
import java.util.Optional;

@Slf4j
public class TeslaTowerBlockEntity extends BlockEntity
    implements IPowerConsumer, MenuProvider, IDiskCloneable {
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
    public @NotNull PowerComponentType getComponentType() {
        if (this.getLevel() == null) return PowerComponentType.INVALID;
        if (!this.getBlockState().is(ModBlocks.TESLA_TOWER.get())) return PowerComponentType.INVALID;
        if (this.getBlockState().getValue(TeslaTowerBlock.HALF) != Vertical4PartHalf.BOTTOM)
            return PowerComponentType.INVALID;
        return PowerComponentType.CONSUMER;
    }

    @Override
    public int getInputPower() {
        if (level == null) return 0;
        return level.getBlockState(getBlockPos()).getValue(TeslaTowerBlock.HALF) == Vertical4PartHalf.BOTTOM ? 128 : 0;
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
            tag.putString(entry.first().getId() + "_-_" + index, entry.second());
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
            .filter(it -> whiteList.stream().noneMatch(it2 -> it2.left().match(it, it2.right())))
            .min((e1, e2) -> new DistanceComparator(getBlockPos().getCenter()).compare(e1.position(), e2.position()));
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
            Optional<BlockPos> targetBlock = lightingRods.stream()
                .min((b1, b2) -> new DistanceComparator(getBlockPos().getCenter()).compare(b1.getCenter(), b2.getCenter()));
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
        whiteList.add(Pair.of(new IsEntityIdFilter(), Component.translatable("entity.minecraft.villager").getString()));
        whiteList.add(Pair.of(new IsEntityIdFilter(), Component.translatable("entity.minecraft.wandering_trader").getString()));
        whiteList.add(Pair.of(new IsFriendlyFilter(), ""));
        whiteList.add(Pair.of(new IsOnVehicleFilter(), ""));
    }

    public void addFilter(String id, String arg) {
        if (level == null) return;
        BlockState blockState = level.getBlockState(getBlockPos());
        int yOffset = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (level.getBlockEntity(getBlockPos().above(-yOffset)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity) {
            teslaTowerBlockEntity.whiteList.add(Pair.of(TeslaFilter.getFilter(id), arg));
        }
    }

    public void removeFilter(String id, String arg) {
        if (level == null) return;
        BlockState blockState = level.getBlockState(getBlockPos());
        int yOffset = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (level.getBlockEntity(getBlockPos().above(-yOffset)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity) {
            teslaTowerBlockEntity.whiteList.removeIf(pair -> pair.first().getId().equals(id) && pair.second().equals(arg));
        }
    }

    public void handleSync(List<Pair<TeslaFilter, String>> filters) {
        if (level == null) return;
        BlockState blockState = level.getBlockState(getBlockPos());
        int yOffset = blockState.getValue(TeslaTowerBlock.HALF).getOffsetY();
        if (level.getBlockEntity(getBlockPos().above(-yOffset)) instanceof TeslaTowerBlockEntity teslaTowerBlockEntity) {
            teslaTowerBlockEntity.whiteList.clear();
            teslaTowerBlockEntity.whiteList.addAll(filters);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.anvilcraft.tesla_tower");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        if (level == null || player.isSpectator()) return null;
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

    @Override
    public void storeDiskData(CompoundTag tag) {
        ListTag filters = new ListTag();
        for (var entry : this.whiteList) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("id", entry.first().getId());
            entryTag.putString("arg", entry.right());
            filters.add(entryTag);
        }
        tag.put("Filters", filters);
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        ArrayList<Pair<TeslaFilter, String>> filters = new ArrayList<>();
        for (Tag tag : data.getList("Filters", Tag.TAG_COMPOUND)) {
            if (!(tag instanceof CompoundTag filter)) continue;
            filters.add(Pair.of(TeslaFilter.getFilter(filter.getString("id")), filter.getString("arg")));
        }
        this.handleSync(filters);
    }
}
