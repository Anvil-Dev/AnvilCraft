package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.function.Function;
import javax.annotation.Nullable;

@Mixin(value = BlockCache.class, remap = false)
abstract class BlockCacheMixin {
    @Shadow
    @Final
    private HashMap<BlockPos, BlockState> cache;

    @Shadow
    @Final
    private HashMap<BlockPos, BlockState> simulated;

    @Shadow
    @Final
    private HashMap<BlockPos, BlockEntity> cacheEntity;

    @Shadow
    @Final
    private HashMap<BlockPos, BlockEntity> simulatedEntity;

    @WrapOperation(
        method = {"getBlockState", "getBlockEntity"},
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/HashMap;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"
        )
    )
    private @Nullable Object anvilcraft$cacheMissingBlockEntity(
        HashMap<Object, Object> instance,
        Object key,
        Function<Object, Object> mappingFunction,
        Operation<Object> original
    ) {
        boolean cacheEntityMap = (Object) instance == this.cacheEntity;
        boolean simulatedEntityMap = (Object) instance == this.simulatedEntity;
        if (!cacheEntityMap && !simulatedEntityMap) {
            return original.call(instance, key, mappingFunction);
        }
        if (instance.containsKey(key)) return instance.get(key);
        if (simulatedEntityMap && this.cacheEntity.containsKey(key)) {
            Object value = this.cacheEntity.get(key);
            instance.put(key, value);
            return value;
        }
        BlockState state = this.anvilcraft$getKnownBlockState(cacheEntityMap, key);
        if (state != null && !state.hasBlockEntity()) {
            instance.put(key, null);
            return null;
        }
        Object value = original.call(instance, key, mappingFunction);
        if (value == null && state != null && !state.hasBlockEntity()) instance.put(key, null);
        return value;
    }

    @Unique
    private @Nullable BlockState anvilcraft$getKnownBlockState(boolean cacheEntityMap, Object key) {
        if (!(key instanceof BlockPos pos)) return null;
        if (cacheEntityMap) return this.cache.get(pos);
        BlockState state = this.simulated.get(pos);
        return state != null ? state : this.cache.get(pos);
    }
}
