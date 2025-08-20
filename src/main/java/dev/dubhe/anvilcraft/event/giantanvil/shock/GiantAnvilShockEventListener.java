package dev.dubhe.anvilcraft.event.giantanvil.shock;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.behavior.BehaviorTree;
import dev.dubhe.anvilcraft.api.behavior.TreeNode;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class GiantAnvilShockEventListener {
    public static final String DESTROY_MODE = "destroy_mode";
    public static final String DESTROY_TYPE = "destroy_type";
    public static final String HURT_TYPE = "hurt_type";
    public static final String NO_HURT = "no_hurt";
    private static final BehaviorTree<ShockContext> behaviorTree;

    static {
        TreeNode<ShockContext> root = TreeNode.<ShockContext>predicatedExecutable(
            it -> it.unwrap().level().getBlockState(it.unwrap().centerPos()).is(ModBlocks.HEAVY_IRON_BLOCK)
        ).then(
            //break mode
            TreeNode.<ShockContext>executes(it -> {
                if (it.has(DESTROY_MODE) && it.has(DESTROY_TYPE)) {
                    DestroyMode mode = it.getAttachment(DESTROY_MODE, DestroyMode.class);
                    DestroyType type = it.getAttachment(DESTROY_TYPE, DestroyType.class);
                    type.accept(it.unwrap(), it.unwrap().rangePosList(), mode);
                }
            }).then(
                //test anvil type
                TreeNode.multiple(
                    TreeNode.<ShockContext>predicatedExecutable(it ->
                        (it.unwrap().testBorder(AnvilBlock.class) || it.unwrap().testBorder(ModBlocks.SPECTRAL_ANVIL))
                            && !it.unwrap().testBorder(ModBlocks.ROYAL_ANVIL)
                            && !it.unwrap().testBorder(ModBlocks.EMBER_ANVIL)
                            && !it.unwrap().testBorder(ModBlocks.TRANSCENDENCE_ANVIL)
                    ).executes(it -> it.putAttachment(DESTROY_MODE, DestroyMode.NORMAL)),
                    TreeNode.<ShockContext>predicatedExecutable(
                        it -> it.unwrap().testBorder(ModBlocks.ROYAL_ANVIL)
                    ).executes(it -> it.putAttachment(DESTROY_MODE, DestroyMode.SILK_TOUCH)),
                    TreeNode.<ShockContext>predicatedExecutable(
                        it -> it.unwrap().testBorder(ModBlocks.EMBER_ANVIL)
                    ).executes(it -> it.putAttachment(DESTROY_MODE, DestroyMode.AUTO_SMELTING)),
                    TreeNode.<ShockContext>predicatedExecutable(
                        it -> it.unwrap().testBorder(ModBlocks.TRANSCENDENCE_ANVIL)
                    ).executes(it -> it.putAttachment(DESTROY_MODE, DestroyMode.FORTUNE))
                )
            ).then(
                //test block type
                TreeNode.multiple(
                    TreeNode.<ShockContext>predicatedExecutable(it ->
                        it.unwrap().testCorner(BlockTags.LOGS)
                    ).executes(it -> it.putAttachment(DESTROY_TYPE, DestroyType.FELLING)),
                    TreeNode.<ShockContext>predicatedExecutable(it ->
                        it.unwrap().testCorner(Blocks.HAY_BLOCK)
                    ).executes(it -> it.putAttachment(DESTROY_TYPE, DestroyType.HARVESTING)),
                    TreeNode.<ShockContext>predicatedExecutable(it ->
                        it.unwrap().testCorner(Blocks.GRASS_BLOCK) || it.unwrap().testCorner(Blocks.MYCELIUM) || it.unwrap().testCorner(Blocks.PODZOL)
                    ).executes(it -> it.putAttachment(DESTROY_TYPE, DestroyType.CLEANING)),
                    TreeNode.<ShockContext>predicatedExecutable(it ->
                        it.unwrap().testCorner(Blocks.OBSIDIAN)
                    ).executes(it -> it.putAttachment(DESTROY_TYPE, DestroyType.GENERAL))
                )
            )
        ).then(
            // hurt mode
            TreeNode.<ShockContext>predicate(
                it -> it.unwrap().testBorder(ModBlocks.CURSED_GOLD_BLOCK)
            ).then(
                TreeNode.<ShockContext>predicatedExecutable(it -> it.unwrap().testCorner(ModBlocks.RUBY_BLOCK))
                    .executes(it -> it.putAttachment(HURT_TYPE, HurtType.FIRE))
            ).then(
                TreeNode.<ShockContext>predicatedExecutable(it -> it.unwrap().testCorner(ModBlocks.SAPPHIRE_BLOCK))
                    .executes(it -> it.putAttachment(HURT_TYPE, HurtType.FROZEN))
            ).then(
                TreeNode.<ShockContext>predicatedExecutable(it -> it.unwrap().testCorner(ModBlocks.TOPAZ_BLOCK))
                    .executes(it -> it.putAttachment(HURT_TYPE, HurtType.SHOCK))
            ).then(
                TreeNode.<ShockContext>predicatedExecutable(it -> it.unwrap().testCorner(ModBlocks.VOID_MATTER_BLOCK))
                    .executes(it -> it.putAttachment(HURT_TYPE, HurtType.VOID))
            )
        ).then(
            TreeNode.<ShockContext>predicatedExecutable(it ->
                it.unwrap().testCorner(ModBlocks.RESIN_BLOCK) && it.unwrap().testBorder(ModBlocks.RESIN_BLOCK)
            ).executes(it -> {
                Level level = it.unwrap().level();
                for (BlockPos pos : it.unwrap().rangePosList()) {
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof AnvilBlock) {
                        FallingBlockEntity entity = new FallingBlockEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY(),
                            pos.getZ() + 0.5,
                            state.hasProperty(BlockStateProperties.WATERLOGGED)
                                ? state.setValue(BlockStateProperties.WATERLOGGED, false)
                                : state
                        );
                        level.setBlock(pos, state.getFluidState().createLegacyBlock(), 3);
                        entity.setDeltaMovement(0, 0.31, 0);
                        level.addFreshEntity(entity);
                    }
                }
                it.putAttachment(NO_HURT, true);
            })
        ).executes(it -> {
            int radius = (int) Math.min(Math.ceil(it.unwrap().fallDistance()), AnvilCraft.config.giantAnvilMaxShockRadius);
            AABB aabb = AABB.ofSize(
                Vec3.atCenterOf(it.unwrap().centerPos().above()),
                radius * 2 + 1,
                1,
                radius * 2 + 1
            );
            List<LivingEntity> e = it.unwrap().level().getEntitiesOfClass(LivingEntity.class, aabb);
            for (LivingEntity l : e) {
                if (it.has(HURT_TYPE)) {
                    HurtType hurtType = it.getAttachment(HURT_TYPE, HurtType.class);
                    l.hurt(hurtType.damageSource(l.level()), it.unwrap().fallDistance() * 2 * 2);
                    hurtType.postApply(l.level(), l, it.unwrap().fallDistance());
                } else {
                    if (l.getItemBySlot(EquipmentSlot.FEET).is(Items.AIR)) {
                        l.hurt(it.unwrap().level().damageSources().anvil(it.unwrap().fallingGiantAnvil()), it.unwrap().fallDistance() * 2);
                    }
                }
            }
        });
        behaviorTree = new BehaviorTree<>(root);
    }

    @SubscribeEvent
    public static void onLand(@NotNull AnvilEvent.GiantOnLand event) {
        ShockContext context = ShockContext.inflate(event);
        behaviorTree.run(context);
    }
}
