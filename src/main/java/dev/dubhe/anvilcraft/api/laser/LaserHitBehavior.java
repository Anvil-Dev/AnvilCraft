package dev.dubhe.anvilcraft.api.laser;

import dev.dubhe.anvilcraft.recipe.LaserHitRecipe;
import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class LaserHitBehavior implements ILaserComponent {
    @Nullable
    private LaserHitRecipe activeRecipe;
    @Nullable
    private BlockPos miningPos;
    @Nullable
    private BlockState miningState;
    @Nullable
    private BlockMiningEffect miningEffect;
    private int progress;
    private int lastMiningTick = -1;

    @Override
    public void onEmissionStopped(ILaserComponentOwner owner) {
        this.activeRecipe = null;
        this.miningPos = null;
        this.miningState = null;
        this.miningEffect = null;
        this.progress = 0;
        this.lastMiningTick = -1;
    }

    @Override
    public boolean onHitBlock(ILaserComponentOwner owner, Level level, BlockPos blockPos) {
        if (!(level instanceof ServerLevel serverLevel)) return true;
        int tick = owner.getLaserTicks();
        if (this.lastMiningTick == tick) return this.activeRecipe == null;
        BlockState state = level.getBlockState(blockPos);
        LaserMiningComponent mining = owner.getComponent(LaserComponentTypes.MINING);
        LaserHitRecipe.Input input = new LaserHitRecipe.Input(
            blockPos, state, LaserStrengthComponent.getStrength(owner),
            mining != null && mining.specialTargets(), LaserTypeComponent.isGamma(owner)
        );
        Optional<RecipeHolder<LaserHitRecipe>> match = LaserHitRecipe.find(serverLevel, input);
        if (match.isEmpty()) {
            this.onEmissionStopped(owner);
            return true;
        }
        LaserHitRecipe recipe = match.get().value();
        BlockMiningEffect effect = LaserMiningComponent.getEffect(owner);
        if (recipe != this.activeRecipe || !blockPos.equals(this.miningPos) || state != this.miningState
            || !effect.equals(this.miningEffect) || tick != this.lastMiningTick + 1) {
            this.progress = 0;
        }
        this.activeRecipe = recipe;
        this.miningPos = blockPos.immutable();
        this.miningState = state;
        this.miningEffect = effect;
        this.lastMiningTick = tick;
        this.progress++;
        if (this.progress < recipe.getHitTime()) return false;
        this.progress = 0;
        List<ItemStack> drops = recipe.createDrops(serverLevel, blockPos, effect);
        if (state != recipe.getResultBlock() && !level.setBlockAndUpdate(blockPos, recipe.getResultBlock())) return false;
        recipe.applyHeat(serverLevel, blockPos);
        owner.deliverLaserDrops(drops, blockPos);
        return false;
    }

    // 合并只比较配置，连续开采进度保留在当前光束的实例中。
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof LaserHitBehavior;
    }

    @Override
    public int hashCode() {
        return LaserHitBehavior.class.hashCode();
    }
}
