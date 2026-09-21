package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

final class BlueprintSpecialBlocks {
    private BlueprintSpecialBlocks() {
    }

    static ItemStack material(BlockState state) {
        if (state.is(ModBlocks.SIMPLE_MAGNETIC_CHUTE.get())) return ModBlocks.MAGNETIC_CHUTE.asStack();
        if (state.getBlock() instanceof FlowerPotBlock) return new ItemStack(Items.FLOWER_POT);
        if (state.getBlock() instanceof CandleCakeBlock) return new ItemStack(Items.CAKE);
        if (state.is(Blocks.ATTACHED_PUMPKIN_STEM)) return new ItemStack(Items.PUMPKIN_SEEDS);
        if (state.is(Blocks.ATTACHED_MELON_STEM)) return new ItemStack(Items.MELON_SEEDS);
        if (state.is(Blocks.TALL_SEAGRASS)) return new ItemStack(Items.SEAGRASS);
        if (state.is(Blocks.KELP_PLANT)) return new ItemStack(Items.KELP);
        if (state.is(Blocks.BAMBOO_SAPLING)) return new ItemStack(Items.BAMBOO);
        if (state.is(Blocks.WEEPING_VINES_PLANT)) return new ItemStack(Items.WEEPING_VINES);
        if (state.is(Blocks.TWISTING_VINES_PLANT)) return new ItemStack(Items.TWISTING_VINES);
        if (state.is(Blocks.CAVE_VINES_PLANT)) return new ItemStack(Items.GLOW_BERRIES);
        if (state.is(Blocks.FROSTED_ICE)) return new ItemStack(Items.ICE);
        return ItemStack.EMPTY;
    }

    static List<ItemStack> extra(BlockState state) {
        if (state.getBlock() instanceof FlowerPotBlock pot && pot.getPotted() != Blocks.AIR) {
            return List.of(new ItemStack(pot.getPotted()));
        }
        if (state.getBlock() instanceof CandleCakeBlock) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            ResourceLocation candle = id.withPath(id.getPath().replace("_cake", ""));
            return List.of(new ItemStack(BuiltInRegistries.ITEM.get(candle)));
        }
        if (state.is(Blocks.TALL_SEAGRASS)) return List.of(new ItemStack(Items.BONE_MEAL));
        if (state.is(Blocks.END_PORTAL_FRAME) && state.getValue(EndPortalFrameBlock.HAS_EYE)) {
            return List.of(new ItemStack(Items.ENDER_EYE));
        }
        return List.of();
    }
}
