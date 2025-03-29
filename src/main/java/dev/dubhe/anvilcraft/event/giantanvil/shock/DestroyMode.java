package dev.dubhe.anvilcraft.event.giantanvil.shock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

enum DestroyMode {
    NORMAL {
        public static final ItemStack TOOL = Items.NETHERITE_PICKAXE.getDefaultInstance();

        @Override
        public List<ItemStack> apply(BlockState blockState, BlockPos blockPos, ShockContext shockContext) {
            return blockState.getDrops(
                new LootParams.Builder((ServerLevel) shockContext.level())
                    .withParameter(LootContextParams.ORIGIN, blockPos.getCenter())
                    .withParameter(LootContextParams.TOOL, TOOL)
                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, shockContext.level().getBlockEntity(blockPos))
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, FakePlayerSupport.get((ServerLevel) shockContext.level()))
            );
        }
    },
    SILK_TOUCH {
        public static ItemStack TOOL;
        public static ItemStack FOR_SNOW_TOOL;

        @Override
        public List<ItemStack> apply(BlockState blockState, BlockPos blockPos, ShockContext shockContext, ItemStack tool) {
            LootParams.Builder builder = new LootParams.Builder((ServerLevel) shockContext.level())
                .withParameter(LootContextParams.ORIGIN, blockPos.getCenter())
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, shockContext.level().getBlockEntity(blockPos))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, FakePlayerSupport.get((ServerLevel) shockContext.level()))
                .withParameter(LootContextParams.TOOL, tool);
            return blockState.getDrops(builder);
        }

        @Override
        public List<ItemStack> apply(BlockState blockState, BlockPos blockPos, ShockContext shockContext) {
            createTool((ServerLevel) shockContext.level());
            LootParams.Builder builder = new LootParams.Builder((ServerLevel) shockContext.level())
                .withParameter(LootContextParams.ORIGIN, blockPos.getCenter())
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, shockContext.level().getBlockEntity(blockPos))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, FakePlayerSupport.get((ServerLevel) shockContext.level()));
            if (blockState.is(Blocks.SNOW)) {
                builder.withParameter(LootContextParams.TOOL, FOR_SNOW_TOOL);
            } else {
                builder.withParameter(LootContextParams.TOOL, TOOL);
            }
            return blockState.getDrops(builder);
        }

        private void createTool(ServerLevel serverLevel) {
            if (TOOL == null) {
                ItemStack itemStack = Items.NETHERITE_PICKAXE.getDefaultInstance();
                itemStack.enchant(serverLevel.holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
                TOOL = itemStack;
            }
            if (FOR_SNOW_TOOL == null) {
                ItemStack itemStack = Items.NETHERITE_SHOVEL.getDefaultInstance();
                itemStack.enchant(serverLevel.holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
                FOR_SNOW_TOOL = itemStack;
            }
        }
    },
    AUTO_SMELTING {
        public static final ItemStack TOOL = Items.NETHERITE_PICKAXE.getDefaultInstance();

        @Override
        public List<ItemStack> apply(BlockState blockState, BlockPos blockPos, ShockContext shockContext) {
            RecipeManager recipeManager = ServerLifecycleHooks.getCurrentServer().getRecipeManager();
            List<ItemStack> itemStacks = new ArrayList<>();
            for (ItemStack it : blockState.getDrops(
                new LootParams.Builder((ServerLevel) shockContext.level())
                    .withParameter(LootContextParams.ORIGIN, blockPos.getCenter())
                    .withParameter(LootContextParams.TOOL, TOOL)
                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, shockContext.level().getBlockEntity(blockPos))
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, FakePlayerSupport.get((ServerLevel) shockContext.level()))
            )) {
                SingleRecipeInput input = new SingleRecipeInput(it);
                ItemStack itemStack = recipeManager.getRecipeFor(
                        RecipeType.SMELTING,
                        input,
                        shockContext.level()
                    ).map(it1 -> it1.value().assemble(input, shockContext.level().registryAccess()))
                    .orElse(it);
//                if (itemStack.getItem() != Items.AIR && itemStack.getCount() == 0) {
//                    itemStack.setCount(1);
//                }
                itemStacks.add(itemStack);
            }
            return itemStacks;
        }
    };

    public abstract List<ItemStack> apply(BlockState blockState, BlockPos blockPos, ShockContext shockContext);

    public List<ItemStack> apply(BlockState blockState, BlockPos blockPos, ShockContext shockContext, ItemStack tool) {
        return apply(blockState, blockPos, shockContext);
    }
}



