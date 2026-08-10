package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.workstation.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// 腐化信标方块实体。
/// 光束改为锻星砧风格的四棱锥暗色光束（见 corrupted-beacon-beam-rework）：
/// - `beamHeight` 记录光束顶端世界 Y（第一个阻挡方块处，无阻挡则为最高建筑高度），
///   由渐进扫描（每 tick 最多 10 格）持续更新，不做平滑插值；
/// - LIT 仅取决于基座等级（levels > 0），不再因上方有实心方块而熄灭。
public class CorruptedBeaconBlockEntity extends BlockEntity {
    @Getter
    int levels;
    /// 光束顶端的世界 Y 坐标（第一个阻挡方块处，若无阻挡则为最高建筑高度）
    @Getter
    private int beamHeight;
    /// 正在扫描中的光束高度
    private int checkingBeamHeight;
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

    // PLACEHOLDER_TICK

    /// 腐化信标方块实体的 tick 逻辑，用于处理信标光束的渲染高度扫描与效果应用。
    ///
    /// @param level       方块所在的世界
    /// @param pos         方块的位置
    /// @param state       方块的状态
    /// @param blockEntity 被 tick 的方块实体
    @SuppressWarnings("unused")
    public static void tick(Level level, BlockPos pos, BlockState state, CorruptedBeaconBlockEntity blockEntity) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();
        int maxY = level.getMaxY();

        // 初始化扫描：从信标上方第一个方块开始
        if (blockEntity.lastCheckY < posY) {
            blockEntity.lastCheckY = posY;
            blockEntity.checkingBeamHeight = maxY;
        }

        BlockPos checkPos = new BlockPos(posX, blockEntity.lastCheckY + 1, posZ);

        // 渐进扫描：每次最多检查 10 个方块，寻找第一个阻挡光束的实心方块
        for (int i = 0; i < 10 && checkPos.getY() <= maxY; i++) {
            BlockState blockstate = level.getBlockState(checkPos);
            // 判断是否为阻挡方块：没有信标颜色乘数、遮光值 >= 15、不是基岩
            if (blockstate.getBeaconColorMultiplier(level, checkPos, pos) == null
                && blockstate.getLightDampening() >= 15
                && !blockstate.is(Blocks.BEDROCK)) {
                // 找到阻挡方块，光束顶部即为该方块的 Y 坐标
                blockEntity.checkingBeamHeight = checkPos.getY();
                blockEntity.lastCheckY = maxY; // 结束本次扫描
                break;
            }
            checkPos = checkPos.above();
            blockEntity.lastCheckY++;
        }

        // 每 80 tick 检查一次：更新基座等级、同步 LIT 状态、应用效果
        if (level.getGameTime() % 80L == 0L) {
            int lastLevel = blockEntity.levels;
            blockEntity.levels = CorruptedBeaconBlockEntity.updateBase(level, posX, posY, posZ);

            if (!level.isClientSide()) {
                boolean shouldLit = blockEntity.levels > 0;
                boolean isCurrentlyLit = state.hasProperty(CorruptedBeaconBlock.LIT)
                    && state.getValue(CorruptedBeaconBlock.LIT);
                if (shouldLit && !isCurrentlyLit) {
                    CorruptedBeaconBlockEntity.setBeaconStatus(level, pos, state, blockEntity, true);
                } else if (lastLevel > 0 && !shouldLit) {
                    blockEntity.levels = 0;
                    CorruptedBeaconBlockEntity.setBeaconStatus(level, pos, state, blockEntity, false);
                }
            }

            // 信标有效时播放音效并影响实体
            if (blockEntity.levels > 0) {
                CorruptedBeaconBlockEntity.playSound(level, pos, SoundEvents.BEACON_AMBIENT);
                CorruptedBeaconBlockEntity.affectEntities(level, pos, blockEntity.checkingBeamHeight);
            }
        }

        // 扫描完成：更新最终光束高度并重启扫描
        if (blockEntity.lastCheckY >= maxY) {
            blockEntity.lastCheckY = level.getMinY() - 1;
            blockEntity.beamHeight = blockEntity.checkingBeamHeight;
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
        while (j <= 4 && (k = y - j) >= level.getMinY()) {
            boolean bl = true;
            block1:
            for (int l = x - j; l <= x + j && bl; ++l) {
                for (int m = z - j; m <= z + j; ++m) {
                    if (level.getBlockState(new BlockPos(l, k, m)).is(ModBlocks.CURSED_GOLD_BLOCK.get())) continue;
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
        CorruptedBeaconBlockEntity.playSound(this.level, this.worldPosition, SoundEvents.BEACON_DEACTIVATE);
        super.setRemoved();
    }

    private static void tryTransformEntity(LivingEntity livingEntity, ServerLevel level, RecipeManager manager) {
        MobTransformInput input = new MobTransformInput(livingEntity);
        Optional<RecipeHolder<MobTransformRecipe>> optionalRecipeHolder = manager.getRecipeFor(
            ModRecipeTypes.MOB_TRANSFORM.get(), input, level);
        MobTransformWithItemRecipe.Input input2 = MobTransformWithItemRecipe.Input.of(livingEntity);
        Optional<RecipeHolder<MobTransformWithItemRecipe>> optionalRecipeHolder2 = manager.getRecipeFor(
            ModRecipeTypes.MOB_TRANSFORM_WITH_ITEM.get(), input2, level);
        Entity result = null;
        boolean noItemFlag = true;
        if (optionalRecipeHolder2.isPresent()) {
            MobTransformWithItemRecipe recipe = optionalRecipeHolder2.get().value();
            result = recipe.apply(level.getRandom(), livingEntity, level);
            if (result != null) noItemFlag = false;
        }
        if (noItemFlag && optionalRecipeHolder.isPresent()) {
            MobTransformRecipe recipe = optionalRecipeHolder.get().value();
            result = recipe.apply(level.getRandom(), livingEntity, level);
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

    private static void affectEntities(Level level, BlockPos pos, int beamTopY) {
        if (level.isClientSide()) return;
        AABB aabb = new AABB(pos).expandTowards(0.0, beamTopY - pos.getY(), 0.0);
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, aabb);
        if (list.isEmpty()) return;
        RecipeManager manager = Objects.requireNonNull(level.getServer()).getRecipeManager();
        for (LivingEntity livingEntity : list) {
            if (!livingEntity.isAlive()) return;
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 0, true, true));
            CorruptedBeaconBlockEntity.tryTransformEntity(livingEntity, (ServerLevel) level, manager);
        }
    }

    public static void playSound(Level level, BlockPos pos, SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
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
        this.lastCheckY = level.getMinY() - 1;
    }
}
