package dev.dubhe.anvilcraft.block;

import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.state.FragmentationDegree;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class SugarBlock extends Block {
    public static final EnumProperty<FragmentationDegree> FRAGMENTATION_DEGREE = EnumProperty.create("fragmentation_degree", FragmentationDegree.class);

    public SugarBlock(Properties properties) {
        super(properties);
        this.stateDefinition.any().setValue(FRAGMENTATION_DEGREE, FragmentationDegree.ZERO);
    }

    public static void loot(RegistrateBlockLootTables tables, Block block) {
        tables.add(block, LootTable.lootTable()
            .withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(block)
                    .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                        .setProperties(StatePropertiesPredicate.Builder.properties()
                            .hasProperty(SugarBlock.FRAGMENTATION_DEGREE, FragmentationDegree.ZERO)))
                    .otherwise(LootItem.lootTableItem(block)
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                            .setProperties(StatePropertiesPredicate.Builder.properties()
                                .hasProperty(SugarBlock.FRAGMENTATION_DEGREE, FragmentationDegree.ONE))))
                    .otherwise(LootItem.lootTableItem(block)
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                            .setProperties(StatePropertiesPredicate.Builder.properties()
                                .hasProperty(SugarBlock.FRAGMENTATION_DEGREE, FragmentationDegree.TWO))))
                    .otherwise(tables.applyExplosionCondition(block, LootItem.lootTableItem(Items.SUGAR)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(9.0f)))
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                            .setProperties(StatePropertiesPredicate.Builder.properties()
                                .hasProperty(SugarBlock.FRAGMENTATION_DEGREE, FragmentationDegree.THREE)))))
                )));
    }

    public void onHit(Level level, BlockPos pos) {
        level.scheduleTick(pos, this, 4);
        Collection<ChargeCollectorManager.Entry> chargeCollectorCollection =
            ChargeCollectorManager.getInstance(level).getNearestChargeCollect(pos);
        double surplus = 1;
        for (ChargeCollectorManager.Entry entry : chargeCollectorCollection) {
            ChargeCollectorBlockEntity chargeCollectorBlockEntity = entry.getBlockEntity();
            if (!ChargeCollectorManager.getInstance(level).canCollect(chargeCollectorBlockEntity, pos)) return;
            surplus = chargeCollectorBlockEntity.incomingCharge(surplus, pos);
            if (surplus == 0) return;
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        double chance = random.nextDouble();
        if (!level.isClientSide) {
            if (state.getValue(FRAGMENTATION_DEGREE) != FragmentationDegree.THREE) {
                if (chance <= 0.05) {
                    level.setBlockAndUpdate(pos, state.setValue(FRAGMENTATION_DEGREE, state.getValue(FRAGMENTATION_DEGREE).next()));
                }
            } else {
                if (chance <= 0.05) {
                    level.destroyBlock(pos, true);
                }
            }
        }
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FRAGMENTATION_DEGREE, FragmentationDegree.ZERO);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FRAGMENTATION_DEGREE);
    }
}
