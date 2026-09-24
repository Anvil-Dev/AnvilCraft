package dev.dubhe.anvilcraft.worldgen;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;

import java.util.Map;
import java.util.WeakHashMap;

final class OverworldLikeBorderFollower implements BorderChangeListener {
    private static final Map<MinecraftServer, OverworldLikeBorderFollower> FOLLOWERS = new WeakHashMap<>();
    private final WorldBorder target;

    private OverworldLikeBorderFollower(WorldBorder target) {
        this.target = target;
    }

    static void configure(MinecraftServer server, ServerLevel level) {
        clear(server);
        WorldBorder source = server.overworld().getWorldBorder();
        WorldBorder target = level.getWorldBorder();
        long now = server.overworld().getGameTime();
        source.applyInitialSettings(now);
        target.applyInitialSettings(now);
        var follower = new OverworldLikeBorderFollower(target);
        FOLLOWERS.put(server, follower);
        source.addListener(follower);
        target.setAbsoluteMaxSize(source.getAbsoluteMaxSize());
        target.setCenter(source.getCenterX(), source.getCenterZ());
        target.setDamagePerBlock(source.getDamagePerBlock());
        target.setSafeZone(source.getSafeZone());
        target.setWarningBlocks(source.getWarningBlocks());
        target.setWarningTime(source.getWarningTime());
        if (source.getLerpTime() > 0) target.lerpSizeBetween(source.getSize(), source.getLerpTarget(), source.getLerpTime(), now);
        else target.setSize(source.getSize());
    }

    static void clear(MinecraftServer server) {
        var follower = FOLLOWERS.remove(server);
        if (follower != null && server.overworld() != null) server.overworld().getWorldBorder().removeListener(follower);
    }

    @Override
    public void onSetSize(WorldBorder border, double size) {
        this.target.setSize(size);
    }

    @Override
    public void onLerpSize(WorldBorder border, double from, double to, long ticks, long gameTime) {
        this.target.lerpSizeBetween(from, to, ticks, gameTime);
    }

    @Override
    public void onSetCenter(WorldBorder border, double x, double z) {
        this.target.setCenter(x, z);
    }

    @Override
    public void onSetWarningTime(WorldBorder border, int time) {
        this.target.setWarningTime(time);
    }

    @Override
    public void onSetWarningBlocks(WorldBorder border, int blocks) {
        this.target.setWarningBlocks(blocks);
    }

    @Override
    public void onSetDamagePerBlock(WorldBorder border, double damage) {
        this.target.setDamagePerBlock(damage);
    }

    @Override
    public void onSetSafeZone(WorldBorder border, double safeZone) {
        this.target.setSafeZone(safeZone);
    }
}
