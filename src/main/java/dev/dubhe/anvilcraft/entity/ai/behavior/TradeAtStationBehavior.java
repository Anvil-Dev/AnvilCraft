package dev.dubhe.anvilcraft.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import dev.dubhe.anvilcraft.block.entity.TradingStationBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.entity.ModVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Comparator;
import java.util.Optional;

public class TradeAtStationBehavior extends Behavior<Villager> {
    private static final int SEARCH_RADIUS = 32;
    private static final double INTERACT_DISTANCE_SQR = 4.0D;
    private static final float SPEED_MODIFIER = 0.5F;
    private static final int TRADE_INTERVAL_TICKS = 20;
    private static final int MAX_TICKS = 600;

    private BlockPos stationPos;
    private int tradeCooldown;
    private int failedAttempts;

    public TradeAtStationBehavior() {
        super(ImmutableMap.of(
            MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.LAST_WORKED_AT_POI, MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
            MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
        ), TradeAtStationBehavior.MAX_TICKS
        );
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        if (villager.getOffers().isEmpty()) return false;
        if (villager.isSleeping() || villager.isBaby()) return false;
        BlockPos found = TradeAtStationBehavior.findMatchingStation(level, villager);
        if (found == null) return false;
        this.stationPos = found;
        return true;
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        this.tradeCooldown = 0;
        this.failedAttempts = 0;
        Brain<Villager> brain = villager.getBrain();
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.stationPos));
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.stationPos, TradeAtStationBehavior.SPEED_MODIFIER, 1));
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        if (this.stationPos == null) return false;
        if (this.failedAttempts >= 3) return false;
        TradingStationBlockEntity be = TradeAtStationBehavior.getStation(level, this.stationPos);
        if (be == null) return false;
        return be.canTradeWithVillager(villager);
    }

    @Override
    protected void tick(ServerLevel level, Villager villager, long gameTime) {
        if (this.stationPos == null) return;
        double distSqr = villager.blockPosition().distSqr(this.stationPos);
        if (distSqr > TradeAtStationBehavior.INTERACT_DISTANCE_SQR) {
            Brain<Villager> brain = villager.getBrain();
            if (brain.getMemory(MemoryModuleType.WALK_TARGET).isEmpty()) {
                brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.stationPos, TradeAtStationBehavior.SPEED_MODIFIER, 1));
            }
            return;
        }
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.stationPos));
        if (this.tradeCooldown > 0) {
            this.tradeCooldown--;
            return;
        }
        TradingStationBlockEntity be = TradeAtStationBehavior.getStation(level, this.stationPos);
        if (be == null) {
            this.failedAttempts = Integer.MAX_VALUE;
            return;
        }
        if (be.tryTradingWithVillager(villager)) {
            villager.playCelebrateSound();
            this.tradeCooldown = TradeAtStationBehavior.TRADE_INTERVAL_TICKS;
            this.failedAttempts = 0;
        } else {
            this.failedAttempts++;
            this.tradeCooldown = TradeAtStationBehavior.TRADE_INTERVAL_TICKS;
        }
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.stationPos = null;
    }

    private static TradingStationBlockEntity getStation(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos, ModBlockEntities.TRADING_STATION.get()).orElse(null);
    }

    private static BlockPos findMatchingStation(ServerLevel level, Villager villager) {
        PoiManager poi = level.getPoiManager();
        PoiType target = ModVillagers.TRADING_STATION_POI.get();
        Optional<BlockPos> match = poi.findAll(
                h -> h.value() == target,
                pos -> {
                    TradingStationBlockEntity be = TradeAtStationBehavior.getStation(level, pos);
                    return be != null && be.canTradeWithVillager(villager);
                },
                villager.blockPosition(),
                TradeAtStationBehavior.SEARCH_RADIUS,
                PoiManager.Occupancy.ANY
            )
            .min(Comparator.comparingDouble(p -> p.distSqr(villager.blockPosition())));
        return match.orElse(null);
    }
}
