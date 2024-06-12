package dev.dubhe.anvilcraft.api.entity.fakeplayer.forge;

import dev.dubhe.anvilcraft.api.entity.player.IAnvilCraftBlockPlacer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

public class AnvilCraftBlockPlacerFakePlayer implements IAnvilCraftBlockPlacer {
    static final UUID placerUUID = UUID.randomUUID();
    static final String placerName = "AnvilCraftBlockPlacer";
    static final GameProfile fakeProfile =
            new GameProfile(placerUUID, "[Block Placer of " + placerName + "]");
    private static ServerPlayer fakePlayer;

    public AnvilCraftBlockPlacerFakePlayer(ServerLevel world) {
        fakePlayer = FakePlayerFactory.get(world, fakeProfile);
    }

    @Override
    public ServerPlayer getPlayer() {
        return fakePlayer;
    }
}
