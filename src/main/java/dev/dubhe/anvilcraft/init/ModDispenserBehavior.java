package dev.dubhe.anvilcraft.init;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModDispenserBehavior {
    /*
    "anvilcraft".hashcode() == 976850d4
    "dispenser".hashcode() == e652ab5
    "representing".hashcode() == 83d24bba
    "all_players".hashcode() == 75a6b114
     */
    public static final UUID ANVILCRAFT_DISPENSER = new UUID(0x976850D40E652AB5L, 0x83D24BBA75A6B114L);
    private static final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

    public static void register() {
        DispenserBlock.registerBehavior(Items.IRON_INGOT, ModDispenserBehavior::ironIngot);
        DispenserBlock.registerBehavior(Items.BOWL, new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource blockSource, ItemStack bowlItem) {
                Level level = blockSource.level();
                BlockPos pos = blockSource.pos();
                BlockState state = blockSource.state();
                List<MushroomCow> mushroomCows = level.getEntitiesOfClass(MushroomCow.class,
                    new AABB(pos.relative(state.getValue(DirectionalBlock.FACING))),
                    m -> !m.isBaby());
                if (mushroomCows.isEmpty()) return super.execute(blockSource, bowlItem);
                MushroomCow mushroomCow = mushroomCows.getFirst();
                ItemStack stewItem;
                SoundEvent sound;
                if (mushroomCow.stewEffects != null) {
                    stewItem = new ItemStack(Items.SUSPICIOUS_STEW);
                    stewItem.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, mushroomCow.stewEffects);
                    mushroomCow.stewEffects = null;
                    sound = SoundEvents.MOOSHROOM_MILK_SUSPICIOUSLY;
                } else {
                    stewItem = new ItemStack(Items.MUSHROOM_STEW);
                    sound = SoundEvents.MOOSHROOM_MILK;
                }
                mushroomCow.playSound(sound, 1.0F, 1.0F);
                return this.consumeWithRemainder(blockSource, bowlItem, stewItem);
            }
        });
        DispenserBlock.registerBehavior(Items.GOLDEN_APPLE, new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource blockSource, ItemStack goldenAppleItem) {
                Level level = blockSource.level();
                BlockPos pos = blockSource.pos();
                BlockState state = blockSource.state();
                List<ZombieVillager> zombieVillagers = level.getEntitiesOfClass(ZombieVillager.class,
                    new AABB(pos.relative(state.getValue(DirectionalBlock.FACING))),
                    z -> z.hasEffect(MobEffects.WEAKNESS) && !z.isConverting());
                if (zombieVillagers.isEmpty()) return super.execute(blockSource, goldenAppleItem);
                ZombieVillager zombieVillager = zombieVillagers.getFirst();
                zombieVillager.startConverting(ANVILCRAFT_DISPENSER,
                    zombieVillager.getRandom().nextInt(2401) + 3600);
                goldenAppleItem.shrink(1);
                return goldenAppleItem;
            }
        });
    }

    private static ItemStack ironIngot(BlockSource source, ItemStack stack) {
        BlockPos blockPos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        ServerLevel level = source.level();
        List<IronGolem> entities =
            level
                .getEntities(EntityTypeTest.forClass(IronGolem.class), new AABB(blockPos), Entity::isAlive)
                .stream()
                .filter(e -> e.getHealth() < e.getMaxHealth())
                .toList();
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
