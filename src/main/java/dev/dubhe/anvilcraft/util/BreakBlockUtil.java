package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class BreakBlockUtil {
    public static List<ItemStack> dropWithTool(ServerLevel level, BlockPos pos, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return List.of();
        return dropWithTool(level, pos, state, level.getBlockEntity(pos), tool);
    }

    private static List<ItemStack> dropWithTool(
        ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool
    ) {
        ServerPlayer fakePlayer = AnvilCraftFakePlayers.getDestroyer().offerPlayer(level);
        AnvilCraftFakePlayers.getDestroyer().enabledDestroy(fakePlayer, tool);
        try {
            LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, tool)
                .withOptionalParameter(LootContextParams.THIS_ENTITY, fakePlayer)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
            return state.getDrops(builder);
        } finally {
            AnvilCraftFakePlayers.getDestroyer().disable(fakePlayer);
        }
    }

    public static List<ItemStack> drop(ServerLevel level, BlockPos pos, BlockMiningEffect effect) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return List.of();
        return drop(level, pos, state, level.getBlockEntity(pos), effect, null);
    }

    public static List<ItemStack> drop(
        ServerLevel level, BlockPos pos, BlockState state, BlockMiningEffect effect, @Nullable ItemStack baseTool
    ) {
        return drop(level, pos, state, level.getBlockEntity(pos), effect, baseTool);
    }

    private static List<ItemStack> drop(
        ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
        BlockMiningEffect effect, @Nullable ItemStack baseTool
    ) {
        ItemStack tool = baseTool == null ? createTool(level, state, effect) : effect.applyTo(level, baseTool);
        return dropWithTool(level, pos, state, blockEntity, tool);
    }

    public static List<ItemStack> dropVirtual(
        ServerLevel level, BlockPos origin, BlockState state, BlockMiningEffect effect
    ) {
        return drop(level, origin, state, null, effect, null);
    }

    public static List<ItemStack> dropForLaser(
        ServerLevel level, BlockPos pos, BlockMiningEffect effect
    ) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return List.of();
        return dropForLaser(level, pos, state, level.getBlockEntity(pos), effect);
    }

    private static List<ItemStack> dropForLaser(
        ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, BlockMiningEffect effect
    ) {
        if (effect.isDisintegration()) {
            return level.random.nextFloat() <= 0.25f
                   ? List.of(ModItems.EXP_GEM.asStack())
                   : List.of();
        }
        return drop(level, pos, state, blockEntity, effect, null);
    }

    public static List<ItemStack> dropVirtualForLaser(
        ServerLevel level, BlockPos origin, BlockState state, BlockMiningEffect effect
    ) {
        return dropForLaser(level, origin, state, null, effect);
    }

    public static Optional<BlockState> findOreForRawMaterial(
        ServerLevel level, BlockPos origin, ItemStack rawMaterial
    ) {
        for (TagKey<Item> rawMaterialTag : rawMaterial.getItem().builtInRegistryHolder().tags().toList()) {
            ResourceLocation tagLocation = rawMaterialTag.location();
            if (!tagLocation.getNamespace().equals("c") || !tagLocation.getPath().startsWith("raw_materials/")) {
                continue;
            }
            String material = tagLocation.getPath().substring("raw_materials/".length());
            TagKey<Item> oreTag = TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("c", "ores/" + material)
            );
            for (Holder<Item> oreHolder : BuiltInRegistries.ITEM.getTagOrEmpty(oreTag)) {
                Block ore = Block.byItem(oreHolder.value());
                if (ore == Blocks.AIR) continue;
                BlockState oreState = ore.defaultBlockState();
                List<ItemStack> normalDrops = dropVirtual(level, origin, oreState, BlockMiningEffect.NORMAL);
                if (normalDrops.stream().anyMatch(stack -> stack.is(rawMaterial.getItem()))) {
                    return Optional.of(oreState);
                }
            }
        }
        return Optional.empty();
    }

    public static List<ItemStack> dropSilkTouch(ServerLevel level, BlockPos pos) {
        return drop(level, pos, BlockMiningEffect.SILK_TOUCH);
    }

    public static ItemStack createTool(ServerLevel level, BlockState state, BlockMiningEffect effect) {
        ItemStack tool = state.is(Blocks.SNOW)
            ? Items.NETHERITE_SHOVEL.getDefaultInstance()
            : Items.NETHERITE_PICKAXE.getDefaultInstance();
        tool.set(DataComponents.CUSTOM_NAME, Component.literal("AnvilCraft Mining Tool"));
        return effect.applyTo(level, tool);
    }

    public static int getExperience(
        ServerLevel level, BlockPos pos, BlockState state, BlockMiningEffect effect
    ) {
        ItemStack tool = createTool(level, state, effect);
        ServerPlayer fakePlayer = AnvilCraftFakePlayers.getDestroyer().offerPlayer(level);
        AnvilCraftFakePlayers.getDestroyer().enabledDestroy(fakePlayer, tool);
        try {
            int experience = state.getExpDrop(level, pos, level.getBlockEntity(pos), fakePlayer, tool);
            return EnchantmentHelper.processBlockExperience(level, tool, experience);
        } finally {
            AnvilCraftFakePlayers.getDestroyer().disable(fakePlayer);
        }
    }

    public static void dropExperience(
        ServerLevel level, BlockPos pos, BlockState state, BlockMiningEffect effect
    ) {
        ExperienceOrb.award(level, pos.getCenter(), getExperience(level, pos, state, effect));
    }
}
