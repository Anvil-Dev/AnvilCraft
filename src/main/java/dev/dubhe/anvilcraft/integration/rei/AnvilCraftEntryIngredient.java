package dev.dubhe.anvilcraft.integration.rei;

import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SelectOne;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import lombok.Getter;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;

@Getter
public class AnvilCraftEntryIngredient {
    final EntryIngredient entryIngredient;
    final float chance;
    final boolean isSelectOne;
    public HashMap<EntryStack<ItemStack>, Float> selectOneChanceMap = new HashMap<>();

    private AnvilCraftEntryIngredient(EntryIngredient entryIngredient, float chance, boolean isSelectOne) {
        this.entryIngredient = entryIngredient;
        this.chance = chance;
        this.isSelectOne = isSelectOne;
    }

    public static AnvilCraftEntryIngredient of(EntryIngredient entryIngredient) {
        return new AnvilCraftEntryIngredient(entryIngredient, 1f, false);
    }

    public static AnvilCraftEntryIngredient of(EntryIngredient entryIngredient, float chance) {
        return new AnvilCraftEntryIngredient(entryIngredient, chance, false);
    }

    /**
     * 从{@link SelectOne}创建{@link AnvilCraftEntryIngredient}
     *
     * @param selectOne {@link SelectOne}
     * @return {@link AnvilCraftEntryIngredient}
     */
    public static AnvilCraftEntryIngredient of(SelectOne selectOne) {
        HashMap<EntryStack<ItemStack>, Float> selectOneChanceMap = new HashMap<>();
        for (RecipeOutcome recipeOutcome : selectOne.getOutcomes()) {
            if (recipeOutcome instanceof SpawnItem spawnItem) {
                selectOneChanceMap.put(EntryStacks.of(spawnItem.getResult()), (float) spawnItem.getChance());
            }
        }
        AnvilCraftEntryIngredient anvilCraftEntryIngredient =
                new AnvilCraftEntryIngredient(EntryIngredient.of(selectOneChanceMap.keySet()), 1, true);
        anvilCraftEntryIngredient.selectOneChanceMap = selectOneChanceMap;
        return  anvilCraftEntryIngredient;
    }
}
