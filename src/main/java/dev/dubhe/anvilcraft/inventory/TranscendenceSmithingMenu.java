package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.item.template.frost.DeformationTemplateItem;
import dev.dubhe.anvilcraft.item.template.frost.PermutationTemplateItem;
import dev.dubhe.anvilcraft.item.template.mto.BaseMultipleToOneTemplateItem;
import dev.dubhe.anvilcraft.network.multiple.TranscendenceSmithingPackets;
import dev.dubhe.anvilcraft.recipe.frost.DeformationRecipe;
import dev.dubhe.anvilcraft.recipe.frost.FrostSmithingRecipeInput;
import dev.dubhe.anvilcraft.recipe.frost.IFrostSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.frost.PermutationRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.BaseMultipleToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.MultipleToOneSmithingRecipeInput;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * 超限锻造台菜单。
 *
 * <p>模板仅用于选择配方模式，不进入任何容器槽位。</p>
 */
public class TranscendenceSmithingMenu extends AbstractContainerMenu {
    public static final int ROYAL_FROST_FIRST_INPUT_SLOT = 0;
    public static final int ROYAL_FROST_SECOND_INPUT_SLOT = 1;
    public static final int EMBER_MATERIAL_SLOT = 2;
    public static final int EMBER_INPUT_SLOT_START = 3;
    public static final int EMBER_INPUT_SLOT_END = 11;
    public static final int ROYAL_FROST_RESULT_SLOT = 11;
    public static final int EMBER_RESULT_SLOT = 12;

    private static final int PLAYER_INVENTORY_SLOT_START = 13;
    private static final int PLAYER_INVENTORY_SLOT_END = 40;
    private static final int PLAYER_HOTBAR_SLOT_START = 40;
    private static final int PLAYER_HOTBAR_SLOT_END = 49;

    private static final int[][] EMBER_INPUT_POSITIONS = {
        {80, 18},
        {80, 54},
        {62, 36},
        {98, 36},
        {62, 18},
        {98, 18},
        {62, 54},
        {98, 54}
    };

    private final ContainerLevelAccess access;
    private final Level level;
    private final Player menuPlayer;
    private final Container royalFrostInputs;
    private final Container emberInputs;
    private final ResultContainer royalFrostResult = new ResultContainer();
    private final ResultContainer emberResult = new ResultContainer();
    private final List<RecipeHolder<SmithingRecipe>> royalRecipes;
    private final List<RecipeHolder<BaseMultipleToOneSmithingRecipe>> emberRecipes;
    private final List<RecipeHolder<? extends IFrostSmithingRecipe>> frostRecipes;

    private List<ItemStack> templates = List.of();
    private List<Identifier> favoriteTemplates = List.of();
    private ItemStack selectedTemplate = ItemStack.EMPTY;
    private boolean templateDataDirty = true;

    @Nullable
    private RecipeHolder<SmithingRecipe> selectedRoyalRecipe;

    @Nullable
    private RecipeHolder<BaseMultipleToOneSmithingRecipe> selectedEmberRecipe;

    @Nullable
    private RecipeHolder<? extends IFrostSmithingRecipe> selectedFrostRecipe;

    private List<RecipeResult> frostResults = List.of();
    private int selectedFrostResult = -1;

