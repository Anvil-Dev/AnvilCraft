package dev.dubhe.anvilcraft.block.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformInput;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformRecipe;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformWithItemRecipe;
import lombok.Getter;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.FastColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CorruptedBeaconBlockEntity extends BlockEntity {
    List<BeaconBeamSection> beamSections = Lists.newArrayList();
    private List<BeaconBeamSection> checkingBeamSections = Lists.newArrayList();
    @Getter
    int levels;
    private int lastCheckY;

    public static CorruptedBeaconBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new CorruptedBeaconBlockEntity(type, pos, blockState);
    }

    public CorruptedBeaconBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CORRUPTED_BEACON.get(), pos, blockState);
    }

    private CorruptedBeaconBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 腐化信标方块实体的tick逻辑，用于处理信标光柱的渲染和效果应用
     *
     * @param level       方块所在的世界
     * @param pos         方块的位置
     * @param state       方块的状态
     * @param blockEntity 被tick的方块实体
     */
    @SuppressWarnings("unused")
    public static void tick(Level level, BlockPos pos, BlockState state, CorruptedBeaconBlockEntity blockEntity) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();
        BlockPos blockpos;
        // 初始化光柱检查位置
        if (blockEntity.lastCheckY < posY) {
            blockpos = pos;
            blockEntity.checkingBeamSections = Lists.newArrayList();
            blockEntity.lastCheckY = pos.getY() - 1;
        } else {
            blockpos = new BlockPos(posX, blockEntity.lastCheckY + 1, posZ);
        }

        // 获取当前正在检查的光柱段
        BeaconBeamSection beamSection = blockEntity.checkingBeamSections.isEmpty() ? null : blockEntity.checkingBeamSections.getLast();
        // 获取地表高度
        int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, posX, posZ);

        // 检查上方方块以构建光柱
        for (int i = 0; i < 10 && blockpos.getY() <= height; i++) {
            BlockState blockstate = level.getBlockState(blockpos);
            // 获取方块的信标颜色乘数
            Integer colorMultiplier = blockstate.getBeaconColorMultiplier(level, blockpos, pos);
            if (colorMultiplier != null) {
                // 如果当前检查的光柱段为空或只有一段，则创建新的光柱段
                if (blockEntity.checkingBeamSections.size() <= 1) {
                    beamSection = new BeaconBeamSection(0xDF101010);
                    blockEntity.checkingBeamSections.add(beamSection);
                } else if (beamSection != null) {
                    // 根据颜色是否相同决定是增加高度还是创建新的光柱段
                    if (colorMultiplier == beamSection.color) {
                        beamSection.increaseHeight();
                    } else {
                        beamSection = new BeaconBeamSection(FastColor.ARGB32.average(beamSection.color, colorMultiplier));
                        blockEntity.checkingBeamSections.add(beamSection);
                    }
                }
            } else {
                // 如果当前方块会阻挡光柱且不是基岩，则清空光柱段
                if (beamSection == null || blockstate.getLightBlock(level, blockpos) >= 15 && !blockstate.is(Blocks.BEDROCK)) {
                    blockEntity.checkingBeamSections.clear();
                    blockEntity.lastCheckY = height;
                    break;
                }

                beamSection.increaseHeight();
            }

            blockpos = blockpos.above();
            blockEntity.lastCheckY++;
        }

        // 保存当前信标等级
        int lastLevel = blockEntity.levels;

        // 每 80 tick 检查一次信标光柱状态是否正确
        if (level.getGameTime() % 80L == 0L) {
            if (!blockEntity.beamSections.isEmpty()) {
                blockEntity.levels = CorruptedBeaconBlockEntity.updateBase(level, posX, posY, posZ);
            }

            // 如果信标有效且有光柱，则播放音效并影响实体
            if (blockEntity.levels > 0 && !blockEntity.beamSections.isEmpty()) {
                CorruptedBeaconBlockEntity.playSound(level, pos, SoundEvents.BEACON_AMBIENT);
                CorruptedBeaconBlockEntity.affectEntities(level, pos);
            }
        }

        // 如果已完成光柱检查
        if (blockEntity.lastCheckY >= height) {
            blockEntity.lastCheckY = level.getMinBuildHeight() - 1;
            blockEntity.beamSections = blockEntity.checkingBeamSections;
            if (!level.isClientSide) {
                boolean lastHasLevel = lastLevel > 0;
                boolean shouldLit = blockEntity.levels > 0 && !blockEntity.beamSections.isEmpty();
                // 根据信标状态变化播放相应的音效和更新方块状态
                if (!lastHasLevel && shouldLit) {
                    CorruptedBeaconBlockEntity.setBeaconStatus(level, pos, state, blockEntity, true);
                } else if (lastHasLevel && !shouldLit) {
                    blockEntity.levels = 0;
                    CorruptedBeaconBlockEntity.setBeaconStatus(level, pos, state, blockEntity, false);
                }
            }
        }
    }

    public static void setBeaconStatus(Level level, BlockPos pos, BlockState state, CorruptedBeaconBlockEntity entity, boolean status) {
        level.setBlockAndUpdate(pos, state.setValue(CorruptedBeaconBlock.LIT, status));

        if (status) {
            CorruptedBeaconBlockEntity.playSound(level, pos, SoundEvents.BEACON_ACTIVATE);
            List<ServerPlayer> players = level.getEntitiesOfClass(
                ServerPlayer.class,
                new AABB(pos).inflate(0.0, -4.0, 0.0).inflate(10.0, 5.0, 10.0)
            );
            // 触发成就
            for (ServerPlayer serverplayer : players) {
                CriteriaTriggers.CONSTRUCT_BEACON.trigger(serverplayer, entity.levels);
            }
        } else {
            CorruptedBeaconBlockEntity.playSound(level, pos, SoundEvents.BEACON_DEACTIVATE);
        }
    }

    private static int updateBase(Level level, int x, int y, int z) {
        int k;
        int i = 0;
        int j = 1;
        while (j <= 4 && (k = y - j) >= level.getMinBuildHeight()) {
            boolean bl = true;
            block1:
            for (int l = x - j; l <= x + j && bl; ++l) {
                for (int m = z - j; m <= z + j; ++m) {
                    if (level.getBlockState(new BlockPos(l, k, m)).is(BlockTags.BEACON_BASE_BLOCKS)) continue;
                    bl = false;
                    continue block1;
                }
            }
            if (!bl) break;
            i = j++;
        }
        return i;
    }

    @Override
    public void setRemoved() {
        if (this.level == null) return;
        playSound(this.level, this.worldPosition, SoundEvents.BEACON_DEACTIVATE);
        super.setRemoved();
    }

    private static void tryTransformEntity(LivingEntity livingEntity, ServerLevel level, RecipeManager manager) {
        MobTransformInput input = new  MobTransformInput(livingEntity);
        Optional<RecipeHolder<MobTransformRecipe>> optionalRecipeHolder = manager.getRecipeFor(
            ModRecipeTypes.MOB_TRANSFORM_TYPE.get(), input, level);
        MobTransformWithItemRecipe.Input input2 = MobTransformWithItemRecipe.Input.of(livingEntity);
        Optional<RecipeHolder<MobTransformWithItemRecipe>> optionalRecipeHolder2 = manager.getRecipeFor(
            ModRecipeTypes.MOB_TRANSFORM_WITH_ITEM_TYPE.get(), input2, level);
        Entity result = null;
        boolean noItemFlag = true;
        if (optionalRecipeHolder2.isPresent()) {
            MobTransformWithItemRecipe recipe = optionalRecipeHolder2.get().value();
            result = recipe.apply(level.random, livingEntity, level);
            if (result != null) noItemFlag = false;
        }
        if (noItemFlag && optionalRecipeHolder.isPresent()) {
            MobTransformRecipe recipe = optionalRecipeHolder.get().value();
            result = recipe.apply(level.random, livingEntity, level);
        }
        if (result == null) return;
        Entity vehicle = null;
        if (livingEntity.isPassenger()) {
            vehicle = livingEntity.getVehicle();
        }
        livingEntity.discard();

        if (result instanceof ZombieHorse || result instanceof SkeletonHorse) {
            ((AbstractHorse) result).setTamed(true);
        }
        if (level.tryAddFreshEntityWithPassengers(result)) {
            if (vehicle != null) {
                result.startRiding(vehicle);
            }
        }
    }

    private static void affectEntities(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        AABB aabb = new AABB(pos).expandTowards(0.0, level.getHeight(), 0.0);
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, aabb);
        if (list.isEmpty()) return;
        RecipeManager manager = Objects.requireNonNull(level.getServer()).getRecipeManager();
        for (LivingEntity livingEntity : list) {
            if (!livingEntity.isAlive()) return;
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 0, true, true));
            tryTransformEntity(livingEntity, (ServerLevel) level, manager);
        }
    }

    public static void playSound(Level level, BlockPos pos, SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    public List<BeaconBeamSection> getBeamSections() {
        return this.levels == 0 ? ImmutableList.of() : this.beamSections;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        this.lastCheckY = level.getMinBuildHeight() - 1;
    }

    @Getter
    public static class BeaconBeamSection {
        final int color;
        private int height;

        public BeaconBeamSection(int color) {
            this.color = color;
            this.height = 1;
        }

        protected void increaseHeight() {
            this.height++;
        }
    }
}