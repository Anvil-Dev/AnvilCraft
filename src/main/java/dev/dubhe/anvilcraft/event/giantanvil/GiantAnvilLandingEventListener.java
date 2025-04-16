package dev.dubhe.anvilcraft.event.giantanvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.behavior.BehaviorTree;
import dev.dubhe.anvilcraft.api.behavior.TreeNode;
import dev.dubhe.anvilcraft.api.event.anvil.GiantAnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.SpectralAnvilBlock;
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
import net.minecraft.world.level.block.AnvilBlock;
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
import java.util.function.Function;

import static dev.dubhe.anvilcraft.util.Util.HORIZONTAL_DIRECTIONS;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class GiantAnvilLandingEventListener {
    private static final int MIN_MULTIBLOCK_SIZE = 3;
    private static final int MAX_MULTIBLOCK_SIZE = 15;

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
