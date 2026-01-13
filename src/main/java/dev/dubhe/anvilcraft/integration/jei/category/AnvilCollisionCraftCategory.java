package dev.dubhe.anvilcraft.integration.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.recipe.component.ChanceBlockState;
import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.BlockTagUtil;
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
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AnvilCollisionCraftCategory implements IRecipeCategory<RecipeHolder<AnvilCollisionCraftRecipe>> {
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
    public RecipeType<RecipeHolder<AnvilCollisionCraftRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.ANVIL_COLLISION;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<AnvilCollisionCraftRecipe> recipeHolder,
        IFocusGroup focuses) {
        AnvilCollisionCraftRecipe recipe = recipeHolder.value();
        // 将此配方需要的铁砧加入输入槽
        builder.addInputSlot(21, 24).addIngredients(
            Ingredient.of(
                recipe.anvil().getBlocks().stream().map(
                    blockHolder -> new ItemStack(blockHolder.value())
                )
            )
        );

        // 如果有输出物品则添加到输出
        if (!recipe.outputItems().isEmpty()) {
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
            JeiSlotUtil.addOutputSlots(builder, chanceItemStacks);
        }

        // 将被撞击的方块加入addInvisibleIngredients中
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addIngredients(
            Ingredient.of(
                recipe.hitBlock().getBlocks().stream().map(
                    blockHolder -> new ItemStack(blockHolder.value())
                )
            )
        );

        // 将转换方块加入addInvisibleIngredients中
        if (!recipe.transformBlocks().isEmpty()) {
            BlockStatePredicate inputBlock = recipe.transformBlocks().getLast().inputBlock();
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addIngredients(
                Ingredient.of(inputBlock.getBlocks().stream().map(
                    blockHolder -> new ItemStack(blockHolder.value()))
                )
            );

            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addIngredients(
                Ingredient.of(
                    recipe.transformBlocks().stream().map(
                        blockTransform -> new ItemStack(blockTransform.outputBlock().state().getBlock())
                    )
                )
            );
        }
    }

    @Override
    public void draw(
        RecipeHolder<AnvilCollisionCraftRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        AnvilCollisionCraftRecipe recipe = recipeHolder.value();

        // explosion
        explosion.draw(guiGraphics, 72, 16);

        for (int i = recipe.hitBlock().getBlocks().size() - 1; i >= 0; i--) {
            List<BlockState> input = recipe.hitBlock().constructStatesForRender();
            if (input.isEmpty()) continue;
            BlockState renderedState = input.get((int) ((System.currentTimeMillis() / 1000) % input.size()));
            // 特判: 如果是大铁砧 则将BlockState改为cube=center,half=mid_center 并修改scale使其大小合理
            // 建议下次写类似大铁砧的方块的时候 把registerDefaultState注册成有材质的中心位置
            // 当然也可以不RenderHelper.renderBlock 直接加进setRecipe的输入输出槽当物品看
            int scale = 12;
            if (renderedState.is(ModBlocks.GIANT_ANVIL)) {
                scale = 5;
                renderedState = ModBlocks.GIANT_ANVIL.getDefaultState()
                    .trySetValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                    .trySetValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER);
            }

            RenderSupport.renderBlock(
                guiGraphics,
                renderedState,
                80,
                28,
                20,
                scale,
                RenderSupport.SINGLE_BLOCK
            );
        }

        // 渲染方块和箭头
        if (!recipe.transformBlocks().isEmpty() || !recipe.outputItems().isEmpty()) {
            if (!recipe.transformBlocks().isEmpty() && recipe.outputItems().isEmpty()) {
                List<BlockTransform> blockTransforms = recipe.transformBlocks();
                for (BlockTransform blockTransform : blockTransforms) {
                    BlockStatePredicate inputBlock = blockTransform.inputBlock();
                    List<BlockState> inputBlockState = inputBlock.constructStatesForRender();
                    BlockState inputBlockRenderedState = inputBlockState.get(
                        (int) ((System.currentTimeMillis() / 1000) % inputBlockState.size())
                    );
                    RenderSupport.renderBlock(
                        guiGraphics,
                        inputBlockRenderedState,
                        120,
                        5,
                        20,
                        12,
                        RenderSupport.SINGLE_BLOCK
                    );

                    ChanceBlockState outputBlock = blockTransform.outputBlock();
                    BlockState outputBlockState = outputBlock.state();
                    RenderSupport.renderBlock(
                        guiGraphics,
                        outputBlockState,
                        120,
                        48,
                        20,
                        12,
                        RenderSupport.SINGLE_BLOCK
                    );

                    blockConversion.draw(guiGraphics, 113, 19);

                    PoseStack pose = guiGraphics.pose();
                    pose.pushPose();
                    pose.scale(0.8f, 0.8f, 1.0f);
                    guiGraphics.drawString(Minecraft.getInstance().font,
                        Component.translatable("gui.anvilcraft.category.anvil_collision.maxcount", blockTransform.maxCount()),
                        135, 75, 0xFF000000, false);
                    pose.popPose();
                }
            }

            if (!recipe.transformBlocks().isEmpty() && !recipe.outputItems().isEmpty()) {
                List<BlockTransform> blockTransforms = recipe.transformBlocks();
                for (BlockTransform blockTransform : blockTransforms) {
                    BlockStatePredicate inputBlock = blockTransform.inputBlock();
                    List<BlockState> inputBlockState = inputBlock.constructStatesForRender();
                    BlockState inputBlockRenderedState = inputBlockState.get(
                        (int) ((System.currentTimeMillis() / 1000) % inputBlockState.size())
                    );
                    RenderSupport.renderBlock(
                        guiGraphics,
                        inputBlockRenderedState,
                        110,
                        3,
                        20,
                        8,
                        RenderSupport.SINGLE_BLOCK
                    );

                    ChanceBlockState outputBlock = blockTransform.outputBlock();
                    BlockState outputBlockState = outputBlock.state();
                    RenderSupport.renderBlock(
                        guiGraphics,
                        outputBlockState,
                        110,
                        13,
                        20,
                        8,
                        RenderSupport.SINGLE_BLOCK
                    );
                    blockConversion.draw(guiGraphics, 86, 6);
                    arrowDefault.draw(guiGraphics, 98, 26);
                    PoseStack pose = guiGraphics.pose();
                    pose.pushPose();
                    pose.scale(0.8f, 0.8f, 1.0f);
                    guiGraphics.drawString(Minecraft.getInstance().font,
                        Component.translatable("gui.anvilcraft.category.anvil_collision.maxcount", blockTransform.maxCount()),
                        135, 75, 0xFF000000, false);
                    pose.popPose();
                }
            }
            if (!recipe.outputItems().isEmpty() && recipe.transformBlocks().isEmpty()) {
                arrowDefault.draw(guiGraphics, 98, 27);
            }
        }

        // 绘制输入输出槽
        JeiSlotUtil.drawInputSlots(guiGraphics, slotDefault, 1);
        if (!recipe.outputItems().isEmpty()) {
            if (JeiRecipeUtil.isChance(recipe.outputItems())) {
                JeiSlotUtil.drawOutputSlots(guiGraphics, slotProbability, recipe.outputItems().size());
            } else {
                JeiSlotUtil.drawOutputSlots(guiGraphics, slotDefault, recipe.outputItems().size());
            }
        }

        // 添加消耗/速度的信息
        PoseStack pose = guiGraphics.pose();
        for (int i = 0; i < 7; i++) {
            RenderSupport.renderItemWithTransparency(
                new ItemStack(Blocks.ANVIL),
                pose,
                55 - i * 3,
                24,
                1f - (float) i / 10
            );
        }
        pose.pushPose();
        pose.scale(0.8f, 0.8f, 1.0f);
        guiGraphics.drawString(Minecraft.getInstance().font,
            Component.translatable("gui.anvilcraft.category.anvil_collision.consume", recipe.consume()),
            0, 65, 0xFF000000, false);
        guiGraphics.drawString(Minecraft.getInstance().font,
            Component.translatable("gui.anvilcraft.category.anvil_collision.speed", recipe.speed()),
            0, 75, 0xFF000000, false);
        pose.popPose();
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<AnvilCollisionCraftRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY) {
        IRecipeCategory.super.getTooltip(tooltip, recipeHolder, recipeSlotsView, mouseX, mouseY);
        ResourceLocation id = getRegistryName(recipeHolder);
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
        registration.addRecipeCatalyst(ModBlocks.ACCELERATION_RING.asStack(), AnvilCraftJeiPlugin.ANVIL_COLLISION);
        registration.addRecipeCatalyst(ModBlocks.DEFLECTION_RING.asStack(), AnvilCraftJeiPlugin.ANVIL_COLLISION);
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilCraftJeiPlugin.ANVIL_COLLISION);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_ANVIL), AnvilCraftJeiPlugin.ANVIL_COLLISION);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_ANVIL), AnvilCraftJeiPlugin.ANVIL_COLLISION);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.TRANSCENDENCE_ANVIL), AnvilCraftJeiPlugin.ANVIL_COLLISION);
    }
}
