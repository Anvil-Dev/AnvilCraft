package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilAmplifierBlock;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDAnvilCollisionCraftRecipeComponent;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HandbookHelperPreview {
    public static Map<String, Object> report() {
        var result = new LinkedHashMap<String, Object>();
        for (var block : List.<Block>of(ModBlocks.GIANT_ANVIL.get(), ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER.get())) {
            var mappings = new LinkedHashMap<String, String>();
            for (var state : block.getStateDefinition().getPossibleStates()) {
                BlockState model = IMultiPartBlockModelHolder.modelHolderState(state);
                if (!model.is(block) || IMultiPartBlockModelHolder.modelHolderState(model) != model) {
                    throw new IllegalStateException("Invalid model holder " + state);
                }
                mappings.put(state.toString(), model.toString());
            }
            result.put(block.toString(), mappings);
        }
        result.put("block_metrics", List.of(AgeratumUtil.BLOCK_SIZE, AgeratumUtil.BLOCK_HEIGHT, AgeratumUtil.BLOCK_TOOLTIP_SIZE));
        return result;
    }

    public static void draw(GuiGraphicsExtractor graphics, int width, int height) {
        graphics.fill(0, 0, width, height, 0xFFF1E6CD);
        graphics.pose().pushMatrix();
        graphics.pose().scale(2, 2);
        final var context = new MDRenderContext(null, Minecraft.getInstance(), graphics, new ArrayList<>(),
            width, height, width / 2, height / 2, -1, -1, 0, 0, 1, 0, 0, new ArrayList<>());
        for (int i = 0; i < 4; i++) AgeratumUtil.renderArrow(graphics, 40 + i * 70, 20, i * 90);
        graphics.blit(RenderPipelines.GUI_TEXTURED, MDAnvilCollisionCraftRecipeComponent.EXPLOSION,
            350, 20, 0, 0, 32, 32, 32, 32);
        for (int i = 0; i < 7; i++) {
            RenderSupport.renderItemWithTransparency(new ItemStack(Blocks.ANVIL), graphics, 480 - i * 3, 28, 1F - i / 10F);
        }
        var states = new ArrayList<>(List.of(Blocks.STONE.defaultBlockState(), Blocks.ANVIL.defaultBlockState(),
            Blocks.SCAFFOLDING.defaultBlockState(), Blocks.CAULDRON.defaultBlockState(), ModBlocks.GIANT_ANVIL.getDefaultState()));
        for (var direction : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            states.add(ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER.getDefaultState()
                .setValue(CelestialForgingAnvilAmplifierBlock.FACING, direction));
        }
        for (int i = 0; i < states.size(); i++) block(context, states.get(i), 40 + i * 70, 150);
        AgeratumUtil.renderItem(context, new ItemStack(Items.DIAMOND, 7), -1, -1, 50, 250);
        AgeratumUtil.renderItems(context, List.of(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GOLD_INGOT),
            new ItemStack(Items.EMERALD), new ItemStack(Items.REDSTONE)), -1, -1, 160, 250);
        block(context, Blocks.STONE.defaultBlockState(), 280, AgeratumUtil.getRenderY(232, 2));
        block(context, Blocks.GOLD_BLOCK.defaultBlockState(), 280, AgeratumUtil.getRenderY(232, 1));
        block(context, Blocks.DIAMOND_BLOCK.defaultBlockState(), 280, 232);
        block(context, Blocks.COPPER_DOOR.defaultBlockState(), 380, 232);
        block(context, Blocks.COPPER_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, true), 460, 232);
        block(context, Blocks.COPPER_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 540, 232);
        graphics.pose().popMatrix();
    }

    private static void block(MDRenderContext context, BlockState state, int x, int y) {
        AgeratumUtil.renderBlock(context, state, -1, -1, x, y);
    }
}
