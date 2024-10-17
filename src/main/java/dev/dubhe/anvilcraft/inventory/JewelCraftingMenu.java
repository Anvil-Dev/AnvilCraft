package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.inventory.component.jewel.JewelInputSlot;
import dev.dubhe.anvilcraft.inventory.component.jewel.JewelResultSlot;
import dev.dubhe.anvilcraft.inventory.container.JewelSourceContainer;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.cache.RecipeCaches;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class JewelCraftingMenu extends AbstractContainerMenu {
    public static final int SOURCE_SLOT = 0;
    public static final int RESULT_SLOT = 1;
    public static final int CRAFT_SLOT_START = 2;
    public static final int CRAFT_SLOT_END = 6;
    public static final int INV_SLOT_START = 6;
    public static final int INV_SLOT_END = 33;
    public static final int USE_ROW_SLOT_START = 33;
    public static final int USE_ROW_SLOT_END = 42;

    @Getter
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

        // source
        addSlot(new Slot(sourceContainer, 0, 80, 19) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return RecipeCaches.getAllJewelResultItem().contains(stack.getItem());
            }
        });

        // result
        addSlot(new JewelResultSlot(sourceContainer, craftingContainer, resultContainer, 0, 134, 51));

        // craft
        for (int i = 0; i < 4; i++) {
            addSlot(new JewelInputSlot(sourceContainer, craftingContainer, i, 26 + i * 18, 51));
        }

        // player
        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);
    }

    @SuppressWarnings("DuplicatedCode")
    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
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

        //noinspection ConstantValue
        if (sourceSlot != null && sourceSlot.hasItem()) {
            if (index >= SOURCE_SLOT && index < CRAFT_SLOT_END) {
                if (!moveItemStackTo(copyOfSourceStack, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= INV_SLOT_START && index < USE_ROW_SLOT_END) {
                if (!moveItemStackTo(copyOfSourceStack, SOURCE_SLOT, SOURCE_SLOT + 1, false)) {
                    if (!moveItemStackTo(copyOfSourceStack, CRAFT_SLOT_START, CRAFT_SLOT_END, false)) {
                        // 在背包里
                        if (index < INV_SLOT_END) {
                            // 移到快捷栏
                            if (!moveItemStackTo(copyOfSourceStack, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else {
                            // 移动到背包
                            if (!moveItemStackTo(copyOfSourceStack, INV_SLOT_START, INV_SLOT_END, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                    }
                }
            }
            if (copyOfSourceStack.isEmpty()) {
                sourceSlot.setByPlayer(ItemStack.EMPTY);
            } else {
                sourceSlot.setChanged();
            }

            if (copyOfSourceStack.getCount() == sourceStack.getCount()) {
                return ItemStack.EMPTY;
            }

            sourceSlot.onTake(player, copyOfSourceStack);
            if (index == RESULT_SLOT) {
                player.drop(copyOfSourceStack, false);
            }
        }
        return sourceStack;
    }


    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.JEWEL_CRAFTING_TABLE.get());
    }

    @Override
    public void slotsChanged(Container container) {
        for (int i = CRAFT_SLOT_START; i < CRAFT_SLOT_END; i++) {
            Slot slot = slots.get(i);
            if (slot instanceof JewelInputSlot inputSlot) {
                inputSlot.updateIngredient();
            }
        }
        access.execute((level, pos) -> changedCraftingSlots(this, level, player, sourceContainer, craftingContainer, resultContainer));
    }

    private static void changedCraftingSlots(
        JewelCraftingMenu menu,
        Level level,
        Player player,
        JewelSourceContainer sourceContainer,
        CraftingContainer craftingContainer,
        ResultContainer resultContainer
    ) {
        if (!level.isClientSide()) {
            ItemStack itemStack = ItemStack.EMPTY;
            ServerPlayer serverPlayer = (ServerPlayer) player;
            RecipeHolder<JewelCraftingRecipe> recipeHolder = sourceContainer.getRecipe();
            if (recipeHolder != null) {
                JewelCraftingRecipe recipe = recipeHolder.value();
                var input = new JewelCraftingRecipe.Input(sourceContainer.getItem(0), craftingContainer.getItems());
                if (recipe.matches(input, level)) {
                    if (resultContainer.setRecipeUsed(level, serverPlayer, recipeHolder)) {
                        ItemStack result = recipe.assemble(input, level.registryAccess());
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
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> {
            clearContainer(player, sourceContainer);
            clearContainer(player, craftingContainer);
        });
    }
}
