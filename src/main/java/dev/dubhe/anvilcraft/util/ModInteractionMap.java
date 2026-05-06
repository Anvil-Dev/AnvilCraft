package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cauldron.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.FishTankBlock;
import dev.dubhe.anvilcraft.block.cauldron.HoneyCauldronBlock;
import dev.dubhe.anvilcraft.block.cauldron.Layered4LevelCauldronBlock;
import dev.dubhe.anvilcraft.block.cauldron.OilCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.tool.MultitoolItem;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCauldronInteractionEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ModInteractionMap {
    public static final CauldronInteraction.Dispatcher LAVA = new CauldronInteraction.Dispatcher();
    public static final CauldronInteraction.Dispatcher EXP_FLUID = new CauldronInteraction.Dispatcher();
    public static final CauldronInteraction.Dispatcher OIL = new CauldronInteraction.Dispatcher();
    public static final CauldronInteraction.Dispatcher CEMENT = new CauldronInteraction.Dispatcher();
    public static final CauldronInteraction.Dispatcher HONEY = new CauldronInteraction.Dispatcher();
    public static final CauldronInteraction.Dispatcher MELT_GEM = new CauldronInteraction.Dispatcher();
    public static final CauldronInteraction.Dispatcher FISH_TANK = new CauldronInteraction.Dispatcher();
    public static final CauldronInteraction.Dispatcher OBSIDIAN = new CauldronInteraction.Dispatcher();

    @SubscribeEvent
    public static void registerDispatchers(RegisterCauldronInteractionEvent.Dispatcher event) {
        event.register(AnvilCraft.of("lava"), ModInteractionMap.LAVA);
        event.register(AnvilCraft.of("exp_fluid"), ModInteractionMap.EXP_FLUID);
        event.register(AnvilCraft.of("oil"), ModInteractionMap.OIL);
        event.register(AnvilCraft.of("cement"), ModInteractionMap.CEMENT);
        event.register(AnvilCraft.of("honey"), ModInteractionMap.HONEY);
        event.register(AnvilCraft.of("melt_gem"), ModInteractionMap.MELT_GEM);
        event.register(AnvilCraft.of("fish_tank"), ModInteractionMap.FISH_TANK);
        event.register(AnvilCraft.of("obsidian"), ModInteractionMap.OBSIDIAN);
    }
    
    @SubscribeEvent
    public static void registerInteractions(RegisterCauldronInteractionEvent.Interaction event) {
        Identifier lava = AnvilCraft.of("lava");
        event.register(
            lava,
            Items.BUCKET,
            (state, level, pos, player, hand, stack) -> CauldronInteractions.fillBucket(
                state,
                level,
                pos,
                player,
                hand,
                stack,
                Items.LAVA_BUCKET.getDefaultInstance(),
                s -> ModBlocks.LAVA_CAULDRON.get().isFull(s),
                SoundEvents.BUCKET_FILL_LAVA
            )
        );

        Identifier expFluid = AnvilCraft.of("exp_fluid");
        event.register(
            expFluid,
            Items.BUCKET,
            (state, level, pos, player, hand, stack) -> CauldronInteractions.fillBucket(
                state,
                level,
                pos,
                player,
                hand,
                stack,
                Items.LAVA_BUCKET.getDefaultInstance(),
                s -> ModBlocks.LAVA_CAULDRON.get().isFull(s),
                SoundEvents.BUCKET_FILL_LAVA
            )
        );
        event.register(
            expFluid,
            Items.GLASS_BOTTLE,
            (state, level, pos, player, hand, stack) -> {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                Item item = stack.getItem();
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, Items.EXPERIENCE_BOTTLE.getDefaultInstance()));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(item));
                Layered4LevelCauldronBlock.lowerFillLevel(state, level, pos);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
                return InteractionResult.SUCCESS_SERVER;
            }
        );
        event.register(
            expFluid,
            Items.EXPERIENCE_BOTTLE,
            (state, level, pos, player, hand, stack) -> {
                if (ModBlocks.EXP_FLUID_CAULDRON.get().isFull(state)) return InteractionResult.PASS;
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                player.awardStat(Stats.FILL_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                // 50%概率
                if (level.getRandom().nextBoolean()) {
                    level.setBlockAndUpdate(pos, state.cycle(HoneyCauldronBlock.LEVEL));
                }
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS);
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                return InteractionResult.SUCCESS_SERVER;
            }
        );

        Identifier oil = AnvilCraft.of("oil");
        event.register(
            oil,
            Items.BUCKET,
            (state, level, pos, player, hand, stack) -> CauldronInteractions.fillBucket(
                state,
                level,
                pos,
                player,
                hand,
                stack,
                ModItems.OIL_BUCKET.asStack(),
                s -> ModBlocks.OIL_CAULDRON.get().isFull(s),
                SoundEvents.BUCKET_FILL
            )
        );
        event.register(
            oil,
            Items.FLINT_AND_STEEL,
            (state, level, pos, player, hand, stack) -> {
                OilCauldronBlock.ignite(level, pos, state);
                stack.hurtAndBreak(2, player, hand.asEquipmentSlot());
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS);
                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            }
        );
        event.register(
            oil,
            Items.FIRE_CHARGE,
            (state, level, pos, _, _, stack) -> {
                OilCauldronBlock.ignite(level, pos, state);
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS);
                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            }
        );
        event.register(
            oil,
            ModItems.MULTITOOL_ITEM.asItem(),
            (state, level, pos, player, hand, stack) -> {
                if (MultitoolItem.getMode(stack) != MultitoolItem.FLINT_AND_STEEL_MODE) return InteractionResult.SUCCESS;
                OilCauldronBlock.ignite(level, pos, state);
                stack.hurtAndBreak(2, player, hand.asEquipmentSlot());
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS);
                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            }
        );

        Identifier cement = AnvilCraft.of("cement");
        event.register(
            cement,
            Items.BUCKET,
            (state, level, pos, player, hand, stack) -> {
                if (!(level.getBlockState(pos).getBlock() instanceof CementCauldronBlock cauldronBlock)) return InteractionResult.PASS;
                Color color = cauldronBlock.getColor();
                return CauldronInteractions.fillBucket(
                    state,
                    level,
                    pos,
                    player,
                    hand,
                    stack,
                    ModItems.CEMENT_BUCKETS.get(color).asStack(),
                    _ -> true,
                    SoundEvents.BUCKET_FILL
                );
            }
        );
        
        Identifier honey = AnvilCraft.of("honey");
        event.register(
            honey,
            Items.GLASS_BOTTLE,
            (state, level, pos, player, hand, stack) -> {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                Item item = stack.getItem();
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, Items.HONEY_BOTTLE.getDefaultInstance()));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(item));
                Layered4LevelCauldronBlock.lowerFillLevel(state, level, pos);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
                return InteractionResult.SUCCESS_SERVER;
            }
        );
        event.register(
            honey,
            Items.HONEY_BOTTLE,
            (state, level, pos, player, hand, stack) -> {
                if (ModBlocks.HONEY_CAULDRON.get().isFull(state)) return InteractionResult.PASS;
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                player.awardStat(Stats.FILL_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                level.setBlockAndUpdate(pos, state.cycle(HoneyCauldronBlock.LEVEL));
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS);
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                return InteractionResult.SUCCESS_SERVER;
            }
        );
        
        Identifier meltGem = AnvilCraft.of("melt_gem");
        event.register(
            meltGem,
            Items.BUCKET,
            (state, level, pos, player, hand, stack) -> CauldronInteractions.fillBucket(
                state,
                level,
                pos,
                player,
                hand,
                stack,
                ModItems.MELT_GEM_BUCKET.asStack(),
                _ -> true,
                SoundEvents.BUCKET_FILL
            )
        );
        
        Identifier fishTank = AnvilCraft.of("fish_tank");
        event.register(
            fishTank,
            Items.FLINT_AND_STEEL,
            (state, level, pos, player, hand, stack) -> {
                if (!Util.<FishTankBlock>cast(state.getBlock()).tryIgnite(level, pos)) return InteractionResult.FAIL;
                stack.hurtAndBreak(2, player, hand.asEquipmentSlot());
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS);
                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            }
        );
        event.register(
            fishTank,
            Items.FIRE_CHARGE,
            (state, level, pos, _, _, stack) -> {
                if (!Util.<FishTankBlock>cast(state.getBlock()).tryIgnite(level, pos)) return InteractionResult.FAIL;
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS);
                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            }
        );
        event.register(
            fishTank,
            ModItems.MULTITOOL_ITEM.asItem(),
            (state, level, pos, player, hand, stack) -> {
                if (MultitoolItem.getMode(stack) != MultitoolItem.FLINT_AND_STEEL_MODE) return InteractionResult.SUCCESS;
                if (!Util.<FishTankBlock>cast(state.getBlock()).tryIgnite(level, pos)) return InteractionResult.FAIL;
                stack.hurtAndBreak(2, player, hand.asEquipmentSlot());
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS);
                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            }
        );
        
        Identifier empty = Identifier.withDefaultNamespace("empty");
        ModItems.CEMENT_BUCKETS.forEach((k, v) -> event.register(
            empty,
            v.get(),
            (_, level, pos, player, hand, stack) -> CauldronInteractions.emptyBucket(
                level,
                pos,
                player,
                hand,
                stack,
                ModBlocks.CEMENT_CAULDRONS.get(k).getDefaultState(),
                SoundEvents.BUCKET_EMPTY
            )
        ));
        event.register(
            empty,
            ModItems.EXP_BUCKET.get(),
            (_, level, pos, player, hand, stack) -> CauldronInteractions.emptyBucket(
                level,
                pos,
                player,
                hand,
                stack,
                ModBlocks.EXP_FLUID_CAULDRON.get().fullFilled(),
                SoundEvents.BUCKET_EMPTY
            )
        );
        event.register(
            empty,
            Items.EXPERIENCE_BOTTLE,
            (_, level, pos, player, hand, stack) -> {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                if (level.getRandom().nextBoolean()) {
                    level.setBlockAndUpdate(pos, ModBlocks.EXP_FLUID_CAULDRON.getDefaultState());
                }
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS);
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                return InteractionResult.SUCCESS_SERVER;
            }
        );
        event.register(
            empty,
            ModItems.OIL_BUCKET.get(),
            (state, level, pos, player, hand, stack) -> switch (state) {
                case BlockState it when it.is(ModBlocks.OIL_CAULDRON) -> CauldronInteractions.emptyBucket(
                    level,
                    pos,
                    player,
                    hand,
                    stack,
                    ModBlocks.OIL_CAULDRON.get().fullFilled(),
                    SoundEvents.BUCKET_EMPTY
                );
                case BlockState it when it.is(ModBlocks.FIRE_CAULDRON) -> CauldronInteractions.emptyBucket(
                    level,
                    pos,
                    player,
                    hand,
                    stack,
                    ModBlocks.FIRE_CAULDRON.get().fullFilled(),
                    SoundEvents.BUCKET_EMPTY
                );
                case BlockState it when it.is(Blocks.CAULDRON) -> {
                    for (int i = 0; i < 6; i++) {
                        if (level.getBlockState(pos.offset(0, i, 0).immutable()).is(ModBlocks.PLASMA_JETS)) {
                            yield CauldronInteractions.emptyBucket(
                                level,
                                pos,
                                player,
                                hand,
                                stack,
                                ModBlocks.FIRE_CAULDRON.get().fullFilled(),
                                SoundEvents.BUCKET_EMPTY
                            );
                        }
                    }
                    yield CauldronInteractions.emptyBucket(
                        level,
                        pos,
                        player,
                        hand,
                        stack,
                        ModBlocks.OIL_CAULDRON.get().fullFilled(),
                        SoundEvents.BUCKET_EMPTY
                    );
                }
                default -> InteractionResult.PASS;
            }
        );
        event.register(
            empty,
            Items.HONEY_BOTTLE,
            (_, level, pos, player, hand, stack) -> {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                level.setBlockAndUpdate(pos, ModBlocks.HONEY_CAULDRON.getDefaultState());
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS);
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                return InteractionResult.SUCCESS_SERVER;
            }
        );
        event.register(
            empty,
            ModItems.MELT_GEM_BUCKET.get(),
            (_, level, pos, player, hand, stack) -> CauldronInteractions.emptyBucket(
                level,
                pos,
                player,
                hand,
                stack,
                ModBlocks.MELT_GEM_CAULDRON.getDefaultState(),
                SoundEvents.BUCKET_EMPTY
            )
        );

        Identifier obsidian = AnvilCraft.of("obsidian");
        event.register(
            obsidian,
            ModBlocks.MENGER_SPONGE.asItem(),
            (_, level, pos, _, _, _) -> {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                return InteractionResult.SUCCESS_SERVER;
            }
        );
    }
}
