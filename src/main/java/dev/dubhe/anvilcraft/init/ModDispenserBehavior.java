package dev.dubhe.anvilcraft.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.ItemEntry;
import dev.dubhe.anvilcraft.api.fluid.FluidHandlerWrapper;
import dev.dubhe.anvilcraft.block.item.HasMobBlockItem;
import dev.dubhe.anvilcraft.block.item.ResinBlockItem;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.Item;
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

import java.util.List;
import java.util.UUID;

import static dev.dubhe.anvilcraft.block.MagnetBlock.LIT;

public class ModDispenserBehavior {
    /*
    "anvilcraft".hashcode() == 976850d4
    "dispenser".hashcode() == e652ab5
    "representing".hashcode() == 83d24bba
    "all_players".hashcode() == 75a6b114
     */
    public static final UUID ANVILCRAFT_DISPENSER = new UUID(0x976850D40E652AB5L, 0x83D24BBA75A6B114L);
    private static final DefaultDispenseItemBehavior DEFAULT_BEHAVIOUR = new DefaultDispenseItemBehavior();

    /**
     * 特殊桶类（油桶、水泥桶等）使用原版 {@link DispensibleContainerItem#emptyContents} 逻辑。
     */
    private static final DefaultDispenseItemBehavior DISPENSIBLE_BUCKET = new DefaultDispenseItemBehavior() {
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

    /**
     * 对物品包装一个 FluidHandlerWrapper 优先的发射行为。
     *
     * <p>先尝试 {@link FluidHandlerWrapper#fillFromItem}（物品→处理器），
     * 再尝试 {@link FluidHandlerWrapper#drainToItem}（处理器→物品），
     * 两者都失败时回退到原版行为。
     */
    private static DefaultDispenseItemBehavior wrapFluidInteraction(DispenseItemBehavior original) {
        return new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource source, ItemStack stack) {
                BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                Level level = source.level();
                FluidHandlerWrapper wrapper = FluidHandlerWrapper.of(level, pos, null);
                if (wrapper != null) {
                    // 先试填充（物品→处理器）
                    ItemStack result = wrapper.fillFromItem(stack, false, level.getRandom());
                    if (result != null) {
                        playEmptySound(level, pos, stack);
                        return this.consumeWithRemainder(source, stack, result);
                    }
                    // 再试抽取（处理器→物品）
                    result = wrapper.drainToItem(stack);
                    if (result != null) {
                        playFillSound(level, pos, stack);
                        return this.consumeWithRemainder(source, stack, result);
                    }
                }
                return original.dispense(source, stack);
            }
        };
    }

    public static void register() {
        DispenserBlock.registerBehavior(Items.IRON_INGOT, ModDispenserBehavior::ironIngot);
        DispenserBlock.registerBehavior(Items.BOWL, ModDispenserBehavior::bowl);
        DispenserBlock.registerBehavior(Items.GOLDEN_APPLE, ModDispenserBehavior::goldenApple);
        DispenserBlock.registerBehavior(ModItems.TOPAZ, ModDispenserBehavior::topaz);
        DispenserBlock.registerBehavior(ModBlocks.RESIN_BLOCK, ModDispenserBehavior::resinBlock);
        DispenserBlock.registerBehavior(ModBlocks.MENGER_SPONGE, ModDispenserBehavior::mengerSponge);

        // 特殊桶：优先与流体容器交互，失败后走原版排放逻辑
        DispenserBlock.registerBehavior(ModItems.EXP_BUCKET, wrapFluidInteraction(DISPENSIBLE_BUCKET));
        DispenserBlock.registerBehavior(ModItems.OIL_BUCKET, wrapFluidInteraction(DISPENSIBLE_BUCKET));
        DispenserBlock.registerBehavior(ModItems.MELT_GEM_BUCKET, wrapFluidInteraction(DISPENSIBLE_BUCKET));
        for (ItemEntry<BucketItem> cementBucket : ModItems.CEMENT_BUCKETS.values()) {
            DispenserBlock.registerBehavior(cementBucket, wrapFluidInteraction(DISPENSIBLE_BUCKET));
        }

        // 通用流体物品：FluidHandlerWrapper 优先，失败回退原版
        wrapAndRegister(Items.MILK_BUCKET);
        wrapAndRegister(Items.WATER_BUCKET);
        wrapAndRegister(Items.LAVA_BUCKET);
        wrapAndRegister(Items.POWDER_SNOW_BUCKET);
        wrapAndRegister(Items.BUCKET);
        wrapAndRegister(Items.GLASS_BOTTLE);
        wrapAndRegister(Items.HONEY_BOTTLE);
        wrapAndRegister(Items.POTION);
        wrapAndRegister(Items.EXPERIENCE_BOTTLE);
    }

    private static void wrapAndRegister(Item item) {
        DispenseItemBehavior original = DispenserBlock.DISPENSER_REGISTRY.get(item);
        if (original == null) return;
        DispenserBlock.registerBehavior(item, wrapFluidInteraction(original));
    }

    private static void playEmptySound(Level level, BlockPos pos, ItemStack container) {
        SoundEvent sound = container.is(Items.GLASS_BOTTLE)
            || container.is(Items.HONEY_BOTTLE)
            || container.is(Items.POTION)
            || container.is(Items.EXPERIENCE_BOTTLE)
            ? SoundEvents.BOTTLE_EMPTY
            : SoundEvents.BUCKET_EMPTY;
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static void playFillSound(Level level, BlockPos pos, ItemStack container) {
        SoundEvent sound = container.is(Items.GLASS_BOTTLE)
            ? SoundEvents.BOTTLE_FILL
            : SoundEvents.BUCKET_FILL;
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /**
     * 发射黄玉：发射方向前方是避雷针时，在避雷针处召唤闪电（与 {@code TopazItem} 右键一致），
     * 消耗一个黄玉；否则按原版行为抛掷物品。
     */
    private static ItemStack topaz(BlockSource source, ItemStack stack) {
        ServerLevel level = source.level();
        BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        if (!level.getBlockState(pos).is(Blocks.LIGHTNING_ROD)) {
            return ModDispenserBehavior.DEFAULT_BEHAVIOUR.dispense(source, stack);
        }
        LightningBolt lightningBolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        lightningBolt.setPos(pos.getCenter());
        level.addFreshEntity(lightningBolt);
        ItemStack stack1 = stack.copy();
        stack1.shrink(1);
        return stack1;
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
            if (blockState.hasProperty(LIT)) {
                blockState = blockState.setValue(LIT, level.hasNeighborSignal(blockPos));
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
