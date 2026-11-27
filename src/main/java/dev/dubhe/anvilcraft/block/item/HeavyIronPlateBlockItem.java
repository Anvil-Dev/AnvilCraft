package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.block.entity.ChutePlates;
import dev.dubhe.anvilcraft.util.BlockPlacementPicking;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class HeavyIronPlateBlockItem extends BlockItem {
    public HeavyIronPlateBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (context.isSecondaryUseActive() || !(level.getBlockEntity(pos) instanceof ChutePlates.Holder chute)) {
            return super.useOn(context);
        }
        if (context instanceof BlockPlacementPicking.PlacementRejection rejected && rejected.anvilcraft$isPlacementRejected()
            || !chute.getChutePlates().installPlate(context.getClickedFace())) {
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        BlockState plate = this.getBlock().defaultBlockState();
        SoundType sound = plate.getSoundType(level, pos, player);
        level.playSound(player, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8f);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, plate));
        context.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
