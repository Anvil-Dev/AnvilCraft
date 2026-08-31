package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.BlockTagUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiItemUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.collision.AnvilCollisionCraftRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.collision.BlockTransform;
import dev.dubhe.anvilcraft.util.TooltipUtil;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AnvilCollisionCraftCategory implements IRecipeCategory<RecipeHolder<AnvilCollisionCraftRecipe>> {
    private static final String HIT_BLOCK = "hit_block";
    private static final String TRANSFORM_INPUT_BLOCK = "transform_input_block";
    private static final String TRANSFORM_OUTPUT_BLOCK = "transform_output_block";

    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable arrowDefault;
    private final IDrawable blockConversion;
    private final IDrawable explosion;
    private final IDrawable slotDefault;
    private final IDrawable slotProbability;
    private final IDrawable icon;
    private final Component title;

    public AnvilCollisionCraftCategory(IGuiHelper helper) {
        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
        this.blockConversion = JeiRenderHelper.getArrowBlockConversion(helper);
        this.explosion = JeiRenderHelper.getExplosion(helper);
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.slotProbability = JeiRenderHelper.getSlotProbability(helper);
        this.icon = helper.createDrawableItemStack(ModBlocks.ACCELERATION_RING.asStack());
        this.title = Component.translatable("gui.anvilcraft.category.anvil_collision");
    }

    @Override
    public IRecipeHolderType<AnvilCollisionCraftRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.ANVIL_COLLISION;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return AnvilCollisionCraftCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return AnvilCollisionCraftCategory.HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<AnvilCollisionCraftRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        AnvilCollisionCraftRecipe recipe = recipeHolder.value();
        // 将此配方需要的铁砧加入输入槽
        builder.addInputSlot(JeiSlotUtil.INPUT_X, JeiSlotUtil.DEFAULT_Y)
            .add(Ingredient.of(recipe.anvil().getBlocks().stream().map(Holder::value)));

        // 如果有输出物品则添加到输出
        if (!recipe.outputItems().isEmpty()) {
            List<ChanceItemStack> chanceItemStacks = AnvilCollisionCraftCategory.getChanceItemStacks(recipe);
            JeiItemUtil.addDefaultOutputSlots(builder, chanceItemStacks);
        }

        // 将被撞击的方块加入addInvisibleIngredients中
        JeiBlockIngredientUtil.addInputSlot(builder, AnvilCollisionCraftCategory.HIT_BLOCK, 70, 24, 18, 18, recipe.hitBlock());

        // 将转换方块加入addInvisibleIngredients中
        if (!recipe.transformBlocks().isEmpty()) {
            boolean hasItemOutputs = !recipe.outputItems().isEmpty();
            int x = hasItemOutputs ? 102 : 110;
            int inputHeight = hasItemOutputs ? 10 : 18;
            int outputY = hasItemOutputs ? 10 : 43;
            int outputHeight = hasItemOutputs ? 10 : 18;
            JeiBlockIngredientUtil.addSlot(
                builder,
                RecipeIngredientRole.INPUT,
                AnvilCollisionCraftCategory.TRANSFORM_INPUT_BLOCK,
                x,
                0,
                18,
                inputHeight,
                recipe.transformBlocks().stream()
                    .flatMap(transform -> transform.inputBlock().getBlocks().stream())
                    .map(block -> new ItemStack(block.value()))
                    .toList()
            );

            JeiBlockIngredientUtil.addSlot(
                builder,
                RecipeIngredientRole.OUTPUT,
                AnvilCollisionCraftCategory.TRANSFORM_OUTPUT_BLOCK,
                x,
                outputY,
                18,
                outputHeight,
                recipe.transformBlocks().stream()
                    .map(transform -> new ItemStack(transform.outputBlock().state().getBlock()))
                    .toList()
            );
        }
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder,
        RecipeHolder<AnvilCollisionCraftRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

    private static List<ChanceItemStack> getChanceItemStacks(AnvilCollisionCraftRecipe recipe) {
        List<ChanceItemStack> chanceItemStacks = new ArrayList<>();
        for (ChanceItemStack outputItem : recipe.outputItems()) {
            if (outputItem.count() instanceof BinomialDistributionGenerator(NumberProvider n, NumberProvider p)) {
                if (p instanceof ConstantValue(float value) && value < 1 && n instanceof ConstantValue(float count)) {
                    chanceItemStacks.add(ChanceItemStack.of(outputItem.stack(), (int) count, value));
                } else {
                    chanceItemStacks.add(ChanceItemStack.of(outputItem.stack(), outputItem.getMaxCount()));
                }
            }
        }
        return chanceItemStacks;
    }

    @Override
    public void draw(
        RecipeHolder<AnvilCollisionCraftRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        AnvilCollisionCraftRecipe recipe = recipeHolder.value();

        // explosion
        this.explosion.draw(graphics, 72, 16);

        List<BlockState> hitBlockStates = recipe.hitBlock().constructStatesForRender();
        if (!hitBlockStates.isEmpty()) {
            BlockState renderedState = JeiBlockIngredientUtil
                .getDisplayedState(recipeSlotsView, AnvilCollisionCraftCategory.HIT_BLOCK, hitBlockStates)
                .orElse(hitBlockStates.getFirst());
            // 特判: 如果是大铁砧 则将BlockState改为cube=center,half=mid_center 并修改scale使其大小合理
            // 建议下次写类似大铁砧的方块的时候 把registerDefaultState注册成有材质的中心位置
            // 当然也可以不RenderHelper.renderBlock 直接加进setRecipe的输入输出槽当物品看
            int scale = JeiBlockIngredientUtil.getRenderablePreviewScale(renderedState, 12);

            RenderSupport.renderBlock(
                graphics,
                renderedState,
                80,
                28,
                scale
            );
        }

        // 渲染方块和箭头
        if (!recipe.transformBlocks().isEmpty() || !recipe.outputItems().isEmpty()) {
            if (!recipe.transformBlocks().isEmpty() && recipe.outputItems().isEmpty()) {
                List<BlockTransform> blockTransforms = recipe.transformBlocks();
                for (BlockTransform blockTransform : blockTransforms) {
                    List<BlockState> inputBlockStates = recipe.transformBlocks().stream()
                        .flatMap(transform -> transform.inputBlock().constructStatesForRender().stream())
                        .toList();
                    BlockState inputBlockRenderedState = JeiBlockIngredientUtil
                        .getDisplayedState(recipeSlotsView, AnvilCollisionCraftCategory.TRANSFORM_INPUT_BLOCK, inputBlockStates)
                        .orElse(inputBlockStates.getFirst());
                    RenderSupport.renderBlock(
                        graphics,
                        inputBlockRenderedState,
                        110,
                        0,
                        20
                    );

                    List<BlockState> outputBlockStates = recipe.transformBlocks().stream()
                        .map(transform -> transform.outputBlock().state())
                        .toList();
                    BlockState outputBlockState = JeiBlockIngredientUtil
                        .getDisplayedState(recipeSlotsView, AnvilCollisionCraftCategory.TRANSFORM_OUTPUT_BLOCK, outputBlockStates)
                        .orElse(outputBlockStates.getFirst());
                    RenderSupport.renderBlock(
                        graphics,
                        outputBlockState,
                        110,
                        43,
                        20
                    );

                    this.blockConversion.draw(graphics, 113, 19);

                    Matrix3x2fStack pose = graphics.pose();
                    pose.pushMatrix();
                    pose.scale(0.8F, 0.8F);
                    graphics.text(
                        Minecraft.getInstance().font,
                        Component.translatable("gui.anvilcraft.category.anvil_collision.maxcount", blockTransform.maxCount()),
                        135,
                        75,
                        0xFF000000,
                        false
                    );
                    pose.popMatrix();
                }
            }

            if (!recipe.transformBlocks().isEmpty() && !recipe.outputItems().isEmpty()) {
                List<BlockTransform> blockTransforms = recipe.transformBlocks();
                for (BlockTransform blockTransform : blockTransforms) {
                    List<BlockState> inputBlockStates = recipe.transformBlocks().stream()
                        .flatMap(transform -> transform.inputBlock().constructStatesForRender().stream())
                        .toList();
                    BlockState inputBlockRenderedState = JeiBlockIngredientUtil
                        .getDisplayedState(recipeSlotsView, AnvilCollisionCraftCategory.TRANSFORM_INPUT_BLOCK, inputBlockStates)
                        .orElse(inputBlockStates.getFirst());
                    RenderSupport.renderBlock(
                        graphics,
                        inputBlockRenderedState,
                        110,
                        3,
                        8
                    );

                    List<BlockState> outputBlockStates = recipe.transformBlocks().stream()
                        .map(transform -> transform.outputBlock().state())
                        .toList();
                    BlockState outputBlockState = JeiBlockIngredientUtil
                        .getDisplayedState(recipeSlotsView, AnvilCollisionCraftCategory.TRANSFORM_OUTPUT_BLOCK, outputBlockStates)
                        .orElse(outputBlockStates.getFirst());
                    RenderSupport.renderBlock(
                        graphics,
                        outputBlockState,
                        110,
                        13,
                        8
                    );
                    this.blockConversion.draw(graphics, 86, 6);
                    this.arrowDefault.draw(graphics, 98, 26);
                    Matrix3x2fStack pose = graphics.pose();
                    pose.pushMatrix();
                    pose.scale(0.8F, 0.8F);
                    graphics.text(
                        Minecraft.getInstance().font,
                        Component.translatable("gui.anvilcraft.category.anvil_collision.maxcount", blockTransform.maxCount()),
                        135,
                        75,
                        0xFF000000,
                        false
                    );
                    pose.popMatrix();
                }
            }
            if (!recipe.outputItems().isEmpty() && recipe.transformBlocks().isEmpty()) {
                this.arrowDefault.draw(graphics, 98, 27);
            }
        }

        // 绘制输入输出槽
        JeiSlotUtil.drawDefaultInputSlots(graphics, this.slotDefault, 1);
        if (!recipe.outputItems().isEmpty()) {
            if (JeiRecipeUtil.isChance(recipe.outputItems())) {
                JeiSlotUtil.drawDefaultOutputSlots(graphics, this.slotProbability, recipe.outputItems().size());
            } else {
                JeiSlotUtil.drawDefaultOutputSlots(graphics, this.slotDefault, recipe.outputItems().size());
            }
        }

        // 添加消耗/速度的信息
        Matrix3x2fStack pose = graphics.pose();
        for (int i = 0; i < 7; i++) {
            ItemStack stack = new ItemStack(Blocks.ANVIL);
            GuiRenderExtras.itemWithTransparency(graphics, stack, 55 - i * 3, 24, 1F - (float) i / 10);
        }
        pose.pushMatrix();
        pose.scale(0.8F, 0.8F);
        graphics.text(
            Minecraft.getInstance().font,
            Component.translatable("gui.anvilcraft.category.anvil_collision.consume", recipe.consume()),
            0,
            65,
            0xFF000000,
            false
        );
        graphics.text(
            Minecraft.getInstance().font,
            Component.translatable("gui.anvilcraft.category.anvil_collision.speed", recipe.speed()),
            0,
            75,
            0xFF000000,
            false
        );
        pose.popMatrix();
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<AnvilCollisionCraftRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        IRecipeCategory.super.getTooltip(tooltip, recipeHolder, recipeSlotsView, mouseX, mouseY);
        Identifier id = this.getIdentifier(recipeHolder);
        AnvilCollisionCraftRecipe recipe = recipeHolder.value();

        if (mouseX >= 70 && mouseX <= 88) {
            if (mouseY >= 24 && mouseY < 42) {
                tooltip.addAll(BlockTagUtil.getTooltipsForInput(recipe.hitBlock()));
            }
        }

        if (!recipe.transformBlocks().isEmpty()) {
            List<BlockTransform> blockTransforms = recipe.transformBlocks();
            for (BlockTransform blockTransform : blockTransforms) {
                if (mouseX >= 110 && mouseX <= 128) {
                    if (mouseY >= 0 && mouseY < 18) {
                        tooltip.addAll(BlockTagUtil.getTooltipsForInput(blockTransform.inputBlock()));
                    }
                    if (mouseY >= 43 && mouseY < 61) {
                        Block block = blockTransform.outputBlock().state().getBlock();
                        if (id != null) {
                            tooltip.addAll(TooltipUtil.recipeIDTooltip(block, id));
                        } else {
                            tooltip.addAll(TooltipUtil.tooltip(block));
                        }
                    }
                }
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.ANVIL_COLLISION,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.ANVIL_COLLISION_CRAFT.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.ANVIL_COLLISION, ModBlocks.ACCELERATION_RING.asStack());
        registration.addCraftingStation(AnvilCraftJeiPlugin.ANVIL_COLLISION, ModBlocks.DEFLECTION_RING.asStack());
    }
}