    public TranscendenceSmithingMenu(
        MenuType<TranscendenceSmithingMenu> type,
        int containerId,
        Inventory playerInventory
    ) {
        this(type, containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public TranscendenceSmithingMenu(
        int containerId,
        Inventory playerInventory,
        ContainerLevelAccess access
    ) {
        this(ModMenuTypes.TRANSCENDENCE_SMITHING.get(), containerId, playerInventory, access);
    }

    /**
     * 创建超限锻造台菜单。
     *
     * @param type            菜单类型
     * @param containerId     容器 id
     * @param playerInventory 玩家背包
     * @param access          方块位置访问器
     */
    public TranscendenceSmithingMenu(
        MenuType<TranscendenceSmithingMenu> type,
        int containerId,
        Inventory playerInventory,
        ContainerLevelAccess access
    ) {
        super(type, containerId);
        this.access = access;
        this.level = playerInventory.player.level();
        this.menuPlayer = playerInventory.player;
        this.royalRecipes = List.copyOf(RecipesRecord.getRecipes(this.level).byType(RecipeType.SMITHING));
        this.emberRecipes = List.copyOf(
            RecipesRecord.getRecipes(this.level).byType(ModRecipeTypes.MULTIPLE_TO_ONE_SMITHING.get())
        );
        this.frostRecipes = this.collectFrostRecipes();
        this.royalFrostInputs = this.createInputContainer(2);
        this.emberInputs = this.createInputContainer(9);

        this.addInputSlots();
        this.addResultSlots();
        this.addPlayerInventory(playerInventory);
        if (!this.level.isClientSide()) {
            this.refreshTemplateCatalog();
        }
    }

    private List<RecipeHolder<? extends IFrostSmithingRecipe>> collectFrostRecipes() {
        List<RecipeHolder<? extends IFrostSmithingRecipe>> result = new ArrayList<>();
        result.addAll(RecipesRecord.getRecipes(this.level).byType(ModRecipeTypes.PERMUTATION.get()));
        result.addAll(RecipesRecord.getRecipes(this.level).byType(ModRecipeTypes.DEFORMATION.get()));
        return List.copyOf(result);
    }

    private Container createInputContainer(int size) {
        return new SimpleContainer(size) {
            @Override
            public void setChanged() {
                super.setChanged();
                TranscendenceSmithingMenu.this.slotsChanged(this);
            }
        };
    }

    private void addInputSlots() {
        this.addSlot(new ModeSlot(
            this.royalFrostInputs,
            0,
            44,
            48,
            this::isRoyalOrFrostModeActive,
            this::canPlaceRoyalFrostFirstInput
        ));
        this.addSlot(new ModeSlot(
            this.royalFrostInputs,
            1,
            62,
            48,
            this::isRoyalOrFrostModeActive,
            this::canPlaceRoyalFrostSecondInput
        ));
        this.addSlot(new ModeSlot(
            this.emberInputs,
            0,
            80,
            36,
            this::isEmberModeActive,
            this::canPlaceEmberMaterial
        ));
        for (int index = 0; index < EMBER_INPUT_POSITIONS.length; index++) {
            int inputIndex = index;
            int[] position = EMBER_INPUT_POSITIONS[index];
            this.addSlot(new ModeSlot(
                this.emberInputs,
                index + 1,
                position[0],
                position[1],
                () -> this.isEmberInputActive(inputIndex),
                stack -> this.canPlaceEmberInput(inputIndex, stack)
            ));
        }
    }

    private void addResultSlots() {
        this.addSlot(new SmithingResultSlot(
            this.royalFrostResult,
            106,
            48,
            this::isRoyalOrFrostModeActive,
            false
        ));
        this.addSlot(new SmithingResultSlot(
            this.emberResult,
            151,
            48,
            this::isEmberModeActive,
            true
        ));
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    public List<ItemStack> getTemplates() {
        return this.templates;
    }

    public List<Identifier> getFavoriteTemplates() {
        return this.favoriteTemplates;
    }

    public ItemStack getSelectedTemplate() {
        return this.selectedTemplate;
    }

    public Mode getMode() {
        if (this.selectedTemplate.getItem() instanceof BaseMultipleToOneTemplateItem) return Mode.EMBER;
        if (this.selectedTemplate.getItem() instanceof PermutationTemplateItem
            || this.selectedTemplate.getItem() instanceof DeformationTemplateItem) {
            return Mode.FROST;
        }
        return Mode.ROYAL;
    }

    public boolean hasSelectedTemplate() {
        return !this.selectedTemplate.isEmpty();
    }

    public boolean isSelectedTemplate(ItemStack stack) {
        return !this.selectedTemplate.isEmpty() && stack.is(this.selectedTemplate.getItem());
    }

    public int getEmberInputSize() {
        if (this.selectedTemplate.getItem() instanceof BaseMultipleToOneTemplateItem template) {
            return template.getSize();
        }
        return 0;
    }

    public List<ItemStack> getEmberInputStacks() {
        List<ItemStack> result = new ArrayList<>(EMBER_INPUT_POSITIONS.length);
        for (int index = 1; index < this.emberInputs.getContainerSize(); index++) {
            result.add(this.emberInputs.getItem(index));
        }
        return result;
    }

    public ItemStack getRoyalFrostFirstInput() {
        return this.royalFrostInputs.getItem(0);
    }

    public ItemStack getRoyalFrostSecondInput() {
        return this.royalFrostInputs.getItem(1);
    }

    public ItemStack getEmberMaterial() {
        return this.emberInputs.getItem(0);
    }

    public ItemStack getActiveResult() {
        return this.getMode() == Mode.EMBER ? this.emberResult.getItem(0) : this.royalFrostResult.getItem(0);
    }

    public List<RecipeResult> getFrostResults() {
        return this.frostResults;
    }

    public int getSelectedFrostResult() {
        return this.selectedFrostResult;
    }

    /** 接收服务端同步的模板目录、置顶顺序与当前虚拟模板。 */
    public void handleTemplateSync(
        List<ItemStack> syncedTemplates,
        List<Identifier> favorites,
        ItemStack template
    ) {
        final boolean templateChanged = !ItemStack.isSameItemSameComponents(this.selectedTemplate, template);
        this.templates = syncedTemplates.stream().map(ItemStack::copy).toList();
        this.favoriteTemplates = List.copyOf(favorites);
        this.selectedTemplate = template.copy();
        if (templateChanged) {
            this.createResult();
        }
    }

    /** 处理模板选择或置顶请求。 */
    public void handleTemplateAction(Player player, Identifier templateId, boolean toggleFavorite) {
        if (!(player instanceof ServerPlayer serverPlayer) || player != this.menuPlayer) return;
        this.refreshTemplateCatalog();
        Optional<ItemStack> requested = this.templates.stream()
            .filter(stack -> itemId(stack).equals(templateId))
            .findFirst();
        if (requested.isEmpty()) return;

        if (toggleFavorite) {
            SmithingTemplateFavorites favorites = player.getData(ModDataAttachments.SMITHING_TEMPLATE_FAVORITES);
            player.setData(ModDataAttachments.SMITHING_TEMPLATE_FAVORITES, favorites.toggle(templateId));
            this.refreshTemplateCatalog();
            this.syncTemplateData(serverPlayer);
            return;
        }

        ItemStack template = requested.get();
        final ItemStack nextTemplate = this.isSelectedTemplate(template)
            ? ItemStack.EMPTY
            : template.copyWithCount(1);
        this.clearContainer(player, this.royalFrostInputs);
        this.clearContainer(player, this.emberInputs);
        this.selectedTemplate = nextTemplate;
        this.createResult();
        this.templateDataDirty = true;
        this.syncTemplateData(serverPlayer);
        this.broadcastChanges();
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (this.menuPlayer instanceof ServerPlayer serverPlayer) {
            this.syncTemplateData(serverPlayer);
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == this.royalFrostInputs || container == this.emberInputs) {
            this.createResult();
        }
    }

    private void refreshTemplateCatalog() {
        if (this.level.isClientSide()) return;
        List<Identifier> favorites = this.menuPlayer
            .getData(ModDataAttachments.SMITHING_TEMPLATE_FAVORITES)
            .templates();
        List<ItemStack> discovered = BuiltInRegistries.ITEM.stream()
            .map(Item::getDefaultInstance)
            .filter(stack -> !stack.isEmpty())
            .filter(stack -> stack.isItemEnabled(this.level.enabledFeatures()))
            .filter(TranscendenceSmithingMenu::isTemplateItem)
            .map(stack -> stack.copyWithCount(1))
            .sorted(Comparator
                .comparingInt((ItemStack stack) -> favoriteIndex(favorites, itemId(stack)))
                .thenComparingInt(stack -> BuiltInRegistries.ITEM.getId(stack.getItem())))
            .toList();
        if (!sameTemplates(this.templates, discovered) || !this.favoriteTemplates.equals(favorites)) {
            this.templates = discovered;
            this.favoriteTemplates = List.copyOf(favorites);
            this.templateDataDirty = true;
        }
    }

    private static boolean isTemplateItem(ItemStack stack) {
        return stack.getItem() instanceof SmithingTemplateItem || stack.is(ModItemTags.TEMPLATES);
    }

    private void syncTemplateData(ServerPlayer player) {
        if (!this.templateDataDirty) return;
        PacketDistributor.sendToPlayer(player, new TranscendenceSmithingPackets.Sync(
            this.containerId,
            this.templates,
            this.favoriteTemplates,
            this.selectedTemplate
        ));
        this.templateDataDirty = false;
    }

    private boolean isRoyalOrFrostModeActive() {
        return this.hasSelectedTemplate() && this.getMode() != Mode.EMBER;
    }

    private boolean isEmberModeActive() {
        return this.hasSelectedTemplate() && this.getMode() == Mode.EMBER;
    }

    private boolean isEmberInputActive(int inputIndex) {
        return this.isEmberModeActive() && inputIndex < this.getEmberInputSize();
    }

    private boolean canPlaceRoyalFrostFirstInput(ItemStack stack) {
        if (this.getMode() == Mode.ROYAL) {
            return this.royalRecipes.stream().anyMatch(recipe ->
                Ingredient.testOptionalIngredient(recipe.value().templateIngredient(), this.selectedTemplate)
                    && recipe.value().baseIngredient().test(stack));
        }
        return this.frostRecipes.stream().anyMatch(recipe ->
            recipe.value().isTemplate(this.selectedTemplate) && recipe.value().isMaterial(stack));
    }

    private boolean canPlaceRoyalFrostSecondInput(ItemStack stack) {
        if (this.getMode() == Mode.ROYAL) {
            return this.royalRecipes.stream().anyMatch(recipe ->
                Ingredient.testOptionalIngredient(recipe.value().templateIngredient(), this.selectedTemplate)
                    && Ingredient.testOptionalIngredient(recipe.value().additionIngredient(), stack));
        }
        return this.frostRecipes.stream().anyMatch(recipe ->
            recipe.value().isTemplate(this.selectedTemplate) && recipe.value().isInput(stack));
    }

    private boolean canPlaceEmberMaterial(ItemStack stack) {
        return this.emberRecipes.stream().anyMatch(recipe ->
            recipe.value().isTemplateIngredient(this.selectedTemplate)
                && recipe.value().isMaterialIngredient(stack));
    }

    private boolean canPlaceEmberInput(int inputIndex, ItemStack stack) {
        if (inputIndex >= this.getEmberInputSize()) return false;
        return this.emberRecipes.stream().anyMatch(recipe ->
            recipe.value().isTemplateIngredient(this.selectedTemplate)
                && recipe.value().isInputIngredient(inputIndex, stack));
    }

    private void createResult() {
        this.clearRecipeState();
        if (!this.hasSelectedTemplate()) return;
        switch (this.getMode()) {
            case ROYAL -> this.createRoyalResult();
            case EMBER -> this.createEmberResult();
            case FROST -> this.createFrostResult();
            default -> throw new IllegalStateException("Unknown smithing mode: " + this.getMode());
        }
    }

    private void clearRecipeState() {
        this.selectedRoyalRecipe = null;
        this.selectedEmberRecipe = null;
        this.selectedFrostRecipe = null;
        this.frostResults = List.of();
        this.selectedFrostResult = -1;
        this.royalFrostResult.setItem(0, ItemStack.EMPTY);
        this.emberResult.setItem(0, ItemStack.EMPTY);
    }

    private void createRoyalResult() {
        SmithingRecipeInput input = new SmithingRecipeInput(
            this.selectedTemplate,
            this.royalFrostInputs.getItem(0),
            this.royalFrostInputs.getItem(1)
        );
        List<RecipeHolder<SmithingRecipe>> matches = RecipesRecord.getRecipes(this.level)
            .getRecipesFor(RecipeType.SMITHING, input, this.level)
            .toList();
        if (matches.isEmpty()) return;

        RecipeHolder<SmithingRecipe> recipe = matches.getFirst();
        ItemStack result = recipe.value().assemble(input);
        if (!result.isItemEnabled(this.level.enabledFeatures())) return;
        this.selectedRoyalRecipe = recipe;
        this.royalFrostResult.setRecipeUsed(recipe);
        this.royalFrostResult.setItem(0, result);
    }

    private void createEmberResult() {
        int inputSize = this.getEmberInputSize();
        if (inputSize == 0) return;
        List<ItemStack> inputs = new ArrayList<>(inputSize);
        for (int index = 0; index < inputSize; index++) {
            inputs.add(this.emberInputs.getItem(index + 1));
        }
        MultipleToOneSmithingRecipeInput input = new MultipleToOneSmithingRecipeInput(
            this.selectedTemplate,
            this.emberInputs.getItem(0),
            inputs
        );
        List<RecipeHolder<BaseMultipleToOneSmithingRecipe>> matches = RecipesRecord.getRecipes(this.level)
            .getRecipesFor(ModRecipeTypes.MULTIPLE_TO_ONE_SMITHING.get(), input, this.level)
            .toList();
        if (matches.isEmpty()) return;

        RecipeHolder<BaseMultipleToOneSmithingRecipe> recipe = matches.getFirst();
        ItemStack result = recipe.value().assemble(input, this.level);
        if (!result.isItemEnabled(this.level.enabledFeatures())) return;
        this.selectedEmberRecipe = recipe;
        this.emberResult.setRecipeUsed(recipe);
        this.emberResult.setItem(0, result);
    }

    private void createFrostResult() {
        FrostSmithingRecipeInput input = new FrostSmithingRecipeInput(
            this.selectedTemplate,
            this.royalFrostInputs.getItem(0),
            this.royalFrostInputs.getItem(1)
        );
        if (this.selectedTemplate.getItem() instanceof PermutationTemplateItem) {
            List<RecipeHolder<PermutationRecipe>> matches = RecipesRecord.getRecipes(this.level)
                .getRecipesFor(ModRecipeTypes.PERMUTATION.get(), input, this.level)
                .toList();
            if (!matches.isEmpty()) this.setFrostResult(matches.getFirst(), input);
            return;
        }
        List<RecipeHolder<DeformationRecipe>> matches = RecipesRecord.getRecipes(this.level)
            .getRecipesFor(ModRecipeTypes.DEFORMATION.get(), input, this.level)
            .toList();
        if (!matches.isEmpty()) this.setFrostResult(matches.getFirst(), input);
    }

    private void setFrostResult(
        RecipeHolder<? extends IFrostSmithingRecipe> recipe,
        FrostSmithingRecipeInput input
    ) {
        List<RecipeResult> results = recipe.value().inputs(input.input());
        if (results.isEmpty()) return;
        if (results.stream().anyMatch(result -> !result.result().item().value().isEnabled(this.level.enabledFeatures()))) return;
        this.selectedFrostRecipe = recipe;
        this.frostResults = List.copyOf(results);
        this.selectedFrostResult = 0;
        this.royalFrostResult.setRecipeUsed(recipe);
        this.royalFrostResult.setItem(0, recipe.value().assemble(0, input, this.level));
    }

    private boolean canTakeRoyalFrostResult() {
        if (this.getMode() == Mode.ROYAL) {
            if (this.selectedRoyalRecipe == null) return false;
            return this.selectedRoyalRecipe.value().matches(new SmithingRecipeInput(
                this.selectedTemplate,
                this.royalFrostInputs.getItem(0),
                this.royalFrostInputs.getItem(1)
            ), this.level);
        }
        return this.selectedFrostRecipe != null
            && this.selectedFrostResult >= 0
            && this.selectedFrostResult < this.frostResults.size()
            && this.selectedFrostRecipe.value().matches(new FrostSmithingRecipeInput(
                this.selectedTemplate,
                this.royalFrostInputs.getItem(0),
                this.royalFrostInputs.getItem(1)
            ), this.level);
    }

    private boolean canTakeEmberResult() {
        if (this.selectedEmberRecipe == null) return false;
        int inputSize = this.getEmberInputSize();
        List<ItemStack> inputs = new ArrayList<>(inputSize);
        for (int index = 0; index < inputSize; index++) {
            inputs.add(this.emberInputs.getItem(index + 1));
        }
        return this.selectedEmberRecipe.value().matches(new MultipleToOneSmithingRecipeInput(
            this.selectedTemplate,
            this.emberInputs.getItem(0),
            inputs
        ), this.level);
    }

    private void takeRoyalFrostResult(Player player, ItemStack stack) {
        stack.onCraftedBy(player, stack.getCount());
        this.royalFrostResult.awardUsedRecipes(player, List.of(
            this.selectedTemplate,
            this.royalFrostInputs.getItem(0),
            this.royalFrostInputs.getItem(1)
        ));
        this.consumeItem(this.royalFrostInputs, 0);
        this.consumeItem(this.royalFrostInputs, 1);
        this.playSmithingSound();
    }

    private void takeEmberResult(Player player, ItemStack stack) {
        stack.onCraftedBy(player, stack.getCount());
        List<ItemStack> relevantItems = new ArrayList<>();
        relevantItems.add(this.selectedTemplate);
        relevantItems.add(this.emberInputs.getItem(0));
        for (int index = 1; index <= this.getEmberInputSize(); index++) {
            relevantItems.add(this.emberInputs.getItem(index));
        }
        this.emberResult.awardUsedRecipes(player, relevantItems);
        this.consumeItem(this.emberInputs, 0);
        for (int index = 1; index <= this.getEmberInputSize(); index++) {
            this.consumeItem(this.emberInputs, index);
        }
        this.playSmithingSound();
    }

    private void consumeItem(Container container, int index) {
        ItemStack stack = container.getItem(index);
        if (stack.isEmpty()) return;
        stack.shrink(1);
        container.setItem(index, stack);
    }

    private void playSmithingSound() {
        this.access.execute((level, pos) -> level.levelEvent(1044, pos, 0));
    }

    /** 切换浮霜锻造结果。 */
    public void turnFrostResult(boolean left) {
        if (this.getMode() != Mode.FROST || this.selectedFrostRecipe == null || this.frostResults.isEmpty()) return;
        int offset = left ? -1 : 1;
        this.selectedFrostResult = Math.floorMod(this.selectedFrostResult + offset, this.frostResults.size());
        FrostSmithingRecipeInput input = new FrostSmithingRecipeInput(
            this.selectedTemplate,
            this.royalFrostInputs.getItem(0),
            this.royalFrostInputs.getItem(1)
        );
        this.royalFrostResult.setItem(
            0,
            this.selectedFrostRecipe.value().assemble(this.selectedFrostResult, input, this.level)
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.isActive() || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == ROYAL_FROST_RESULT_SLOT || index == EMBER_RESULT_SLOT) {
            if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_SLOT_START, PLAYER_HOTBAR_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (index < ROYAL_FROST_RESULT_SLOT) {
            if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_SLOT_START, PLAYER_HOTBAR_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INVENTORY_SLOT_START) {
            if (!this.moveIntoActiveInput(stack) && !this.moveWithinPlayerInventory(stack, index)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    private boolean moveIntoActiveInput(ItemStack stack) {
        if (!this.hasSelectedTemplate()) return false;
        if (this.getMode() == Mode.EMBER) {
            if (this.getSlot(EMBER_MATERIAL_SLOT).mayPlace(stack)
                && this.moveItemStackTo(stack, EMBER_MATERIAL_SLOT, EMBER_MATERIAL_SLOT + 1, false)) {
                return true;
            }
            int activeInputEnd = EMBER_INPUT_SLOT_START + this.getEmberInputSize();
            return this.moveItemStackTo(stack, EMBER_INPUT_SLOT_START, activeInputEnd, false);
        }
        if (this.getSlot(ROYAL_FROST_FIRST_INPUT_SLOT).mayPlace(stack)
            && this.moveItemStackTo(
                stack,
                ROYAL_FROST_FIRST_INPUT_SLOT,
                ROYAL_FROST_FIRST_INPUT_SLOT + 1,
                false
            )) {
            return true;
        }
        return this.moveItemStackTo(
            stack,
            ROYAL_FROST_SECOND_INPUT_SLOT,
            ROYAL_FROST_SECOND_INPUT_SLOT + 1,
            false
        );
    }

    private boolean moveWithinPlayerInventory(ItemStack stack, int index) {
        if (index < PLAYER_INVENTORY_SLOT_END) {
            return this.moveItemStackTo(stack, PLAYER_HOTBAR_SLOT_START, PLAYER_HOTBAR_SLOT_END, false);
        }
        return this.moveItemStackTo(stack, PLAYER_INVENTORY_SLOT_START, PLAYER_INVENTORY_SLOT_END, false);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.royalFrostResult
            && slot.container != this.emberResult
            && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.isActive()
            && slot.container != this.royalFrostResult
            && slot.container != this.emberResult
            && super.canDragTo(slot);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> {
            this.clearContainer(player, this.royalFrostInputs);
            this.clearContainer(player, this.emberInputs);
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(this.access, player, ModBlocks.TRANSCENDENCE_SMITHING_TABLE.get());
    }

    private static int favoriteIndex(List<Identifier> favorites, Identifier template) {
        int index = favorites.indexOf(template);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private static Identifier itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    private static boolean sameTemplates(List<ItemStack> first, List<ItemStack> second) {
        if (first.size() != second.size()) return false;
        for (int index = 0; index < first.size(); index++) {
            if (!ItemStack.isSameItemSameComponents(first.get(index), second.get(index))) return false;
        }
        return true;
    }

    public enum Mode {
        ROYAL,
        EMBER,
        FROST
    }

    private static class ModeSlot extends Slot {
        private final BooleanSupplier active;
        private final Predicate<ItemStack> filter;

        ModeSlot(
            Container container,
            int slot,
            int x,
            int y,
            BooleanSupplier active,
            Predicate<ItemStack> filter
        ) {
            super(container, slot, x, y);
            this.active = active;
            this.filter = filter;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.isActive() && this.filter.test(stack);
        }

        @Override
        public boolean isActive() {
            return this.active.getAsBoolean();
        }
    }

    private class SmithingResultSlot extends Slot {
        private final BooleanSupplier active;
        private final boolean ember;

        SmithingResultSlot(Container container, int x, int y, BooleanSupplier active, boolean ember) {
            super(container, 0, x, y);
            this.active = active;
            this.ember = ember;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            if (!this.hasItem() || !this.isActive()) return false;
            return this.ember
                ? TranscendenceSmithingMenu.this.canTakeEmberResult()
                : TranscendenceSmithingMenu.this.canTakeRoyalFrostResult();
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            if (this.ember) {
                TranscendenceSmithingMenu.this.takeEmberResult(player, stack);
            } else {
                TranscendenceSmithingMenu.this.takeRoyalFrostResult(player, stack);
            }
            super.onTake(player, stack);
        }

        @Override
        public boolean isActive() {
            return this.active.getAsBoolean();
        }
    }
}
