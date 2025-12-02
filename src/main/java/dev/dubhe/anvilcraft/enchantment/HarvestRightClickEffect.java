package dev.dubhe.anvilcraft.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.CaveVinesPlantBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import javax.annotation.Nullable;

public record HarvestRightClickEffect(int range) implements EnchantmentEntityEffect {
    public static final MapCodec<HarvestRightClickEffect> CODEC = RecordCodecBuilder.mapCodec(it ->
        it.group(
            Codec.INT.optionalFieldOf("range", 3)
                .forGetter(HarvestRightClickEffect::range)
        ).apply(it, HarvestRightClickEffect::new)
    );

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse enchantedItemInUse, Entity entity, Vec3 vec3) {
        ItemStack itemStack = enchantedItemInUse.itemStack();
        BlockPos pos = BlockPos.containing(vec3);
        BlockState state = level.getBlockState(pos);
        final int radius = Math.min(enchantmentLevel, 3);
        if (!(entity instanceof LivingEntity livingEntity)) {
            return;
        }
        if (!itemStack.is(ItemTags.HOES)) {
            return;
        }
        if (harvestable(state) == null) {
            return;
        }
        Iterable<BlockPos> posIterable = BlockPos.betweenClosed(pos.offset(radius, radius, radius), pos.offset(-radius, -radius, -radius));
        for (BlockPos blockPos : posIterable) {
            BlockState blockState = level.getBlockState(blockPos);
            Block harvestableBlock = harvestable(blockState);
            if (harvestableBlock == null) {
                continue;
            }
            switch (harvestableBlock) {
                case CropBlock cropBlock -> {
                    if (cropBlock.isMaxAge(blockState)) {
                        level.setBlockAndUpdate(blockPos, cropBlock.getStateForAge(0));
                        itemStack.hurtAndBreak(1, level, livingEntity, enchantedItemInUse.onBreak());
                        List<ItemStack> drops = Block.getDrops(blockState, level, blockPos, null, livingEntity, itemStack);
                        for (ItemStack dropItem : drops) {
                            Block.popResource(level, blockPos, dropItem);
                        }
                    }
                }
                case NetherWartBlock ignored -> {
                    if (blockState.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE) {
                        level.setBlockAndUpdate(blockPos, blockState.setValue(NetherWartBlock.AGE, 0));
                        itemStack.hurtAndBreak(1, level, livingEntity, enchantedItemInUse.onBreak());
                        List<ItemStack> drops = Block.getDrops(blockState, level, blockPos, null, livingEntity, itemStack);
                        for (ItemStack dropItem : drops) {
                            Block.popResource(level, blockPos, dropItem);
                        }
                    }
                }
                case CocoaBlock ignored -> {
                    if (blockState.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE) {
                        level.setBlockAndUpdate(blockPos, blockState.setValue(CocoaBlock.AGE, 0));
                        itemStack.hurtAndBreak(1, level, livingEntity, enchantedItemInUse.onBreak());
                        List<ItemStack> drops = Block.getDrops(blockState, level, blockPos, null, livingEntity, itemStack);
                        for (ItemStack dropItem : drops) {
                            Block.popResource(level, blockPos, dropItem);
                        }
                    }
                }
                case SweetBerryBushBlock ignored -> {
                    if (blockPos.equals(pos)) {
                        continue;
                    }
                    int age = blockState.getValue(SweetBerryBushBlock.AGE);
                    boolean isMaxAge = age == SweetBerryBushBlock.MAX_AGE;
                    if (age > 1) {
                        int i = 1 + level.random.nextInt(2);
                        Block.popResource(level, blockPos, new ItemStack(Items.SWEET_BERRIES, i + (isMaxAge ? 1 : 0)));
                        level.playSound(
                            null,
                            blockPos,
                            SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                            SoundSource.BLOCKS,
                            1,
                            0.8f + level.random.nextFloat() * 0.4f
                        );
                        BlockState blockState1 = blockState.setValue(SweetBerryBushBlock.AGE, 1);
                        level.setBlock(blockPos, blockState1, 2);
                        level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(livingEntity, blockState));
                    }
                }
                case CaveVinesBlock ignored -> {
                    if (blockPos.equals(pos)) {
                        continue;
                    }
                    CaveVines.use(livingEntity, blockState, level, blockPos);
                }
                case CaveVinesPlantBlock ignored -> {
                    if (blockPos.equals(pos)) {
                        continue;
                    }
                    CaveVines.use(livingEntity, blockState, level, blockPos);
                }
                default -> {}
            }
        }
    }

    @Nullable
    private Block harvestable(BlockState state) {
        if (
            state.getBlock() instanceof CropBlock
            || state.is(Blocks.NETHER_WART)
            || state.is(Blocks.COCOA)
            || state.is(Blocks.SWEET_BERRY_BUSH)
            || state.is(Blocks.CAVE_VINES)
            || state.is(Blocks.CAVE_VINES_PLANT)
        ) {
            return state.getBlock();
        }
        return null;
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }
}
