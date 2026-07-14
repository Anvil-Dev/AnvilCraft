package dev.dubhe.anvilcraft.api.entity.fakeplayer;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.api.entity.player.IAnvilCraftBlockPlacer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.UUID;

public class AnvilCraftBlockPlacerFakePlayer implements IAnvilCraftBlockPlacer {
    static final UUID placerUUID = UUID.randomUUID();
    static final String placerName = "AnvilCraftBlockPlacer";
    static final GameProfile fakeProfile = new GameProfile(placerUUID, "[Block Placer of " + placerName + "]");
    private final ServerLevel level;
    private ServerPlayer fakePlayer;

    public AnvilCraftBlockPlacerFakePlayer(ServerLevel level) {
        this.level = level;
    }

    @Override
    public ServerPlayer getPlayer() {
        if (fakePlayer == null) {
            fakePlayer = FakePlayerFactory.get(level, fakeProfile);
        }
        return fakePlayer;
    }
}
