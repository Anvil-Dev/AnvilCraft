package dev.dubhe.anvilcraft.block.plate;

import com.google.common.collect.ImmutableSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.AABB;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PlayerInHandItemDurabilityPressurePlateBlock extends PowerLevelPressurePlateBlock {
    public PlayerInHandItemDurabilityPressurePlateBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
    }

    @Override
    protected Set<Class<? extends Entity>> getEntityClasses() {
        return ImmutableSet.of(Player.class);
    }

    @Override
    protected int getSignalStrength(Level level, AABB box, Set<Class<? extends Entity>> entityClasses) {
        List<Player> players = level.getEntitiesOfClass(Player.class,
            box, EntitySelector.NO_SPECTATORS.and(entity -> !entity.isIgnoringBlockTriggers()));
        if (players.isEmpty()) {
            return 0;
        }
        Player player = players.getFirst();

        int result;
        ItemStack item = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (item.isEmpty()) {
            return 0;
        }

        Integer maxDamage = item.get(DataComponents.MAX_DAMAGE);
        Integer damage = item.get(DataComponents.DAMAGE);
        if (maxDamage != null) {
            if (damage == null) {
                damage = 0;
            }
            int remain = maxDamage - damage;
            int percent = (remain * 15 / maxDamage);
            if (percent == 0) {
                percent = 1;
            }
            result = percent;
        } else {
            result = Integer.MAX_VALUE;
        }

        return Math.clamp(result, 0, 15);
    }
}
