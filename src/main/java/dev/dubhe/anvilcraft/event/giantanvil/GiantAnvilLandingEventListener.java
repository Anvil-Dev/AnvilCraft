package dev.dubhe.anvilcraft.event.giantanvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.anvil.GiantAnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.entity.HasMobBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.BlockCrushRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPattern;
import dev.dubhe.anvilcraft.recipe.multiblock.ModifySpawnerAction;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockInput;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dev.dubhe.anvilcraft.util.Util.HORIZONTAL_DIRECTIONS;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class GiantAnvilLandingEventListener {
    private static final List<IShockBehaviorDefinition> behaviorDefs = new ArrayList<>();
    private static final int MIN_MULTIBLOCK_SIZE = 3;
    private static final int MAX_MULTIBLOCK_SIZE = 3;

    static {
        behaviorDefs.add(new IShockBehaviorDefinition.MatchAll((blockPosList, level) -> {
            for (BlockPos pos : blockPosList) {
                BlockState state = level.getBlockState(pos);
                if (state.is(BlockTags.LEAVES)
                    || state.is(BlockTags.FLOWERS)
                    || state.is(Blocks.RED_MUSHROOM)
                    || state.canBeReplaced()
                    || state.is(Blocks.BROWN_MUSHROOM)
                    || state.is(BlockTags.SNOW)
                    || state.is(BlockTags.ICE)) {
                    LootParams.Builder builder = new LootParams.Builder((ServerLevel) level)
                        .withParameter(LootContextParams.ORIGIN, pos.getCenter());
                    builder.withParameter(LootContextParams.TOOL, ItemStack.EMPTY);
                    if (state.is(BlockTags.SNOW)) {
                        builder.withOptionalParameter(LootContextParams.BLOCK_ENTITY, null);
                        builder.withParameter(LootContextParams.THIS_ENTITY, new PrimedTnt(EntityType.TNT, level));
                        builder.withParameter(LootContextParams.TOOL, Items.DIAMOND_SHOVEL.getDefaultInstance());
                        builder.withParameter(LootContextParams.EXPLOSION_RADIUS, 4f);
                    }
                    for (ItemStack drop : state.getDrops(builder)) {
                        ItemEntity itemEntity =
                            new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                        level.addFreshEntity(itemEntity);
                        state.spawnAfterBreak((ServerLevel) level, pos, ItemStack.EMPTY, true);
                    }
                    level.destroyBlock(pos, false);
                }
            }
        }));
        behaviorDefs.add(new IShockBehaviorDefinition.SimpleTag(BlockTags.WOOL, (blockPosList, level) -> {
            for (BlockPos pos : blockPosList) {
                BlockState state = level.getBlockState(pos);
                if (state.is(BlockTags.LEAVES)
                    || state.is(BlockTags.FLOWERS)
                    || state.is(Blocks.RED_MUSHROOM)
                    || state.canBeReplaced()
                    || state.is(Blocks.BROWN_MUSHROOM)
                    || state.is(BlockTags.SNOW)
                    || state.is(BlockTags.ICE)) {
                    if (level instanceof ServerLevel serverLevel) {
                        List<ItemStack> drops;
                        if (state.is(Blocks.SNOW)) {
                            // Snow Layers don't drop any item when not broken by player
                            // so we need to special check here
                            drops = BreakBlockUtil.dropFromSnowLayers(state);
                        } else {
                            drops = BreakBlockUtil.dropSilkTouchOrShears(serverLevel, pos);
                        }
                        AnvilUtil.dropItems(drops, level, pos.getCenter());
                    }
                    level.destroyBlock(pos, false);
                }
                if (isFellingApplicableBlock(state)) {
                    removeLeaves(pos, level);
                }
            }
        }));
        behaviorDefs.add(new IShockBehaviorDefinition.SimpleTag(BlockTags.LOGS, (blockPosList, level) -> {
            for (BlockPos pos : blockPosList) {
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.SUGAR_CANE)
                    || state.is(Blocks.BAMBOO)
                    || state.is(Blocks.KELP)
                    || state.is(Blocks.CACTUS)) {
                    level.destroyBlock(pos, true);
                }
                processChorus(pos, state, level);
                if (isFellingApplicableBlock(state)) {
                    if (state.getBlock() instanceof ChorusPlantBlock) {
                        level.destroyBlock(pos, true);
                    }
                    BlockPos.breadthFirstTraversal(
                        pos,
                        Integer.MAX_VALUE,
                        1024,
                        Util::acceptDirections,
                        blockPos -> {
                            if (blockPos.getY() < pos.getY()) return false;
                            BlockState blockState = level.getBlockState(blockPos);
                            if (isFellingApplicableBlock(blockState)) {
                                level.destroyBlock(blockPos, true);
                                return true;
                            }
                            return false;
                        });
                }
            }
        }));
        behaviorDefs.add(new IShockBehaviorDefinition.SimpleBlock(Blocks.HAY_BLOCK, (blockPosList, level) -> {
            for (BlockPos pos : blockPosList) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof CropBlock cropBlock) {
                    if (cropBlock.isMaxAge(state)) {
                        level.destroyBlock(pos, true);
                        level.setBlockAndUpdate(pos, cropBlock.getStateForAge(0));
                    }
                }
                if (state.getBlock() instanceof SweetBerryBushBlock) {
                    level.destroyBlock(pos, true);
                    level.setBlockAndUpdate(pos, state.setValue(SweetBerryBushBlock.AGE, 0));
                }
                if (state.getBlock() instanceof CaveVinesBlock) {
                    level.destroyBlock(pos, true);
                    level.setBlockAndUpdate(pos, state.setValue(CaveVines.BERRIES, false));
                }
                if (state.getBlock() instanceof NetherWartBlock) {
                    level.destroyBlock(pos, true);
                    level.setBlockAndUpdate(pos, state.setValue(NetherWartBlock.AGE, 0));
                }
                processChorus(pos, state, level);
                processCocoaBeans(pos, state, level);
            }
        }));
        behaviorDefs.add(new IShockBehaviorDefinition.SimpleBlock(Blocks.ANVIL,
            (blockPosList, level) -> {
                for (BlockPos pos : blockPosList) {
                    BlockState state = level.getBlockState(pos);
                    BlockCrushRecipe.Input input = new BlockCrushRecipe.Input(state.getBlock());
                    level.getRecipeManager().getRecipeFor(ModRecipeTypes.BLOCK_CRUSH_TYPE.get(), input, level).ifPresent(recipe -> {
                        level.setBlockAndUpdate(pos, recipe.value().result.defaultBlockState());
                        level.destroyBlock(pos, true);
                    });
                }
            })
        );
    }

    private static void processChorus(BlockPos pos, BlockState state, Level level) {
        if (state.getBlock() instanceof ChorusPlantBlock) {
            BlockPos.breadthFirstTraversal(
                pos, Integer.MAX_VALUE, 1024, Util::acceptDirections, blockPos -> {
                    if (blockPos.getY() < pos.getY()) return false;
                    BlockState blockState = level.getBlockState(blockPos);
                    if (blockState.is(Blocks.CHORUS_PLANT)) {
                        level.destroyBlock(blockPos, true);
                        return true;
                    }
                    if (blockState.is(Blocks.CHORUS_FLOWER)) {
                        level.destroyBlock(blockPos, false);
                        AnvilUtil.dropItems(List.of(Blocks.CHORUS_FLOWER.asItem().getDefaultInstance()),
                            level,
                            blockPos.getCenter());
                        return true;
                    }
                    return false;
                });
        }
    }

    private static void processCocoaBeans(BlockPos pos, BlockState state, Level level) {
        if (state.is(BlockTags.JUNGLE_LOGS)) {
            BlockPos.breadthFirstTraversal(
                pos, Integer.MAX_VALUE, 1024, Util::acceptDirections, blockPos -> {
                    BlockState blockState = level.getBlockState(blockPos);
                    if (blockState.is(Blocks.COCOA)) {
                        if (blockState.getValue(CocoaBlock.AGE) >= 2) {
                            level.destroyBlock(blockPos, true);
                            level.setBlock(
                                blockPos,
                                blockState.setValue(CocoaBlock.AGE, 0),
                                Block.UPDATE_ALL_IMMEDIATE
                            );
                        }
                        return true;
                    }
                    if (blockState.is(BlockTags.JUNGLE_LOGS)) {
                        return true;
                    }
                    return false;
                });
        }
    }

    private static void removeLeaves(BlockPos pos, Level level) {
        BlockPos.breadthFirstTraversal(
            pos, Integer.MAX_VALUE, 1024, Util::acceptDirections, blockPos -> {
                if (blockPos.getY() < pos.getY()) return false;
                BlockState blockState = level.getBlockState(blockPos);
                if (isFellingApplicableBlock(blockState)) {
                    if (isMushroomBlock(blockState)) {
                        level.destroyBlock(blockPos, false);
                        AnvilUtil.dropItems(List.of(blockState.getBlock().asItem().getDefaultInstance()),
                            level,
                            blockPos.getCenter());
                        return true;
                    }
                    if (!blockState.is(BlockTags.LOGS)) {
                        level.destroyBlock(blockPos, false);
                        AnvilUtil.dropItems(List.of(blockState.getBlock().asItem().getDefaultInstance()),
                            level,
                            blockPos.getCenter());
                    }
                    return true;
                }
                return false;
            });
    }

    private static boolean isFellingApplicableBlock(BlockState blockState) {
        return blockState.is(BlockTags.LOGS)
            || (blockState.is(BlockTags.LEAVES) && !blockState.getValue(LeavesBlock.PERSISTENT))
            || blockState.is(Blocks.MANGROVE_ROOTS)
            || blockState.is(ModBlockTags.MUSHROOM_BLOCK)
            || blockState.is(BlockTags.WART_BLOCKS)
            || blockState.is(Blocks.SHROOMLIGHT);
    }

    private static boolean isMushroomBlock(BlockState blockState) {
        return blockState.is(ModBlockTags.MUSHROOM_BLOCK)
            || blockState.is(BlockTags.WART_BLOCKS)
            || blockState.is(Blocks.SHROOMLIGHT);
    }

    /**
     * 撼地
     */
    @SubscribeEvent
    public static void onLand(@NotNull GiantAnvilFallOnLandEvent event) {
        BlockPos groundPos = event.getPos().below(2);
        if (!isValidShockBaseBlock(groundPos, event.getLevel())) return;
        behaviorDefs.stream()
            .filter(it -> it.cornerMatches(groundPos, event.getLevel()))
            .min((a, b) -> b.priority() - a.priority())
            .ifPresent(def -> {
                int radius = (int) Math.min(Math.ceil(event.getFallDistance()), AnvilCraft.config.giantAnvilMaxShockRadius);
                BlockPos ground = groundPos.above();
                AABB aabb = AABB.ofSize(Vec3.atCenterOf(ground), radius * 2 + 1, 1, radius * 2 + 1);
                List<LivingEntity> e = event.getLevel().getEntitiesOfClass(LivingEntity.class, aabb);
                for (LivingEntity l : e) {
                    if (l.getItemBySlot(EquipmentSlot.FEET).is(Items.AIR)) {
                        l.hurt(event.getLevel().damageSources().fall(), event.getFallDistance() * 2);
                    }
                }
                ArrayList<BlockPos> posList = new ArrayList<>();
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        BlockPos pos = ground.offset(dx, 0, dz);
                        posList.add(pos);
                    }
                }
                def.acceptRanges(posList, event.getLevel());
            });
    }

    /**
     * 在一个边长为 {@code size} 的立方体区域中，绕着中心将 {@code pos} 旋转到对应位置。
     *
     * @param pos      被旋转的方块坐标 (从 {@code (0, 0, 0)} 到 {@code （size - 1, size - 1, size - 1)})
     * @param size     立方体区域的边长
     * @param rotation 旋转操作
     * @return 旋转后的相对坐标
     */
    private static BlockPos rotatePos(BlockPos pos, int size, Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90 -> new BlockPos(pos.getZ(), pos.getY(), size - 1 - pos.getX());
            case CLOCKWISE_180 -> new BlockPos(size - 1 - pos.getX(), pos.getY(), size - 1 - pos.getZ());
            case CLOCKWISE_90 -> new BlockPos(size - 1 - pos.getZ(), pos.getY(), pos.getX());
            default -> pos;
        };
    }

    @SubscribeEvent
    public static void handleMultiblock(@NotNull GiantAnvilFallOnLandEvent event) {
        Level level = event.getLevel();
        BlockPos landPos = event.getPos().below(2);

        BlockState centerState = level.getBlockState(landPos);
        boolean overCompressorDetected = false;
        if (centerState.is(ModBlocks.SPACE_OVERCOMPRESSOR)) {
            overCompressorDetected = true;
        } else if (!centerState.is(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES)) {
            return;
        }
        int size = findCraftingTableSize(landPos, level);
        if (size < 3 || size > 15) return;

        BlockPos inputCorner = landPos.offset(-size / 2, -size, -size / 2);

        List<List<List<BlockState>>> blocks = new ArrayList<>();
        for (int y = 0; y < size; y++) {
            List<List<BlockState>> blocksY = new ArrayList<>();
            for (int z = 0; z < size; z++) {
                List<BlockState> blocksZ = new ArrayList<>();
                for (int x = 0; x < size; x++) {
                    BlockState state = level.getBlockState(inputCorner.offset(x, y, z));
                    blocksZ.add(state);
                }
                blocksY.add(blocksZ);
            }
            blocks.add(blocksY);
        }
        MultiblockInput input = new MultiblockInput(blocks, size);
        if (overCompressorDetected) {
            level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.MULTIBLOCK_TYPE.get(), input, level)
                .ifPresent(recipe -> {
                    ItemStack result = recipe.value().getResult().copy();
                    for (int y = 0; y < size; y++) {
                        for (int z = 0; z < size; z++) {
                            for (int x = 0; x < size; x++) {
                                level.setBlockAndUpdate(
                                    inputCorner.offset(x, y, z),
                                    Blocks.AIR.defaultBlockState());
                            }
                        }
                    }
                    AnvilUtil.dropItems(
                        List.of(result),
                        level,
                        landPos.relative(Direction.Axis.Y, -size / 2).getCenter());
                });
            return;
        }
        level.getRecipeManager()
            .getRecipeFor(ModRecipeTypes.MULTIBLOCK_CONVERSION_TYPE.get(), input, level)
            .ifPresent(recipe -> {
                MultiblockConversionRecipe value = recipe.value();
                Rotation rotation = value.getMatchedRotation();
                BlockPattern outputPattern = value.getOutputPattern();
                BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
                Optional<EntityType<?>> entity = value.getModifySpawnerAction()
                    .map(ModifySpawnerAction::fromPos)
                    .map(pos -> rotatePos(pos, size, rotation))
                    .map(inputCorner::offset)
                    .map(level::getBlockEntity)
                    .filter(be -> be instanceof HasMobBlockEntity)
                    .map(be -> ((HasMobBlockEntity) be).getOrCreateDisplayEntity(level))
                    .map(Entity::getType);
                for (int y = 0; y < size; y++) {
                    for (int z = 0; z < size; z++) {
                        for (int x = 0; x < size; x++) {
                            switch (rotation) {
                                case COUNTERCLOCKWISE_90 -> mpos.setWithOffset(inputCorner, z, y, size - 1 - x);
                                case CLOCKWISE_180 -> mpos.setWithOffset(inputCorner, size - 1 - x, y, size - 1 - z);
                                case CLOCKWISE_90 -> mpos.setWithOffset(inputCorner, size - 1 - z, y, x);
                                default -> mpos.setWithOffset(inputCorner, x, y, z);
                            }
                            BlockState newState = outputPattern.getPredicate(x, y, z).getDefaultState().rotate(rotation);
                            level.setBlock(mpos, newState, 18);
                        }
                    }
                }
                // NC update (Block#neighborChanged) after structure converted
                for (int y = 0; y < size; y++) {
                    for (int z = 0; z < size; z++) {
                        for (int x = 0; x < size; x++) {
                            if (x > 0 && x < size - 1 && y > 0 && y < size - 1 && z > 0 && z < size - 1) continue;
                            mpos.setWithOffset(inputCorner, x, y, z);
                            level.blockUpdated(mpos, input.getBlockState(x, y, z).getBlock());
                            BlockState newState = level.getBlockState(mpos);
                            if (newState.hasAnalogOutputSignal()) {
                                level.updateNeighbourForOutputSignal(mpos, newState.getBlock());
                            }
                        }
                    }
                }
                // PP update (Block#updateShape) after structure converted
                // copy and modified from StructureTemplate#updateShapeAtEdge
                DiscreteVoxelShape shape = BitSetDiscreteVoxelShape.withFilledBounds(
                    size, size, size,
                    0, 0, 0,
                    size, size, size
                );
                BlockPos.MutableBlockPos mpos2 = new BlockPos.MutableBlockPos();
                shape.forAllFaces(
                    (direction, x, y, z) -> {
                        BlockPos innerPos = mpos.setWithOffset(inputCorner, x, y, z);
                        BlockPos outerPos = mpos2.setWithOffset(innerPos, direction);
                        BlockState innerState = level.getBlockState(innerPos);
                        if (innerState != input.getBlockState(x, y, z)) {
                            level.neighborShapeChanged(direction.getOpposite(), level.getBlockState(innerPos),
                                outerPos, innerPos, 3, 512);
                        }
                        level.neighborShapeChanged(direction, level.getBlockState(outerPos),
                            innerPos, outerPos, 3, 512);
                    }
                );
                entity.ifPresent(entityType -> {
                    BlockPos offset = rotatePos(value.getModifySpawnerAction().get().toPos(), size, rotation);
                    Optional.ofNullable(level.getBlockEntity(inputCorner.offset(offset)))
                        .filter(be -> be instanceof Spawner)
                        .ifPresent(be -> ((Spawner) be).setEntityId(entityType, level.getRandom()));
                });
            });
    }

    private static boolean isValidShockBaseBlock(BlockPos centerPos, Level level) {
        BlockState blockState = level.getBlockState(centerPos);
        if (!blockState.is(ModBlocks.HEAVY_IRON_BLOCK.get())) {
            return false;
        }
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            if (!level.getBlockState(centerPos.relative(direction)).is(ModBlocks.HEAVY_IRON_BLOCK)) return false;
        }
        return true;
    }

    private static int findCraftingTableSize(BlockPos centerPos, Level level) {
        int maxSize = 0;
        for (int size = MIN_MULTIBLOCK_SIZE; size <= MAX_MULTIBLOCK_SIZE; size += 2) {
            boolean flag = true;
            for (int x = -size / 2; x <= size / 2 && flag; x++) {
                for (int z = -size / 2; z <= size / 2 && flag; z++) {
                    if (x == 0 && z == 0) continue;
                    BlockPos pos = centerPos.offset(x, 0, z);
                    if (!level.getBlockState(pos).is(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES)) {
                        flag = false;
                    }
                }
            }
            if (flag) {
                maxSize = size;
            } else {
                break;
            }
        }
        return maxSize;
    }
}
