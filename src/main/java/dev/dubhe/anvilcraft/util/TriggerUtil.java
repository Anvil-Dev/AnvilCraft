package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TriggerUtil {
    public static void placerPlaceBlock(Level level, BlockPos pos, Block block) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.PLACER_PLACE_BLOCK.get().trigger(player, block);
            }
        }
    }

    public static void devourerDevourBlock(Level level, BlockPos pos, Block block) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.DEVOURER_DEVOUR_BLOCK.get().trigger(player, block);
            }
        }
    }

    public static void anvilLooting(Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.ANVIL_LOOTING.get().trigger(player, entity);
            }
        }
    }

    public static void liftingAnvil(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.LIFTING_ANVIL.get().trigger(player);
            }
        }
    }

    public static void anvilOnGround(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.ANVIL_ON_GROUND.get().trigger(player);
            }
        }
    }

    public static void recipe(Level level, BlockPos pos, ResourceLocation id, List<ItemStack> stacks) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                CriteriaTriggers.RECIPE_CRAFTED.trigger(player, id, stacks);
            }
        }
    }

    public static void inWorldRecipe(Level level, BlockPos pos, ResourceLocation recipeType, ResourceLocation id) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.IN_WORLD_RECIPE.get().trigger(player, recipeType, id);
            }
        }
    }

    public static void anvilHammerClickBlock(Level level, BlockPos pos, String type) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.ANVIL_HAMMER_CLICK_BLOCK.get().trigger(player, type);
            }
        }
    }

    public static void anvilHammerHurtEntity(Level level, BlockPos pos, float damage) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.ANVIL_HAMMER_HURT_ENTITY.get().trigger(player, damage);
            }
        }
    }

    public static void killedEntityByAnvilHammer(Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.PLAYER_KILLED_ENTITY_BY_ANVIL_HAMMER.get().trigger(player, entity);
            }
        }
    }

    public static void anvilHitPiezoelectricCrystal(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.ANVIL_HIT_PIEZOELECTRIC_CRYSTAL.get().trigger(player);
            }
        }
    }

    public static void playerWearAnvilHammer(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.PLAYER_WEAR_ANVIL_HAMMER.get().trigger(player);
            }
        }
    }

    public static void convertBeacon(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.CONVERT_BEACON.get().trigger(player);
            }
        }
    }

    public static void fireReforge(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.FIRE_REFORGE.get().trigger(player);
            }
        }
    }

    public static void heatCollectOn(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity entity) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 7)) {
                ModCriterionTriggers.HEAT_COLLECTOR_COLLECT.get().trigger(player, state, entity, -1);
            }
        }
    }

    public static void heatCollectorOutput(Level level, BlockPos pos, int output) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 7)) {
                ModCriterionTriggers.HEAT_COLLECTOR_COLLECT.get().trigger(player, Blocks.AIR.defaultBlockState(), null, output);
            }
        }
    }
}
