package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTerminal;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        anvilHammerHurtEntity(level, pos, damage, Items.AIR);
    }

    public static void anvilHammerHurtEntity(Level level, BlockPos pos, float damage, Item item) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.ANVIL_HAMMER_HURT_ENTITY.get().trigger(player, damage, item);
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

    public static void enterPowerGrid(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.ENTER_POWER_GRID.get().trigger(player);
            }
        }
    }

    /** 电网规模达到 16 个组件时派发「高压电塔」。 */
    public static void highVoltageTower(Level level, BlockPos pos, int componentCount) {
        if (level.isClientSide || componentCount < HIGH_VOLTAGE_TOWER_COMPONENTS) return;
        for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
            ModCriterionTriggers.HIGH_VOLTAGE_TOWER.get().trigger(player);
        }
    }

    private static final int HIGH_VOLTAGE_TOWER_COMPONENTS = 16;

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

    public static void mineralFountainCreate(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 7)) {
                ModCriterionTriggers.MINERAL_FOUNTAIN_CREATE.get().trigger(player);
            }
        }
    }

    public static void placerShuttle(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 7)) {
                ModCriterionTriggers.PLACER_SHUTTLE.get().trigger(player);
            }
        }
    }

    public static void blockComparatorTurnOver(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.BLOCK_COMPARATOR_TURN_OVER.get().trigger(player);
            }
        }
    }

    public static void pipeConnectContainers(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 5)) {
                ModCriterionTriggers.PIPE_CONNECT_CONTAINERS.get().trigger(player);
            }
        }
    }

    public static void voidEnergyCollectorWorking(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 7)) {
                ModCriterionTriggers.VOID_ENERGY_COLLECTOR_WORKING.get().trigger(player);
            }
        }
    }

    public static void multiBlockForm(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 7)) {
                ModCriterionTriggers.MULTI_BLOCK_FORM.get().trigger(player);
            }
        }
    }

    public static void shulkerContainerUpgrade(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 7)) {
                ModCriterionTriggers.SHULKER_CONTAINER_UPGRADE.get().trigger(player);
            }
        }
    }

    public static void hyperdimensionStationUpgrade(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 7)) {
                ModCriterionTriggers.HYPERDIMENSION_STATION_UPGRADE.get().trigger(player);
            }
        }
    }

    public static void largeCauldronCocktail(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 7)) {
                ModCriterionTriggers.LARGE_CAULDRON_COCKTAIL.get().trigger(player);
            }
        }
    }

    public static void gammaLaser(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 32)) {
                ModCriterionTriggers.GAMMA_LASER.get().trigger(player);
            }
        }
    }

    public static void dysonSphere(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 32)) {
                ModCriterionTriggers.DYSON_SPHERE.get().trigger(player);
            }
        }
    }

    public static void infinitePower(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 32)) {
                ModCriterionTriggers.INFINITE_POWER.get().trigger(player);
            }
        }
    }

    public static void inexhaustible(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 32)) {
                ModCriterionTriggers.INEXHAUSTIBLE.get().trigger(player);
            }
        }
    }

    public static void railgunSelfKill(ServerPlayer player) {
        ModCriterionTriggers.RAILGUN_SELF_KILL.get().trigger(player);
    }

    public static void wormholeDelivery(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 32)) {
                ModCriterionTriggers.WORMHOLE_DELIVERY.get().trigger(player);
            }
        }
    }

    public static void celestialVisit(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 32)) {
                ModCriterionTriggers.CELESTIAL_VISIT.get().trigger(player, 1);
            }
        }
    }

    /**
     * 记录玩家锻造并锁定过的天体大类；每累计一种新的大类就派发一次，
     * 让「银河系漫游指南」按 1/5 ~ 5/5 逐步推进。
     */
    public static void celestialVisit(Level level, BlockPos pos, CelestialBodyClass bodyClass) {
        if (level.isClientSide || bodyClass == null) return;
        String name = bodyClass.category().name();
        for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 32)) {
            CompoundTag data = player.getPersistentData();
            ListTag stored = data.getList(CELESTIAL_VISITED_TAG, Tag.TAG_STRING);
            boolean known = false;
            for (Tag tag : stored) {
                if (tag.getAsString().equals(name)) {
                    known = true;
                    break;
                }
            }
            ListTag visited = new ListTag();
            visited.addAll(stored);
            if (!known) {
                visited.add(StringTag.valueOf(name));
                data.put(CELESTIAL_VISITED_TAG, visited);
            }
            /// 无论是否新大类都按当前累计数派发，便于旧存档补齐已错过的进度。
            ModCriterionTriggers.CELESTIAL_VISIT.get().trigger(player, visited.size());
        }
    }

    private static final String CELESTIAL_VISITED_TAG = "anvilcraft:celestial_visited";

    /** 演化终局伴随超新星爆发，玩家多在爆炸范围外观察，因此搜索半径对齐震屏半径。 */
    public static void celestialEvolution(Level level, BlockPos pos, int massAnvils, StellarTerminal.Kind terminal) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 32)) {
                ModCriterionTriggers.CELESTIAL_EVOLUTION.get().trigger(player, massAnvils, terminal);
            }
        }
    }

    public static void planetDetonation(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, pos, 7)) {
                ModCriterionTriggers.PLANET_DETONATION.get().trigger(player);
            }
        }
    }

    /** 死亡事件只涉及当事玩家，不需要按位置搜索。 */
    public static void planetaryCollapseDeath(ServerPlayer player) {
        ModCriterionTriggers.PLANETARY_COLLAPSE_DEATH.get().trigger(player);
    }

    /** 死亡事件只涉及当事玩家，不需要按位置搜索。 */
    public static void starContactDeath(ServerPlayer player) {
        ModCriterionTriggers.STAR_CONTACT_DEATH.get().trigger(player);
    }
}
