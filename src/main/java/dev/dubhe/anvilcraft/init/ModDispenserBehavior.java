package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.ItemEntry;
import dev.dubhe.anvilcraft.block.item.HasMobBlockItem;
import dev.dubhe.anvilcraft.block.item.ResinBlockItem;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.EntityUtil;
import dev.dubhe.anvilcraft.util.PlayerUtil;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DispenserBlock;
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
    private static final DefaultDispenseItemBehavior DEFAULT_BEHAVIOUR = new DefaultDispenseItemBehavior();
    private static final DefaultDispenseItemBehavior BUCKET = new DefaultDispenseItemBehavior() {
        @Override
        public ItemStack execute(BlockSource source, ItemStack stack) {
            DispensibleContainerItem item = (DispensibleContainerItem) stack.getItem();
            BlockPos blockpos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
            Level level = source.level();
            if (item.emptyContents(null, level, blockpos, null, stack)) {
                item.checkExtraContent(null, level, stack, blockpos);
                return this.consumeWithRemainder(source, stack, new ItemStack(Items.BUCKET));
            } else {
                return ModDispenserBehavior.DEFAULT_BEHAVIOUR.dispense(source, stack);
            }
        }
    };

    public static void register() {
        DispenserBlock.registerBehavior(Items.IRON_INGOT, ModDispenserBehavior::ironIngot);
        DispenserBlock.registerBehavior(Items.BOWL, ModDispenserBehavior::bowl);
        DispenserBlock.registerBehavior(Items.GOLDEN_APPLE, ModDispenserBehavior::goldenApple);
        DispenserBlock.registerBehavior(ModBlocks.RESIN_BLOCK, ModDispenserBehavior::resinBlock);
        DispenserBlock.registerBehavior(ModItems.OIL_BUCKET, BUCKET);
        DispenserBlock.registerBehavior(ModItems.MELT_GEM_BUCKET, BUCKET);
        DispenserBlock.registerBehavior(ModBlocks.MENGER_SPONGE, ModDispenserBehavior::mengerSponge);
        for (ItemEntry<BucketItem> cementBucket : ModItems.CEMENT_BUCKETS.values()) {
            DispenserBlock.registerBehavior(cementBucket, BUCKET);
        }
    }

    private static ItemStack mengerSponge(BlockSource source, ItemStack stack) {
        ServerLevel level = source.level();
        BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        if (level.getBlockState(pos).getBlock() instanceof AbstractCauldronBlock) {
            level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
        }
        return stack;
    }

    private static ItemStack ironIngot(BlockSource source, ItemStack stack) {
        BlockPos blockPos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        ServerLevel level = source.level();
        if (level.getBlockState(blockPos).is(ModBlocks.HOLLOW_MAGNET_BLOCK)) {
            level.setBlockAndUpdate(blockPos, ModBlocks.FERRITE_CORE_MAGNET_BLOCK.getDefaultState());
            ItemStack stack1 = stack.copy();
            stack1.shrink(1);
            return stack1;
        }
        List<IronGolem> entities =
            level
                .getEntities(EntityTypeTest.forClass(IronGolem.class), new AABB(blockPos), Entity::isAlive)
                .stream()
                .filter(e -> e.getHealth() < e.getMaxHealth())
                .toList();
        if (entities.isEmpty()) return ModDispenserBehavior.DEFAULT_BEHAVIOUR.dispense(source, stack);
        IronGolem ironGolem = entities.get(level.random.nextInt(0, entities.size()));
        ironGolem.heal(25.0f);
        float g = 1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f;
        ironGolem.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0f, g);
        ItemStack stack1 = stack.copy();
        stack1.shrink(1);
        for (ServerPlayer player : PlayerUtil.searchPlayerByPos(level, blockPos, 5)) {
            ModCriterionTriggers.REPAIR_IRON_GOLEM.get().trigger(player);
        }
        return stack1;
    }

    private static ItemStack bowl(BlockSource blockSource, ItemStack bowlStack) {
        MushroomCow mushroomCow = EntityUtil.getAnyEntityOfClass(
            blockSource.level(), MushroomCow.class,
            new AABB(blockSource.pos().relative(blockSource.state().getValue(DirectionalBlock.FACING))),
            m -> !m.isBaby()
        );

        if (mushroomCow == null) return DEFAULT_BEHAVIOUR.dispense(blockSource, bowlStack);

        ItemStack stewItem;
        SoundEvent sound;
        if (mushroomCow.stewEffects == null) {
            stewItem = new ItemStack(Items.MUSHROOM_STEW);
            sound = SoundEvents.MOOSHROOM_MILK;
        } else {
            stewItem = new ItemStack(Items.SUSPICIOUS_STEW);
            stewItem.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, mushroomCow.stewEffects);
            mushroomCow.stewEffects = null;
            sound = SoundEvents.MOOSHROOM_MILK_SUSPICIOUSLY;
        }
        mushroomCow.playSound(sound, 1.0F, 1.0F);

        bowlStack.shrink(1);

        if (bowlStack.isEmpty()) return stewItem;
        ItemStack remainedStewItem = blockSource.blockEntity().insertItem(stewItem);
        if (!remainedStewItem.isEmpty()) DEFAULT_BEHAVIOUR.dispense(blockSource, remainedStewItem);
        return bowlStack;
    }

    private static ItemStack goldenApple(BlockSource blockSource, ItemStack stack) {
        ZombieVillager zombieVillager = EntityUtil.getAnyEntityOfClass(
            blockSource.level(), ZombieVillager.class,
            new AABB(blockSource.pos().relative(blockSource.state().getValue(DirectionalBlock.FACING))),
            z -> z.hasEffect(MobEffects.WEAKNESS) && !z.isConverting()
        );
        if (zombieVillager == null) return DEFAULT_BEHAVIOUR.dispense(blockSource, stack);
        zombieVillager.startConverting(ANVILCRAFT_DISPENSER, zombieVillager.getRandom().nextInt(2401) + 3600);
        stack.shrink(1);
        return stack;
    }

    private static ItemStack resinBlock(BlockSource blockSource, ItemStack resinBlockItem) {
        if (ResinBlockItem.hasMob(resinBlockItem)) {
            ItemStack resin = ResinBlockItem.spawnMobFromItem(
                blockSource.level(), blockSource.pos().relative(blockSource.state().getValue(DirectionalBlock.FACING)), resinBlockItem
            );
            if (!resin.isEmpty()) {
                DefaultDispenseItemBehavior.spawnItem(
                    blockSource.level(), resin, 6, blockSource.state().getValue(DispenserBlock.FACING),
                    DispenserBlock.getDispensePosition(blockSource)
                );
            }
        } else {
            Mob mob = EntityUtil.getAnyEntityOfClass(
                blockSource.level(), Mob.class,
                new AABB(blockSource.pos().relative(blockSource.state().getValue(DirectionalBlock.FACING))),
                HasMobBlockItem::canMobBeSaved
            );
            if (mob == null) return DEFAULT_BEHAVIOUR.dispense(blockSource, resinBlockItem);
            ItemStack mobResin = ResinBlockItem.saveMobInItem(blockSource.level(), mob, resinBlockItem);

            if (resinBlockItem.isEmpty()) return mobResin;

            ItemStack remainedMobResin = blockSource.blockEntity().insertItem(mobResin);

            if (!remainedMobResin.isEmpty()) {
                DefaultDispenseItemBehavior.spawnItem(
                    blockSource.level(), remainedMobResin, 6,
                    blockSource.state().getValue(DispenserBlock.FACING),
                    DispenserBlock.getDispensePosition(blockSource)
                );
            }
        }
        return resinBlockItem;
    }
}
