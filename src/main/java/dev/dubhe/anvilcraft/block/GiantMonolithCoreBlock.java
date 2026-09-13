package dev.dubhe.anvilcraft.block;

import dev.anvilcraft.lib.v2.registrum.providers.loot.RegistrumBlockLootTables;
import dev.dubhe.anvilcraft.block.entity.MonolithCoreBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyRecipe;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.util.DataGenUtil;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import javax.annotation.Nullable;

/**
 * 巨型石碑芯（giant_monolith_core）：按 3x3x3 多方块落地（同巨型铁砧），
 * 仅中层中心部件显示整体模型，其余 26 个部件为无碰撞的透明占位。
 * 东西/南北两种朝向沿用水平轴属性，模型默认为南北朝向。
 */
public class GiantMonolithCoreBlock extends SimpleMultiPartBlock<Cube3x3PartHalf> implements EntityBlock {
    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);
    public static final EnumProperty<GiantAnvilCube> CUBE = EnumProperty.create("cube", GiantAnvilCube.class);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public GiantMonolithCoreBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(HALF, Cube3x3PartHalf.BOTTOM_CENTER)
            .setValue(CUBE, GiantAnvilCube.CORNER)
            .setValue(AXIS, Direction.Axis.Z));
    }

    public static void loot(RegistrumBlockLootTables tables, GiantMonolithCoreBlock block) {
        tables.add(block, LootTable.lootTable()
            .withPool(tables.applyExplosionCondition(block, LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                    .setProperties(StatePropertiesPredicate.Builder.properties()
                        .hasProperty(HALF, Cube3x3PartHalf.MID_CENTER)))
                .add(LootItem.lootTableItem(block)
                    .when(DataGenUtil.hasSilkTouch(tables.getRegistries()))
                    .otherwise(LootItem.lootTableItem(ModBlocks.MONOLITH)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(27.0F))))))));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative() && !this.isMainPart(state)) {
            BlockPos mainPos = this.getMainPartPos(pos, state);
            BlockState mainState = level.getBlockState(mainPos);
            if (mainState.is(this)) {
                // 保留被挖掘的部件，等玩家破坏回调使用实际工具统一结算掉落。
                level.setBlock(mainPos, mainState.getFluidState().createLegacyBlock(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(
        Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool
    ) {
        super.playerDestroy(level, player, pos, state.setValue(HALF, Cube3x3PartHalf.MID_CENTER), blockEntity, tool);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.isMainPart(state) ? ModBlockEntities.MONOLITH_CORE.create(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type
    ) {
        if (level.isClientSide || type != ModBlockEntities.MONOLITH_CORE.get()) return null;
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof MonolithCoreBlockEntity core) core.tick();
        };
    }

    @Override
    public InteractionResult use(
        BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
    ) {
        ItemStack stack = player.getItemInHand(hand);
        if (!MonolithCoreBlockEntity.acceptsOffering(stack, true)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        BlockPos mainPos = this.getMainPartPos(pos, state);
        if (!(level.getBlockEntity(mainPos) instanceof MonolithCoreBlockEntity core) || core.isCoolingDown()) {
            return InteractionResult.CONSUME;
        }
        List<RecipeHolder<SpecialCelestialBodyRecipe>> recipes = serverLevel.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.SPECIAL_CELESTIAL_BODY_TYPE.get()).stream()
            .filter(holder -> !holder.value().excludeFromMonolith())
            .toList();
        if (recipes.isEmpty()) return InteractionResult.FAIL;
        SpecialCelestialBodyRecipe recipe = recipes.get(level.random.nextInt(recipes.size())).value();
        ItemStack book = createKnowledgeBook(recipe, serverLevel.getSeed());
        if (!core.beginOffering(stack, player, book)) return InteractionResult.CONSUME;
        stack.consume(1, player);
        return InteractionResult.CONSUME;
    }

    public static ItemStack createKnowledgeBook(SpecialCelestialBodyRecipe recipe, long worldSeed) {
        Component page = Component.translatable(
            "book.anvilcraft.monolith.page",
            Component.translatable("screen.anvilcraft.cfa.class.special." + recipe.name()),
            recipe.time(), recipe.space(), recipe.mass(), recipe.energy(),
            recipe.getEffectiveSeedItem(worldSeed).getDescription()
        );
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
            Filterable.passThrough("Celestial Knowledge"), "The Monolith", 0,
            List.of(Filterable.passThrough(page)), true
        ));
        book.set(DataComponents.ITEM_NAME, Component.translatable("book.anvilcraft.monolith.title"));
        return book;
    }

    @Override
    public Property<Cube3x3PartHalf> getPart() {
        return HALF;
    }

    @Override
    public Cube3x3PartHalf[] getParts() {
        return Cube3x3PartHalf.values();
    }

    @Override
    public BlockState placedState(Cube3x3PartHalf part, BlockState state) {
        return super.placedState(part, state)
            .setValue(CUBE, part == Cube3x3PartHalf.MID_CENTER ? GiantAnvilCube.CENTER : GiantAnvilCube.CORNER);
    }

    @Override
    public BlockState getPlacementState(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, CUBE, AXIS);
    }

    @Override
    public Vec3i getMainPartOffset() {
        return new Vec3i(0, 1, 0);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        state = state.setValue(HALF, state.getValue(HALF).rotate(rotation));
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.getValue(AXIS)) {
                case X -> state.setValue(AXIS, Direction.Axis.Z);
                case Z -> state.setValue(AXIS, Direction.Axis.X);
                default -> state;
            };
            default -> state;
        };
    }
}
