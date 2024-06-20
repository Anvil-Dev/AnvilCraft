package dev.dubhe.anvilcraft.api;

import net.minecraft.core.BlockPos;

import java.util.HashSet;

public class BonemealManager {
    public static final HashSet<BlockPos> bonemealBlocks = new HashSet<>();
    protected int cooldown = 0;
}
