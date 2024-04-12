package dev.dubhe.anvilcraft.init;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ModDispenserBehavior {
    private static final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

    public static void register() {
        DispenserBlock.registerBehavior(Items.IRON_INGOT, ModDispenserBehavior::ironIngot);
    }

    private static @NotNull ItemStack ironIngot(@NotNull BlockSource source, @NotNull ItemStack stack) {
        BlockPos blockPos = source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));
        ServerLevel level = source.getLevel();
        List<IronGolem> entities = level.getEntities(EntityTypeTest.forClass(IronGolem.class),
            new AABB(blockPos), Entity::isAlive).stream().filter(e -> e.getHealth() < e.getMaxHealth()).toList();
        if (entities.isEmpty()) return ModDispenserBehavior.defaultDispenseItemBehavior.dispense(source, stack);
        IronGolem ironGolem = entities.get(level.random.nextInt(0, entities.size()));
        ironGolem.heal(25.0f);
        float g = 1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f;
        ironGolem.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0f, g);
        ItemStack stack1 = stack.copy();
        stack1.shrink(1);
        return stack1;
    }
}
