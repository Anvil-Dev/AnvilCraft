package dev.dubhe.anvilcraft.block.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class CorruptedBeaconBlockEntity extends BlockEntity {
    List<BeaconBeamSection> beamSections = Lists.newArrayList();
    private List<BeaconBeamSection> checkingBeamSections = Lists.newArrayList();
    int levels;
    private int lastCheckY;

    public static @NotNull CorruptedBeaconBlockEntity createBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new CorruptedBeaconBlockEntity(type, pos, blockState);
    }

    public CorruptedBeaconBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CORRUPTED_BEACON.get(), pos, blockState);
    }

    private CorruptedBeaconBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * tick 逻辑
     */
    @SuppressWarnings("unused")
    public static void tick(
        Level pLevel,
        BlockPos pPos,
        BlockState pState,
        CorruptedBeaconBlockEntity pBlockEntity
    ) {
        int i = pPos.getX();
        int j = pPos.getY();
        int k = pPos.getZ();
        BlockPos blockpos;
        if (pBlockEntity.lastCheckY < j) {
            blockpos = pPos;
            pBlockEntity.checkingBeamSections = Lists.newArrayList();
            pBlockEntity.lastCheckY = pPos.getY() - 1;
        } else {
            blockpos = new BlockPos(i, pBlockEntity.lastCheckY + 1, k);
        }

        BeaconBeamSection beamSection = pBlockEntity.checkingBeamSections.isEmpty()
            ? null
            : pBlockEntity.checkingBeamSections.getLast();
        int l = pLevel.getHeight(Heightmap.Types.WORLD_SURFACE, i, k);

        for (int i1 = 0; i1 < 10 && blockpos.getY() <= l; i1++) {
            BlockState blockstate = pLevel.getBlockState(blockpos);
            Integer j1 = blockstate.getBeaconColorMultiplier(pLevel, blockpos, pPos);
            if (j1 != null) {
                if (pBlockEntity.checkingBeamSections.size() <= 1) {
                    beamSection = new BeaconBeamSection(j1);
                    pBlockEntity.checkingBeamSections.add(beamSection);
                } else if (beamSection != null) {
                    if (j1 == beamSection.color) {
                        beamSection.increaseHeight();
                    } else {
                        beamSection = new BeaconBeamSection(
                            FastColor.ARGB32.average(beamSection.color, j1)
                        );
                        pBlockEntity.checkingBeamSections.add(beamSection);
                    }
                }
            } else {
                if (beamSection == null || blockstate.getLightBlock(pLevel, blockpos) >= 15 && !blockstate.is(Blocks.BEDROCK)) {
                    pBlockEntity.checkingBeamSections.clear();
                    pBlockEntity.lastCheckY = l;
                    break;
                }

                beamSection.increaseHeight();
            }

            blockpos = blockpos.above();
            pBlockEntity.lastCheckY++;
        }

        int k1 = pBlockEntity.levels;
        if (pLevel.getGameTime() % 80L == 0L) {
            if (!pBlockEntity.beamSections.isEmpty()) {
                pBlockEntity.levels = updateBase(pLevel, i, j, k);
            }

            if (pBlockEntity.levels > 0 && !pBlockEntity.beamSections.isEmpty()) {
                playSound(pLevel, pPos, SoundEvents.BEACON_AMBIENT);
                CorruptedBeaconBlockEntity.affectEntities(pLevel, pPos);
            }
        }

        if (pBlockEntity.lastCheckY >= l) {
            pBlockEntity.lastCheckY = pLevel.getMinBuildHeight() - 1;
            boolean flag = k1 > 0;
            pBlockEntity.beamSections = pBlockEntity.checkingBeamSections;
            if (!pLevel.isClientSide) {
                boolean flag1 = pBlockEntity.levels > 0;
                if (!flag && flag1) {
                    playSound(pLevel, pPos, SoundEvents.BEACON_ACTIVATE);

                    for (ServerPlayer serverplayer : pLevel.getEntitiesOfClass(
                        ServerPlayer.class, new AABB(i, j, k, i, j - 4, k).inflate(10.0, 5.0, 10.0)
                    )) {
                        CriteriaTriggers.CONSTRUCT_BEACON.trigger(serverplayer, pBlockEntity.levels);
                    }
                } else if (flag && !flag1) {
                    playSound(pLevel, pPos, SoundEvents.BEACON_DEACTIVATE);
                }
            }
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

    private static void tryTransformEntity(
            Entity livingEntity,
            BlockPos pos,
            ServerLevel level,
            RecipeManager manager
    ) {

    }

    private static void affectEntities(@NotNull Level level, BlockPos pos) {
        if (level.isClientSide) return;
        AABB aabb = new AABB(pos).expandTowards(0.0, level.getHeight(), 0.0);
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, aabb);
        if (list.isEmpty()) return;
        RecipeManager manager = Objects.requireNonNull(level.getServer()).getRecipeManager();
        for (LivingEntity livingEntity : list) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 0, true, true));
            tryTransformEntity(livingEntity, pos, (ServerLevel) level, manager);
        }
    }

    public static void playSound(@NotNull Level level, BlockPos pos, SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    public List<BeaconBeamSection> getBeamSections() {
        return this.levels == 0 ? ImmutableList.of() : this.beamSections;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return this.saveWithoutMetadata(pRegistries);
    }

    @Override
    public void setLevel(@NotNull Level level) {
        super.setLevel(level);
        this.lastCheckY = level.getMinBuildHeight() - 1;
    }

    @Getter
    public static class BeaconBeamSection {
        final int color;
        private int height;

        public BeaconBeamSection(int pColor) {
            this.color = pColor;
            this.height = 1;
        }

        protected void increaseHeight() {
            this.height++;
        }

    }

    /**
     * 为了适配forge中修改的渲染逻辑所添加的函数
     * 返回一个无限碰撞箱
     *
     * @return forge中为原版信标生成的无限碰撞箱
     */
    @SuppressWarnings("unused")
    public AABB getRenderBoundingBox() {
        return new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }
}
