package dev.dubhe.anvilcraft.event.giantanvil.shock;

import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class DestroyMode {
    public static final DestroyMode NORMAL = new DestroyMode() {
        public static final ItemStack TOOL = Items.NETHERITE_PICKAXE.getDefaultInstance();

        @Override
        public List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx) {
            return state.getDrops(
                new LootParams.Builder((ServerLevel) ctx.level())
                    .withParameter(LootContextParams.ORIGIN, pos.getCenter())
                    .withParameter(LootContextParams.TOOL, TOOL)
                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, ctx.level().getBlockEntity(pos))
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, FakePlayerSupport.get((ServerLevel) ctx.level()))
            );
        }
    };

    public static final DestroyMode SILK_TOUCH = new DestroyMode() {
        public static ItemStack TOOL;
        public static ItemStack FOR_SNOW_TOOL;

        @Override
        public List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx, ItemStack tool) {
            LootParams.Builder builder = new LootParams.Builder((ServerLevel) ctx.level())
                .withParameter(LootContextParams.ORIGIN, pos.getCenter())
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, ctx.level().getBlockEntity(pos))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, FakePlayerSupport.get((ServerLevel) ctx.level()))
                .withParameter(LootContextParams.TOOL, tool);
            return state.getDrops(builder);
        }

        @Override
        public List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx) {
            createTool((ServerLevel) ctx.level());
            LootParams.Builder builder = new LootParams.Builder((ServerLevel) ctx.level())
                .withParameter(LootContextParams.ORIGIN, pos.getCenter())
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, ctx.level().getBlockEntity(pos))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, FakePlayerSupport.get((ServerLevel) ctx.level()));
            if (state.is(Blocks.SNOW)) {
                builder.withParameter(LootContextParams.TOOL, FOR_SNOW_TOOL);
            } else {
                builder.withParameter(LootContextParams.TOOL, TOOL);
            }
            return state.getDrops(builder);
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
    };

    public static final DestroyMode AUTO_SMELTING = new DestroyMode() {
        public static final ItemStack TOOL = Items.NETHERITE_PICKAXE.getDefaultInstance();

        @Override
        public List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx) {
            RecipeManager recipeManager = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer()).getRecipeManager();
            List<ItemStack> itemStacks = new ArrayList<>();
            for (ItemStack it : state.getDrops(
                new LootParams.Builder((ServerLevel) ctx.level())
                    .withParameter(LootContextParams.ORIGIN, pos.getCenter())
                    .withParameter(LootContextParams.TOOL, TOOL)
                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, ctx.level().getBlockEntity(pos))
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, FakePlayerSupport.get((ServerLevel) ctx.level()))
            )) {
                SingleRecipeInput input = new SingleRecipeInput(it);
                ItemStack itemStack = recipeManager.getRecipeFor(
                        RecipeType.SMELTING,
                        input,
                        ctx.level()
                    ).map(it1 -> it1.value().assemble(input, ctx.level().registryAccess()))
                    .orElse(it);
                // if (itemStack.getItem() != Items.AIR && itemStack.getCount() == 0) {
                //     itemStack.setCount(1);
                // }
                itemStacks.add(itemStack);
            }
            return itemStacks;
        }
    };

    public static final DestroyMode FORTUNE = new DestroyMode() {
        public static ItemStack TOOL;
        public static ItemStack FOR_SNOW_TOOL;

        @Override
        public List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx, ItemStack tool) {
            LootParams.Builder builder = new LootParams.Builder((ServerLevel) ctx.level())
                .withParameter(LootContextParams.ORIGIN, pos.getCenter())
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, ctx.level().getBlockEntity(pos))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, FakePlayerSupport.get((ServerLevel) ctx.level()))
                .withParameter(LootContextParams.TOOL, tool);
            return state.getDrops(builder);
        }

        @Override
        public List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx) {
            createTool((ServerLevel) ctx.level());
            LootParams.Builder builder = new LootParams.Builder((ServerLevel) ctx.level())
                .withParameter(LootContextParams.ORIGIN, pos.getCenter())
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, ctx.level().getBlockEntity(pos))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, FakePlayerSupport.get((ServerLevel) ctx.level()));
            if (state.is(Blocks.SNOW)) {
                builder.withParameter(LootContextParams.TOOL, FOR_SNOW_TOOL);
            } else {
                builder.withParameter(LootContextParams.TOOL, TOOL);
            }
            return state.getDrops(builder);
        }

        private void createTool(ServerLevel serverLevel) {
            if (TOOL == null) {
                ItemStack itemStack = Items.NETHERITE_PICKAXE.getDefaultInstance();
                itemStack.enchant(serverLevel.holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 5);
                TOOL = itemStack;
            }
            if (FOR_SNOW_TOOL == null) {
                ItemStack itemStack = Items.NETHERITE_SHOVEL.getDefaultInstance();
                itemStack.enchant(serverLevel.holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 5);
                FOR_SNOW_TOOL = itemStack;
            }
        }
    };

    public static final DestroyMode DISINTEGRATION = new DestroyMode() {
        @Override
        public List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx, ItemStack tool) {
            this.dropExp(ctx.level(), pos, state);
            return List.of();
        }

        @Override
        public List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx) {
            this.dropExp(ctx.level(), pos, state);
            return List.of();
        }

        private void dropExp(Level level, BlockPos pos, BlockState state) {
            if (!(level instanceof ServerLevel serverLevel)) return;
            ServerPlayer destroyer = AnvilCraftFakePlayers.anvilcraftDestroyer.offerPlayer(serverLevel);
            ItemStack dummyTool = BreakBlockUtil.getDummyDisintegrationTool(serverLevel);
            AnvilCraftFakePlayers.anvilcraftDestroyer.enabledDestroy(destroyer, dummyTool);
            ExperienceOrb.award(
                serverLevel,
                pos.getCenter(),
                EnchantmentHelper.processBlockExperience(
                    serverLevel,
                    dummyTool,
                    state.getExpDrop(level, pos, level.getBlockEntity(pos), destroyer, dummyTool)
                )
            );
            AnvilCraftFakePlayers.anvilcraftDestroyer.disable(destroyer);
        }
    };

    public DestroyMode() {

    }

    public abstract List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx);

    public List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx, ItemStack tool) {
        return apply(state, pos, ctx);
    }
}



