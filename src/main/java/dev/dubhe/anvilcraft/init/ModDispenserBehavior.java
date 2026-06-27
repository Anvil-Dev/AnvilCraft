package dev.dubhe.anvilcraft.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.ItemEntry;
import dev.dubhe.anvilcraft.block.storage.MagnetBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.block.HasMobBlockItem;
import dev.dubhe.anvilcraft.item.block.ResinBlockItem;
import dev.dubhe.anvilcraft.util.EntityUtil;
import dev.dubhe.anvilcraft.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.UUID;

public class ModDispenserBehavior {
    // "anvilcraft".hashcode() == 976850d4
    // "dispenser".hashcode() == e652ab5
    // "representing".hashcode() == 83d24bba
    // "all_players".hashcode() == 75a6b114
    public static final UUID ANVILCRAFT_DISPENSER = new UUID(0x976850D40E652AB5L, 0x83D24BBA75A6B114L);
    private static final DefaultDispenseItemBehavior DEFAULT_BEHAVIOUR = new DefaultDispenseItemBehavior();

    /// 装满的液体容器（液体桶 / 蜂蜜瓶）：尝试将容器内液体注入目标方块的流体能力，成功后返回对应空容器
    private static final DefaultDispenseItemBehavior FLUID_BUCKET = new DefaultDispenseItemBehavior() {
        @Override
        public ItemStack execute(BlockSource source, ItemStack stack) {
            BlockPos blockpos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
            Level level = source.level();
            ResourceHandler<FluidResource> target = level.getCapability(Capabilities.Fluid.BLOCK, blockpos, null);
            if (target != null) {
                // 单独取一份容器物品用于读取其流体内容，避免影响发射器槽内的整组堆叠
                ItemStack single = stack.copyWithCount(1);
                ResourceHandler<FluidResource> itemHandler =
                    single.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forStack(single));
                if (itemHandler != null) {
                    for (int i = 0; i < itemHandler.size(); i++) {
                        FluidResource resource = itemHandler.getResource(i);
                        int available = itemHandler.getAmountAsInt(i);
                        if (resource.isEmpty() || available <= 0) continue;
                        try (Transaction transaction = Transaction.openRoot()) {
                            int filled = target.insert(resource, available, transaction);
                            if (filled < available) continue;
                            transaction.commit();
                            return this.consumeWithRemainder(source, stack, emptyContainerFor(stack));
                        }
                    }
                }
            }
            return ModDispenserBehavior.DEFAULT_BEHAVIOUR.dispense(source, stack);
        }
    };

    /// 根据装满的液体容器返回其对应的空容器
    private static ItemStack emptyContainerFor(ItemStack filled) {
        if (filled.is(Items.HONEY_BOTTLE)) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        return new ItemStack(Items.BUCKET);
    }

    /// 空液体容器（空桶/玻璃瓶）：尝试从目标方块的流体能力抽取液体，装入对应容器
    private static final DefaultDispenseItemBehavior EMPTY_FLUID_CONTAINER = new DefaultDispenseItemBehavior() {
        @Override
        public ItemStack execute(BlockSource source, ItemStack stack) {
            BlockPos blockpos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
            Level level = source.level();
            ResourceHandler<FluidResource> target = level.getCapability(Capabilities.Fluid.BLOCK, blockpos, null);
            if (target != null) {
                boolean isBottle = stack.is(Items.GLASS_BOTTLE);
                int amount = isBottle ? FluidType.BUCKET_VOLUME / 4 : FluidType.BUCKET_VOLUME;
                for (int i = 0; i < target.size(); i++) {
                    FluidResource resource = target.getResource(i);
                    if (resource.isEmpty()) continue;
                    try (Transaction transaction = Transaction.openRoot()) {
                        int drained = target.extract(i, resource, amount, transaction);
                        if (drained < amount) continue;
                        ItemStack result;
                        if (isBottle && resource.getFluid() instanceof dev.dubhe.anvilcraft.fluid.HoneyFluid) {
                            result = new ItemStack(Items.HONEY_BOTTLE);
                        } else if (!isBottle && resource.getFluid().getBucket() != Items.AIR) {
                            result = new ItemStack(resource.getFluid().getBucket());
                        } else {
                            continue;
                        }
                        transaction.commit();
                        return this.consumeWithRemainder(source, stack, result);
                    }
                }
            }
            return ModDispenserBehavior.DEFAULT_BEHAVIOUR.dispense(source, stack);
        }
    };

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
        DispenserBlock.registerBehavior(Items.MILK_BUCKET, FLUID_BUCKET);
        DispenserBlock.registerBehavior(Items.HONEY_BOTTLE, FLUID_BUCKET);
        DispenserBlock.registerBehavior(ModItems.OIL_BUCKET, BUCKET);
        DispenserBlock.registerBehavior(ModItems.MELT_GEM_BUCKET, BUCKET);
        DispenserBlock.registerBehavior(ModBlocks.MENGER_SPONGE, ModDispenserBehavior::mengerSponge);
        for (ItemEntry<BucketItem> cementBucket : ModItems.CEMENT_BUCKETS.values()) {
            DispenserBlock.registerBehavior(cementBucket, BUCKET);
        }

        // 空桶：优先尝试从目标方块的流体能力抽取液体生成对应桶物品，未命中时降级到原版行为
        DispenseItemBehavior originalBucket = DispenserBlock.DISPENSER_REGISTRY.get(Items.BUCKET);
        DispenserBlock.registerBehavior(Items.BUCKET, new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource source, ItemStack stack) {
                BlockPos blockpos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                Level level = source.level();
                ResourceHandler<FluidResource> target = level.getCapability(Capabilities.Fluid.BLOCK, blockpos, null);
                if (target != null) {
                    int amount = FluidType.BUCKET_VOLUME;
                    for (int i = 0; i < target.size(); i++) {
                        FluidResource resource = target.getResource(i);
                        if (resource.isEmpty() || resource.getFluid().getBucket() == Items.AIR) continue;
                        try (Transaction transaction = Transaction.openRoot()) {
                            int drained = target.extract(i, resource, amount, transaction);
                            if (drained < amount) continue;
                            ItemStack bucket = new ItemStack(resource.getFluid().getBucket());
                            transaction.commit();
                            return this.consumeWithRemainder(source, stack, bucket);
                        }
                    }
                }
                return originalBucket.dispense(source, stack);
            }
        });
        DispenserBlock.registerBehavior(Items.GLASS_BOTTLE, EMPTY_FLUID_CONTAINER);
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
            BlockState blockState = ModBlocks.FERRITE_CORE_MAGNET_BLOCK.get().defaultBlockState();
            if (blockState.hasProperty(MagnetBlock.LIT)) {
                blockState = blockState.setValue(MagnetBlock.LIT, level.hasNeighborSignal(blockPos));
            }
            level.setBlockAndUpdate(blockPos, blockState);
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
        IronGolem ironGolem = entities.get(level.getRandom().nextInt(0, entities.size()));
        ironGolem.heal(25.0F);
        float g = 1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F;
        ironGolem.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, g);
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
