package dev.dubhe.anvilcraft.inventory;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.inventory.component.jewel.JewelInputSlot;
import dev.dubhe.anvilcraft.inventory.component.jewel.JewelResultSlot;
import dev.dubhe.anvilcraft.inventory.container.JewelSourceContainer;
import dev.dubhe.anvilcraft.network.JewelCraftingAutoFillPacket;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class JewelCraftingMenu extends AbstractContainerMenu {
    public static final int RESULT_SLOT = 0;
    public static final int SOURCE_SLOT = 1;
    public static final int CRAFT_SLOT_START = 2;
    public static final int CRAFT_SLOT_END = 6;
    public static final int INV_SLOT_START = 6;
    public static final int INV_SLOT_END = 33;
    public static final int USE_ROW_SLOT_START = 33;
    public static final int USE_ROW_SLOT_END = 42;

    private final JewelSourceContainer sourceContainer = new JewelSourceContainer(this);
    private final CraftingContainer craftingContainer = new TransientCraftingContainer(this, 4, 1);
    private final ResultContainer resultContainer = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;

    public JewelCraftingMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory) {
        this(menuType, containerId, inventory, ContainerLevelAccess.NULL);
    }

    public JewelCraftingMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(menuType, containerId);
        this.access = access;
        this.player = inventory.player;

        // result
        this.addSlot(new JewelResultSlot(this.resultContainer, this.craftingContainer, this.resultContainer, 0, 134, 51));

        // result
        this.addSlot(new Slot(this.sourceContainer, 0, 80, 19) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return RecipesRecord.getRecipes(inventory.player.level()).byType(ModRecipeTypes.JEWEL_CRAFTING.get())
                    .stream()
                    .anyMatch(holder -> holder.value().source().test(stack));
            }
        });

        // craft
        for (int i = 0; i < 4; i++) {
            this.addSlot(new JewelInputSlot(this.sourceContainer, this.craftingContainer, i, 26 + i * 18, 51));
        }

        // player
        this.addPlayerInventory(inventory);
        this.addPlayerHotbar(inventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY; // EMPTY_ITEM
        }
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // noinspection ConstantValue
        if (sourceSlot == null || !sourceSlot.hasItem()) return sourceStack;

        if (index == RESULT_SLOT) {
            int totalCrafted = 0;
            while (true) {
                ItemStack currentResult = sourceSlot.getItem();
                if (currentResult.isEmpty()) break;

                ItemStack moveStack = currentResult.copy();
                if (!moveItemStackTo(moveStack, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
                    break;
                }

                int moved = currentResult.getCount() - moveStack.getCount();
                if (moved <= 0) break;

                sourceSlot.onQuickCraft(moveStack, currentResult);

                if (moveStack.isEmpty()) {
                    sourceSlot.setByPlayer(ItemStack.EMPTY);
                } else {
                    sourceSlot.setChanged();
                }
                ItemStack takenStack = currentResult.copyWithCount(moved);
                sourceSlot.onTake(player, takenStack);
                totalCrafted += moved;
            }

            return totalCrafted > 0 ? sourceStack.copyWithCount(totalCrafted) : ItemStack.EMPTY;
        } else if (index >= SOURCE_SLOT && index < CRAFT_SLOT_END) {
            if (!moveItemStackTo(copyOfSourceStack, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= INV_SLOT_START && index < USE_ROW_SLOT_END) {
            ItemStack empty = this.quickMoveInvStack(index, copyOfSourceStack);
            if (empty != null) return empty;
        }

        if (copyOfSourceStack.isEmpty()) {
            sourceSlot.setByPlayer(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        if (copyOfSourceStack.getCount() == sourceStack.getCount()) {
            return ItemStack.EMPTY;
        }

        sourceStack.setCount(copyOfSourceStack.getCount());
        sourceSlot.onTake(player, copyOfSourceStack);
        if (index == RESULT_SLOT) {
            player.drop(copyOfSourceStack, false);
        }
        return sourceStack;
    }

    protected @Nullable ItemStack quickMoveInvStack(int index, ItemStack copyOfSourceStack) {
        // 从背包里转移物品
        if (this.moveItemStackTo(copyOfSourceStack, SOURCE_SLOT, SOURCE_SLOT + 1, false)) {
            this.slotsChanged(this.sourceContainer);
        } else if (this.moveItemStackTo(copyOfSourceStack, CRAFT_SLOT_START, CRAFT_SLOT_END, false)) {
            this.slotsChanged(this.craftingContainer);
        } else if (index < INV_SLOT_END && !this.moveItemStackTo(copyOfSourceStack, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false)) {
            // 移到快捷栏
            return ItemStack.EMPTY;
        } else if (index >= INV_SLOT_END && !this.moveItemStackTo(copyOfSourceStack, INV_SLOT_START, INV_SLOT_END, false)) {
            // 移动到背包
            return ItemStack.EMPTY;
        }
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.JEWEL_CRAFTING_TABLE.get());
    }

    public @Nullable RecipeHolder<JewelCraftingRecipe> findRecipeBySource(ItemStack source) {
        return RecipesRecord.getRecipes(this.player.level()).byType(ModRecipeTypes.JEWEL_CRAFTING.get())
            .stream()
            .filter(holder -> holder.value().source().test(source))
            .findFirst()
            .orElse(null);
    }

    @Override
    public void slotsChanged(Container container) {
        for (int i = CRAFT_SLOT_START; i < CRAFT_SLOT_END; i++) {
            Slot slot = slots.get(i);
            if (slot instanceof JewelInputSlot inputSlot) {
                inputSlot.updateIngredient();
            }
        }
        this.access.execute((level, _) -> changedCraftingSlots(
            this,
            level,
            this.player,
            this.sourceContainer,
            this.craftingContainer,
            this.resultContainer
        ));
    }

    private static void changedCraftingSlots(
        JewelCraftingMenu menu,
        Level level,
        Player player,
        JewelSourceContainer sourceContainer,
        CraftingContainer craftingContainer,
        ResultContainer resultContainer
    ) {
        if (level.isClientSide()) return;
        ItemStack itemStack = ItemStack.EMPTY;
        ServerPlayer serverPlayer = (ServerPlayer) player;
        var input = new JewelCraftingRecipe.Input(sourceContainer.getItem(0), craftingContainer.getItems());
        Optional<RecipeHolder<JewelCraftingRecipe>> recipeOp = RecipesRecord.getRecipes(level).getRecipesFor(
            ModRecipeTypes.JEWEL_CRAFTING.get(),
            input,
            serverPlayer.level()
        ).findAny();
        if (recipeOp.isPresent()) {
            RecipeHolder<JewelCraftingRecipe> holder = recipeOp.get();
            JewelCraftingRecipe recipe = holder.value();
            if (recipe.matches(input, level)) {
                if (resultContainer.setRecipeUsed(serverPlayer, holder)) {
                    ItemStack result = recipe.assemble(input);
                    if (result.isItemEnabled(level.enabledFeatures())) {
                        itemStack = result;
                        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                        enchantments.set(level.registryAccess().holderOrThrow(Enchantments.VANISHING_CURSE), 1);
                        itemStack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
                    }
                }
            }
        }
        resultContainer.setItem(0, itemStack);
        menu.setRemoteSlot(RESULT_SLOT, itemStack);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
            menu.containerId,
            menu.incrementStateId(),
            RESULT_SLOT,
            itemStack
        ));
    }

    /// 自动填充配方所需物品
    /// 当玩家按下空格键时调用此方法
    public void autoFill() {
        if (this.player.level().isClientSide()) {
            ClientPacketDistributor.sendToServer(new JewelCraftingAutoFillPacket());
            return;
        }

        JewelCraftingRecipe recipe = Optional.ofNullable(this.sourceContainer.getRecipe())
            .map(RecipeHolder::value)
            .orElse(null);
        if (recipe == null) return;

        List<ItemIngredientPredicate> ingredients = recipe.ingredients();
        for (int i = 0; i < Math.min(ingredients.size(), 4); i++) {
            this.quickMoveStack(this.player, CRAFT_SLOT_START + i);
            this.moveInvItemTo(ingredients.get(i), CRAFT_SLOT_START + i);
        }
    }

    protected void moveInvItemTo(ItemIngredientPredicate needItem, int targetIndex) {
        for (int i = INV_SLOT_START; i < USE_ROW_SLOT_END; i++) {
            Slot slot = slots.get(i);
            if (!needItem.test(slot.getItem())) continue;
            if (!this.moveItemStackTo(slot.getItem(), targetIndex, targetIndex + 1, false)) {
                return;
            }
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((_, _) -> {
            this.clearContainer(player, this.sourceContainer);
            this.clearContainer(player, this.craftingContainer);
        });
    }
}
