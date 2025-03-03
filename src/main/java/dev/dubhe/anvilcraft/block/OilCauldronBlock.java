package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.util.ModInteractionMap;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class OilCauldronBlock extends Layered4LevelCauldronBlock implements IHammerRemovable {
    public OilCauldronBlock(Properties properties) {
        super(properties, ModInteractionMap.OIL);
    }

    public static void ignite(Level level, BlockPos pos, BlockState beforeConvert) {
        level.setBlockAndUpdate(pos, ModBlocks.FIRE_CAULDRON.get().copyLevelFrom(beforeConvert));
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!this.isEntityInsideContent(state, pos, entity)) return;
        if (entity.getType().equals(EntityType.ARROW) && entity.isOnFire()) {
            ignite(level, pos, state);
            return;
        }
        if (!(entity instanceof ItemEntity itemEntity)) return;
        if (itemEntity.getItem().is(ModItemTags.FIRE_STARTER)) {
            ignite(level, pos, state);
            itemEntity.getItem().setCount(itemEntity.getItem().getCount() - 1);
            return;
        }
        if (itemEntity.getItem().is(ModItemTags.UNBROKEN_FIRE_STARTER)) {
            ignite(level, pos, state);
        }
    }

    @Override
    public ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        CauldronInteraction interaction = this.interactions.map().get(stack.getItem());
        if (interaction == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return interaction.interact(state, level, pos, player, hand, stack);
    }
}
