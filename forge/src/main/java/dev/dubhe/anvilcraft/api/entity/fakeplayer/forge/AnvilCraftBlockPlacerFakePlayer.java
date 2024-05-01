package dev.dubhe.anvilcraft.api.entity.fakeplayer.forge;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.api.entity.player.IAnvilCraftBlockPlacer;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.FakePlayerFactory;

public class AnvilCraftBlockPlacerFakePlayer implements IAnvilCraftBlockPlacer {
    static final UUID placerUUID = UUID.randomUUID();
    static final String placerName = "AnvilCraftBlockPlacer";
    static final GameProfile fakeProfile = new GameProfile(placerUUID, "[Block Placer of " + placerName + "]");
    private static ServerPlayer fakePlayer;

    public AnvilCraftBlockPlacerFakePlayer(ServerLevel world) {
        fakePlayer = FakePlayerFactory.get(world, fakeProfile);
    }

    @Override
    public InteractionResult placeBlock(Level level, BlockPos pos, Direction direction,
        BlockItem blockItem) {
        getPlayer().lookAt(Anchor.EYES, getPlayer().getEyePosition().relative(direction, 1));
        BlockHitResult blockHitResult = new BlockHitResult(
            pos.below().relative(direction.getOpposite()).getCenter(),
            direction,
            pos,
            false
        );
        BlockPlaceContext blockPlaceContext =
            new BlockPlaceContext(level,
                getPlayer(),
                getPlayer().getUsedItemHand(),
                new ItemStack(blockItem),
                blockHitResult
            );
        return blockItem.place(blockPlaceContext);
    }

    @Override
    public ServerPlayer getPlayer() {
        return fakePlayer;
    }
}
